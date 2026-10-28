package com.edtech.coe.scheduling.service.impl;

import com.edtech.coe.batch.service.BatchLookupService;
import com.edtech.coe.coe.service.CoeLockService;
import com.edtech.coe.common.exception.BusinessException;
import com.edtech.coe.common.exception.ResourceNotFoundException;
import com.edtech.coe.iam.dto.AuthenticatedUser;
import com.edtech.coe.iam.dto.UserResponse;
import com.edtech.coe.iam.entity.Role;
import com.edtech.coe.iam.service.UserService;
import com.edtech.coe.lab.service.LabLookupService;
import com.edtech.coe.scheduling.dto.AffectedSessionResponse;
import com.edtech.coe.scheduling.dto.CreateUnavailabilityRequest;
import com.edtech.coe.scheduling.dto.PersonSummary;
import com.edtech.coe.scheduling.dto.UnavailabilityResponse;
import com.edtech.coe.scheduling.entity.MentorUnavailability;
import com.edtech.coe.scheduling.entity.SessionStatus;
import com.edtech.coe.scheduling.entity.TrainingSession;
import com.edtech.coe.scheduling.repository.MentorUnavailabilityRepository;
import com.edtech.coe.scheduling.repository.TrainingSessionRepository;
import com.edtech.coe.scheduling.service.AvailabilityService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AvailabilityServiceImpl implements AvailabilityService {

    // An overlap message lists at most this many existing records, so it stays readable
    private static final int MAX_OVERLAP_LINES = 3;

    private final MentorUnavailabilityRepository unavailabilityRepository;
    private final TrainingSessionRepository sessionRepository;
    private final UserService userService;
    private final BatchLookupService batchLookupService;
    private final LabLookupService labLookupService;
    private final CoeLockService coeLockService;

    // The longest absence that can be recorded in one go. It catches a typo such as 2207
    // instead of 2027.
    @Value("${app.scheduling.max-unavailability-days:365}")
    private int maxUnavailabilityDays;

    // READ_COMMITTED: the overlap check runs after the COE lock is taken, and must see the
    // records and sessions that the request just ahead of us saved. The default level would
    // show us an older picture. This is the same reason as in SessionServiceImpl.
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public UnavailabilityResponse recordUnavailability(CreateUnavailabilityRequest request,
                                                       AuthenticatedUser caller) {
        LocalDate from = request.getFromDate();
        LocalDate to = request.getToDate();
        validateRange(from, to);

        UserResponse mentor = loadMentor(request.getMentorId(), caller);
        Long coeId = mentor.getCoeId();

        // From here, one request at a time per COE until this transaction ends. Booking a
        // session takes the same lock, so a booking and an absence for the same mentor can
        // never both slip through at once.
        coeLockService.lock(coeId);

        List<MentorUnavailability> overlapping =
                unavailabilityRepository.findOverlapping(mentor.getId(), from, to);
        if (!overlapping.isEmpty()) {
            throw overlapError(mentor, overlapping);
        }

        MentorUnavailability saved = unavailabilityRepository.save(MentorUnavailability.builder()
                .coeId(coeId)
                .mentorId(mentor.getId())
                .fromDate(from)
                .toDate(to)
                .type(request.getType())
                .note(trimToNull(request.getNote()))
                .recordedBy(caller.getUserId())
                .build());   // active starts as true

        // The classes this absence leaves without a teacher. They are not changed here: the
        // Practice Head reassigns each one.
        List<AffectedSessionResponse> affected = affectedSessions(mentor.getId(), from, to);

        Map<Long, UserResponse> people = userService.getUsersByIds(
                new HashSet<>(List.of(mentor.getId(), caller.getUserId())));

        return UnavailabilityResponse.from(saved,
                summary(mentor.getId(), people),
                summary(caller.getUserId(), people),
                affected);
    }

    @Override
    public UnavailabilityResponse getUnavailabilityById(Long id, AuthenticatedUser caller) {
        MentorUnavailability record = findInScope(id, caller);

        Map<Long, UserResponse> people = userService.getUsersByIds(
                new HashSet<>(List.of(record.getMentorId(), record.getRecordedBy())));

        return UnavailabilityResponse.from(record,
                summary(record.getMentorId(), people),
                summary(record.getRecordedBy(), people));
    }

    @Override
    public Page<UnavailabilityResponse> getUnavailability(Long coeId, Long mentorId, Boolean active,
                                                          LocalDate fromDate, LocalDate toDate,
                                                          Pageable pageable, AuthenticatedUser caller) {
        if (fromDate != null && toDate != null && toDate.isBefore(fromDate)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "To date must not be before from date");
        }

        Long effectiveCoeId = resolveListCoe(coeId, caller);

        Page<MentorUnavailability> page = unavailabilityRepository.search(
                effectiveCoeId, mentorId, active, fromDate, toDate, pageable);

        // One lookup for the whole page, not one per row
        Set<Long> personIds = new HashSet<>();
        page.getContent().forEach(record -> {
            personIds.add(record.getMentorId());
            personIds.add(record.getRecordedBy());
        });
        Map<Long, UserResponse> people = userService.getUsersByIds(personIds);

        return page.map(record -> UnavailabilityResponse.from(record,
                summary(record.getMentorId(), people),
                summary(record.getRecordedBy(), people)));
    }

    @Override
    @Transactional
    public void cancelUnavailability(Long id, AuthenticatedUser caller) {
        MentorUnavailability record = findInScope(id, caller);

        // Cancelling twice is safe: nothing to do the second time
        if (!record.isActive()) {
            return;
        }

        // Soft delete: keep the row, so reports can still see what was recorded and withdrawn.
        // The mentor can be booked again as soon as this transaction ends.
        record.setActive(false);
    }

    // ---------- checks ----------

    private void validateRange(LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "From date must not be after to date");
        }

        // Both dates are included, so 3 to 5 February is 3 days
        long days = ChronoUnit.DAYS.between(from, to) + 1;
        if (days > maxUnavailabilityDays) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "An absence can be at most " + maxUnavailabilityDays + " days. This one is "
                            + days + " days");
        }
    }

    // One message for every way a mentor can be unusable, so a caller cannot find out
    // which user ids exist in other COEs
    private UserResponse loadMentor(Long mentorId, AuthenticatedUser caller) {
        UserResponse mentor = userService.getUsersByIds(Set.of(mentorId)).get(mentorId);

        boolean usable = mentor != null
                && mentor.getRole() == Role.MENTOR
                // A platform-wide caller may act for any COE. Everyone else only for their own.
                && (caller.isPlatformWide() || Objects.equals(mentor.getCoeId(), caller.getCoeId()));

        if (!usable) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "Mentor with id " + mentorId + " was not found in this COE");
        }
        if (!mentor.isActive()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "Mentor " + mentor.getUserCode() + " is inactive");
        }
        return mentor;
    }

    private BusinessException overlapError(UserResponse mentor, List<MentorUnavailability> overlapping) {
        List<String> lines = overlapping.stream()
                .limit(MAX_OVERLAP_LINES)
                .map(record -> record.getType() + " from " + record.getFromDate() + " to "
                        + record.getToDate() + " (record " + record.getId() + ")")
                .toList();
        String more = overlapping.size() > MAX_OVERLAP_LINES
                ? " and " + (overlapping.size() - MAX_OVERLAP_LINES) + " more"
                : "";

        return new BusinessException(HttpStatus.CONFLICT,
                "Mentor " + mentor.getFullName() + " (" + mentor.getUserCode() + ") already has an absence"
                        + " recorded: " + String.join("; ", lines) + more
                        + ". Cancel it first, or choose dates that do not overlap");
    }

    // ---------- the classes an absence leaves without a teacher ----------

    private List<AffectedSessionResponse> affectedSessions(Long mentorId, LocalDate from, LocalDate to) {
        List<TrainingSession> sessions = sessionRepository.findByMentorAndDateRange(
                mentorId, from, to, SessionStatus.SCHEDULED);

        if (sessions.isEmpty()) {
            return List.of();
        }

        // One lookup for all of them, not one per session
        Set<Long> batchIds = sessions.stream().map(TrainingSession::getBatchId).collect(Collectors.toSet());
        Set<Long> labIds = sessions.stream().map(TrainingSession::getLabId).collect(Collectors.toSet());
        Map<Long, String> batchNames = batchLookupService.getBatchNames(batchIds);
        Map<Long, String> labNames = labLookupService.getLabNames(labIds);

        List<AffectedSessionResponse> result = new ArrayList<>();
        for (TrainingSession session : sessions) {
            result.add(AffectedSessionResponse.from(session,
                    batchNames.get(session.getBatchId()),
                    labNames.get(session.getLabId())));
        }
        return result;
    }

    // ---------- scope rules ----------

    // Get and cancel: a record outside the caller's COE looks like it does not exist
    private MentorUnavailability findInScope(Long id, AuthenticatedUser caller) {
        MentorUnavailability record = unavailabilityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Unavailability record", id));

        if (!caller.isPlatformWide() && !Objects.equals(caller.getCoeId(), record.getCoeId())) {
            throw new ResourceNotFoundException("Unavailability record", id);
        }
        return record;
    }

    // List: platform-wide roles may filter by any COE, everyone else is held to their own
    private Long resolveListCoe(Long requestedCoeId, AuthenticatedUser caller) {
        if (caller.isPlatformWide()) {
            return requestedCoeId;
        }
        // Without this check, a missing coeId would turn into "no filter" and list every COE
        if (caller.getCoeId() == null) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "Your account is not linked to a COE");
        }
        if (requestedCoeId != null && !requestedCoeId.equals(caller.getCoeId())) {
            throw new BusinessException(HttpStatus.FORBIDDEN,
                    "You can only view absences in your own COE");
        }
        return caller.getCoeId();
    }

    // ---------- helpers ----------

    // A person may be missing if the user record was removed. The row still shows, with
    // empty details.
    private PersonSummary summary(Long id, Map<Long, UserResponse> people) {
        UserResponse user = people.get(id);
        return user == null
                ? PersonSummary.of(id, null, null)
                : PersonSummary.of(id, user.getUserCode(), user.getFullName());
    }

    // Blank or whitespace-only text becomes null, so it is stored as "no value"
    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}