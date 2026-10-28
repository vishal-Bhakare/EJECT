package com.edtech.coe.scheduling.service.impl;

import com.edtech.coe.batch.dto.BatchResponse;
import com.edtech.coe.batch.entity.BatchStatus;
import com.edtech.coe.batch.service.BatchLookupService;
import com.edtech.coe.batch.service.BatchService;
import com.edtech.coe.coe.service.CoeLockService;
import com.edtech.coe.coe.service.CoeService;
import com.edtech.coe.common.exception.BusinessException;
import com.edtech.coe.common.exception.ResourceNotFoundException;
import com.edtech.coe.iam.dto.AuthenticatedUser;
import com.edtech.coe.iam.dto.UserResponse;
import com.edtech.coe.iam.entity.Role;
import com.edtech.coe.iam.service.UserService;
import com.edtech.coe.lab.dto.LabResponse;
import com.edtech.coe.lab.service.LabLookupService;
import com.edtech.coe.lab.service.LabService;
import com.edtech.coe.scheduling.dto.ChangeSessionStatusRequest;
import com.edtech.coe.scheduling.dto.CreateSessionRequest;
import com.edtech.coe.scheduling.dto.SessionResponse;
import com.edtech.coe.scheduling.dto.UpdateSessionRequest;
import com.edtech.coe.scheduling.entity.MentorUnavailability;
import com.edtech.coe.scheduling.entity.SessionStatus;
import com.edtech.coe.scheduling.entity.TrainingSession;
import com.edtech.coe.scheduling.repository.MentorUnavailabilityRepository;
import com.edtech.coe.scheduling.repository.TrainingSessionRepository;
import com.edtech.coe.scheduling.service.SessionService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SessionServiceImpl implements SessionService {

    // A clash message lists at most this many lines, so it stays readable
    private static final int MAX_CLASH_LINES = 5;

    private final TrainingSessionRepository sessionRepository;
    private final MentorUnavailabilityRepository unavailabilityRepository;   // NEW
    private final BatchService batchService;
    private final BatchLookupService batchLookupService;
    private final LabService labService;
    private final LabLookupService labLookupService;
    private final UserService userService;
    private final CoeService coeService;
    private final CoeLockService coeLockService;

    // true = a batch that can be bigger than the lab is refused
    @Value("${app.scheduling.enforce-lab-capacity:true}")
    private boolean enforceLabCapacity;

    // READ_COMMITTED: each query sees everything that other requests have already committed.
    // The clash check runs after the COE lock is taken, and must see the sessions that the
    // request just ahead of us saved. The default level would show us an older picture.
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public SessionResponse createSession(CreateSessionRequest request, AuthenticatedUser caller) {
        validateTimes(request.getStartTime(), request.getEndTime());

        // The batch decides the COE, and the scope check comes with it
        BatchResponse batch = loadBatch(request.getBatchId(), caller);
        requireOpenBatch(batch, "schedule a session in");

        Long coeId = batch.getCoeId();
        requireActiveCoe(coeId);
        validateDateInBatch(request.getSessionDate(), batch);

        LabResponse lab = loadLab(request.getLabId(), caller, coeId);
        checkLabCapacity(lab, batch);
        UserResponse mentor = loadMentor(request.getMentorId(), coeId);

        // From here, one request at a time per COE until this transaction ends
        coeLockService.lock(coeId);
        checkSchedulable(coeId, request.getSessionDate(), request.getStartTime(), request.getEndTime(),
                mentor.getId(), mentorLabel(mentor), lab.getId(), lab.getName(),
                batch.getId(), batch.getName(), null, true);   // NEW: a new session always checks the mentor's absences

        TrainingSession session = TrainingSession.builder()
                .sessionCode(temporaryCode())
                .coeId(coeId)
                .batchId(batch.getId())
                .labId(lab.getId())
                .mentorId(mentor.getId())
                .title(request.getTitle().trim())
                .description(trimToNull(request.getDescription()))
                .sessionDate(request.getSessionDate())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .build();   // status starts as SCHEDULED

        // First save gives us the auto-generated id
        TrainingSession saved = sessionRepository.save(session);

        // Build the real code from the id, e.g. SES-0001
        saved.setSessionCode(buildSessionCode(saved.getId()));

        return SessionResponse.from(saved, batch.getName(), lab.getName(),
                mentor.getUserCode(), mentor.getFullName());
    }

    @Override
    public SessionResponse getSessionById(Long id, AuthenticatedUser caller) {
        return toResponses(List.of(findInScope(id, caller))).get(0);
    }

    @Override
    public Page<SessionResponse> getSessions(Long coeId, Long batchId, Long labId, Long mentorId,
                                             SessionStatus status, LocalDate fromDate, LocalDate toDate,
                                             Pageable pageable, AuthenticatedUser caller) {
        if (fromDate != null && toDate != null && toDate.isBefore(fromDate)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "To date must not be before from date");
        }

        Long effectiveCoeId = resolveListCoe(coeId, caller);

        Page<TrainingSession> page = sessionRepository.search(
                effectiveCoeId, batchId, labId, mentorId, status, fromDate, toDate, pageable);

        // One lookup for the whole page, not one per row
        Lookups lookups = lookups(page.getContent());
        return page.map(session -> toResponse(session, lookups));
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public SessionResponse updateSession(Long id, UpdateSessionRequest request, AuthenticatedUser caller) {
        validateTimes(request.getStartTime(), request.getEndTime());

        // Locked first, so nobody can edit, cancel or complete this session at the same moment
        TrainingSession session = findInScopeForUpdate(id, caller);

        if (session.getStatus() != SessionStatus.SCHEDULED) {
            throw new BusinessException(HttpStatus.CONFLICT,
                    "A " + session.getStatus() + " session cannot be edited");
        }

        Long coeId = session.getCoeId();
        BatchResponse batch = batchService.getBatchById(session.getBatchId(), caller);
        requireOpenBatch(batch, "change a session in");
        requireActiveCoe(coeId);
        validateDateInBatch(request.getSessionDate(), batch);

        LabResponse lab = loadLab(request.getLabId(), caller, coeId);
        checkLabCapacity(lab, batch);

        // The mentor cannot be changed here (that is the reassign workflow), but the
        // mentor's name is needed for the messages and the response
        UserResponse mentor = userService.getUsersByIds(Set.of(session.getMentorId()))
                .get(session.getMentorId());

        // NEW: the mentor's absences only matter when the session moves to a different day.
        // A session whose mentor became unavailable AFTER it was booked must still be editable
        // on its own day (a new topic, a different lab, a shifted time), because the right fix
        // for it is to reassign the mentor, not to freeze the class. Moving it to another day
        // is checked, so a class cannot be shifted onto a day the mentor is away.
        boolean dateChanged = !request.getSessionDate().equals(session.getSessionDate());

        coeLockService.lock(coeId);
        checkSchedulable(coeId, request.getSessionDate(), request.getStartTime(), request.getEndTime(),
                session.getMentorId(), mentorLabel(mentor, session.getMentorId()),
                lab.getId(), lab.getName(), batch.getId(), batch.getName(), session.getId(),
                dateChanged);

        // Full replacement: optional fields that are not sent become null
        session.setLabId(lab.getId());
        session.setTitle(request.getTitle().trim());
        session.setDescription(trimToNull(request.getDescription()));
        session.setSessionDate(request.getSessionDate());
        session.setStartTime(request.getStartTime());
        session.setEndTime(request.getEndTime());

        // saveAndFlush runs auditing now, so the response shows the new updatedAt
        TrainingSession saved = sessionRepository.saveAndFlush(session);

        return SessionResponse.from(saved, batch.getName(), lab.getName(),
                mentor == null ? null : mentor.getUserCode(),
                mentor == null ? null : mentor.getFullName());
    }

    @Override
    @Transactional
    public SessionResponse changeStatus(Long id, ChangeSessionStatusRequest request,
                                        AuthenticatedUser caller) {
        TrainingSession session = findInScopeForUpdate(id, caller);

        if (request.getStatus() == SessionStatus.SCHEDULED) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "A session cannot be set back to SCHEDULED. Create a new session instead");
        }

        return moveTo(session, request.getStatus(), request.getReason());
    }

    @Override
    @Transactional
    public void deleteSession(Long id, AuthenticatedUser caller) {
        TrainingSession session = findInScopeForUpdate(id, caller);

        // Cancelling twice is safe: nothing to do the second time
        if (session.getStatus() == SessionStatus.CANCELLED) {
            return;
        }

        // A completed session is rejected here by the status rules
        moveTo(session, SessionStatus.CANCELLED, null);
    }

    // ---------- status rules ----------

    private SessionResponse moveTo(TrainingSession session, SessionStatus target, String reason) {
        SessionStatus current = session.getStatus();

        if (current == target) {
            throw new BusinessException(HttpStatus.CONFLICT, "Session is already " + target);
        }
        // Only a SCHEDULED session can change. COMPLETED and CANCELLED are final.
        if (current != SessionStatus.SCHEDULED) {
            throw new BusinessException(HttpStatus.CONFLICT,
                    "A " + current + " session cannot be changed to " + target);
        }

        session.setStatus(target);
        // A reason only makes sense for a cancel
        session.setStatusReason(target == SessionStatus.CANCELLED ? trimToNull(reason) : null);
        session.setStatusChangedAt(Instant.now());

        TrainingSession saved = sessionRepository.saveAndFlush(session);
        return toResponses(List.of(saved)).get(0);
    }

    // ---------- loading and checking the things a session points at ----------

    // The batch id comes from the request body, so a batch that is missing or outside the
    // caller's COE is a 400 (bad input), not a 404 (wrong URL)
    private BatchResponse loadBatch(Long batchId, AuthenticatedUser caller) {
        try {
            return batchService.getBatchById(batchId, caller);
        } catch (ResourceNotFoundException ex) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "Batch with id " + batchId + " was not found");
        }
    }

    private LabResponse loadLab(Long labId, AuthenticatedUser caller, Long coeId) {
        LabResponse lab;
        try {
            lab = labService.getLabById(labId, caller);
        } catch (ResourceNotFoundException ex) {
            throw labNotFound(labId);
        }

        // A platform-wide caller can see labs of every COE, so the COE is checked here too
        if (!Objects.equals(lab.getCoeId(), coeId)) {
            throw labNotFound(labId);
        }
        if (!lab.isActive()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "Lab '" + lab.getName() + "' is inactive");
        }
        return lab;
    }

    // One message for every way a mentor can be unusable, so a caller cannot find out
    // which user ids exist in other COEs
    private UserResponse loadMentor(Long mentorId, Long coeId) {
        UserResponse mentor = userService.getUsersByIds(Set.of(mentorId)).get(mentorId);

        if (mentor == null
                || mentor.getRole() != Role.MENTOR
                || !Objects.equals(mentor.getCoeId(), coeId)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "Mentor with id " + mentorId + " was not found in this COE");
        }
        if (!mentor.isActive()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "Mentor " + mentor.getUserCode() + " is inactive");
        }
        return mentor;
    }

    private BusinessException labNotFound(Long labId) {
        return new BusinessException(HttpStatus.BAD_REQUEST,
                "Lab with id " + labId + " was not found in this COE");
    }

    private void requireOpenBatch(BatchResponse batch, String what) {
        if (batch.getStatus() != BatchStatus.PLANNED && batch.getStatus() != BatchStatus.ONGOING) {
            throw new BusinessException(HttpStatus.CONFLICT,
                    "Cannot " + what + " a " + batch.getStatus() + " batch");
        }
    }

    private void requireActiveCoe(Long coeId) {
        if (!coeService.isActiveCoe(coeId)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "The COE is inactive, so sessions cannot be scheduled or changed");
        }
    }

    private void validateTimes(LocalTime start, LocalTime end) {
        // Sessions never cross midnight, and a class needs some length
        if (!end.isAfter(start)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "End time must be after start time");
        }
    }

    private void validateDateInBatch(LocalDate date, BatchResponse batch) {
        if (date.isBefore(batch.getStartDate()) || date.isAfter(batch.getEndDate())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "Session date must be within the batch dates ("
                            + batch.getStartDate() + " to " + batch.getEndDate() + ")");
        }
    }

    private void checkLabCapacity(LabResponse lab, BatchResponse batch) {
        if (enforceLabCapacity && batch.getMaxLearners() > lab.getCapacity()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "Lab '" + lab.getName() + "' has " + lab.getCapacity() + " seats, but batch '"
                            + batch.getName() + "' can have up to " + batch.getMaxLearners() + " learners");
        }
    }

    // ---------- the schedule check ----------

    // Must be called after the COE lock is taken, so nobody can save a clashing session, or
    // record an absence for this mentor, between this check and our own save.
    //
    // It reports everything that is wrong in one message, so the user can fix it all in one go:
    // the mentor's recorded absences (when checkMentorAbsences is true) and every clash with
    // another SCHEDULED session. A cancelled or completed session holds no slot.
    private void checkSchedulable(Long coeId, LocalDate date, LocalTime start, LocalTime end,
                                  Long mentorId, String mentorLabel, Long labId, String labName,
                                  Long batchId, String batchName, Long excludeId,
                                  boolean checkMentorAbsences) {
        List<String> lines = new ArrayList<>();

        // NEW: the mentor's recorded absences that cover this day (only active records count)
        if (checkMentorAbsences) {
            for (MentorUnavailability absence : unavailabilityRepository.findOverlapping(mentorId, date, date)) {
                lines.add("Mentor " + mentorLabel + " is unavailable (" + absence.getType() + ", "
                        + absence.getFromDate() + " to " + absence.getToDate() + ")");
            }
        }

        List<TrainingSession> conflicts = sessionRepository.findConflicts(
                coeId, date, start, end, mentorId, labId, batchId, SessionStatus.SCHEDULED, excludeId);

        // Say exactly what clashes with what
        for (TrainingSession other : conflicts) {
            String when = other.getSessionCode() + " (" + other.getTitle() + ") from "
                    + other.getStartTime() + " to " + other.getEndTime();

            if (other.getMentorId().equals(mentorId)) {
                lines.add("Mentor " + mentorLabel + " is already booked for " + when);
            }
            if (other.getLabId().equals(labId)) {
                lines.add("Lab '" + labName + "' is already booked for " + when);
            }
            if (other.getBatchId().equals(batchId)) {
                lines.add("Batch '" + batchName + "' already has " + when);
            }
        }

        if (lines.isEmpty()) {
            return;
        }

        List<String> shown = lines.size() > MAX_CLASH_LINES ? lines.subList(0, MAX_CLASH_LINES) : lines;
        String more = lines.size() > MAX_CLASH_LINES
                ? " (and " + (lines.size() - MAX_CLASH_LINES) + " more)"
                : "";

        throw new BusinessException(HttpStatus.CONFLICT,
                "Schedule clash on " + date + ": " + String.join("; ", shown) + more);
    }

    // ---------- scope rules ----------

    // Get: a session outside the caller's COE looks like it does not exist
    private TrainingSession findInScope(Long id, AuthenticatedUser caller) {
        TrainingSession session = sessionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Session", id));
        return checkScope(session, caller);
    }

    // Edit, cancel, complete: the same check, and the row is locked until the transaction ends
    private TrainingSession findInScopeForUpdate(Long id, AuthenticatedUser caller) {
        TrainingSession session = sessionRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Session", id));
        return checkScope(session, caller);
    }

    private TrainingSession checkScope(TrainingSession session, AuthenticatedUser caller) {
        if (!caller.isPlatformWide() && !Objects.equals(caller.getCoeId(), session.getCoeId())) {
            throw new ResourceNotFoundException("Session", session.getId());
        }
        return session;
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
                    "You can only view sessions in your own COE");
        }
        return caller.getCoeId();
    }

    // ---------- building responses ----------

    // The names and codes of the batch, lab and mentor for a group of sessions, each looked
    // up once for the whole group
    @Getter
    @RequiredArgsConstructor
    private static class Lookups {
        private final Map<Long, String> batchNames;
        private final Map<Long, String> labNames;
        private final Map<Long, UserResponse> mentors;
    }

    private Lookups lookups(Collection<TrainingSession> sessions) {
        Set<Long> batchIds = sessions.stream().map(TrainingSession::getBatchId).collect(Collectors.toSet());
        Set<Long> labIds = sessions.stream().map(TrainingSession::getLabId).collect(Collectors.toSet());
        Set<Long> mentorIds = sessions.stream().map(TrainingSession::getMentorId).collect(Collectors.toSet());

        return new Lookups(
                batchLookupService.getBatchNames(batchIds),
                labLookupService.getLabNames(labIds),
                userService.getUsersByIds(mentorIds));
    }

    private SessionResponse toResponse(TrainingSession session, Lookups lookups) {
        UserResponse mentor = lookups.getMentors().get(session.getMentorId());
        return SessionResponse.from(
                session,
                lookups.getBatchNames().get(session.getBatchId()),
                lookups.getLabNames().get(session.getLabId()),
                mentor == null ? null : mentor.getUserCode(),
                mentor == null ? null : mentor.getFullName());
    }

    private List<SessionResponse> toResponses(List<TrainingSession> sessions) {
        Lookups lookups = lookups(sessions);
        return sessions.stream().map(session -> toResponse(session, lookups)).toList();
    }

    // ---------- private helpers ----------

    private String mentorLabel(UserResponse mentor) {
        return mentor.getFullName() + " (" + mentor.getUserCode() + ")";
    }

    // The mentor may be missing if the user record was removed. The session still works.
    private String mentorLabel(UserResponse mentor, Long mentorId) {
        return mentor == null ? "id " + mentorId : mentorLabel(mentor);
    }

    private String temporaryCode() {
        // max 30 chars: "TMP-" (4) + 20 = 24
        return "TMP-" + UUID.randomUUID().toString().replace("-", "").substring(0, 20);
    }

    private String buildSessionCode(Long id) {
        return "SES-" + String.format("%04d", id);
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