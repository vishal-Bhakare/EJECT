package com.edtech.coe.scheduling.service.impl;

import com.edtech.coe.batch.dto.BatchResponse;
import com.edtech.coe.batch.entity.BatchStatus;
import com.edtech.coe.batch.service.BatchService;
import com.edtech.coe.coe.service.CoeLockService;
import com.edtech.coe.coe.service.CoeService;
import com.edtech.coe.common.exception.BusinessException;
import com.edtech.coe.common.exception.ResourceNotFoundException;
import com.edtech.coe.iam.dto.AuthenticatedUser;
import com.edtech.coe.iam.dto.UserResponse;
import com.edtech.coe.iam.entity.Role;
import com.edtech.coe.iam.service.MentorLookupService;
import com.edtech.coe.iam.service.UserService;
import com.edtech.coe.scheduling.dto.MentorChangeResponse;
import com.edtech.coe.scheduling.dto.PersonSummary;
import com.edtech.coe.scheduling.dto.ReassignMentorRequest;
import com.edtech.coe.scheduling.entity.MentorUnavailability;
import com.edtech.coe.scheduling.entity.SessionMentorChange;
import com.edtech.coe.scheduling.entity.SessionStatus;
import com.edtech.coe.scheduling.entity.TrainingSession;
import com.edtech.coe.scheduling.event.SessionMentorChangedEvent;
import com.edtech.coe.scheduling.repository.MentorUnavailabilityRepository;
import com.edtech.coe.scheduling.repository.SessionMentorChangeRepository;
import com.edtech.coe.scheduling.repository.TrainingSessionRepository;
import com.edtech.coe.scheduling.service.ReassignmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReassignmentServiceImpl implements ReassignmentService {

    // A refusal message lists at most this many lines, so it stays readable
    private static final int MAX_LINES = 5;

    private final TrainingSessionRepository sessionRepository;
    private final MentorUnavailabilityRepository unavailabilityRepository;
    private final SessionMentorChangeRepository changeRepository;
    private final BatchService batchService;
    private final CoeService coeService;
    private final CoeLockService coeLockService;
    private final UserService userService;
    private final MentorLookupService mentorLookupService;
    private final ApplicationEventPublisher eventPublisher;

    // ---------- who is free ----------

    @Override
    public List<PersonSummary> getAvailableMentors(Long sessionId, AuthenticatedUser caller) {
        TrainingSession session = findInScope(sessionId, caller);
        requireScheduled(session);

        Long coeId = session.getCoeId();

        // Everyone who cannot take it: the current mentor, mentors who are away that day, and
        // mentors who already teach another class during that time. Three small queries,
        // combined here, are easier to read and change than one large one.
        Set<Long> excluded = new HashSet<>();
        excluded.add(session.getMentorId());
        excluded.addAll(unavailabilityRepository.findUnavailableMentorIds(coeId, session.getSessionDate()));
        excluded.addAll(sessionRepository.findBusyMentorIds(
                coeId, session.getSessionDate(), session.getStartTime(), session.getEndTime(),
                SessionStatus.SCHEDULED));

        // The list is advice. reassignMentor checks again under a lock, so a mentor who was
        // booked a moment after this list was drawn is still refused.
        return mentorLookupService.getActiveMentors(coeId).stream()
                .filter(mentor -> !excluded.contains(mentor.getId()))
                .map(mentor -> PersonSummary.of(mentor.getId(), mentor.getUserCode(), mentor.getFullName()))
                .toList();
    }

    // ---------- swapping the mentor ----------

    // READ_COMMITTED: the check below runs after the COE lock is taken, and must see the
    // sessions and absences that the request just ahead of us saved. The default level would
    // show us an older picture. This is the same reason as in SessionServiceImpl.
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public MentorChangeResponse reassignMentor(Long sessionId, ReassignMentorRequest request,
                                               AuthenticatedUser caller) {
        // Locked first, so nobody can edit, cancel or reassign this session at the same moment
        TrainingSession session = findInScopeForUpdate(sessionId, caller);
        requireScheduled(session);

        Long coeId = session.getCoeId();
        BatchResponse batch = batchService.getBatchById(session.getBatchId(), caller);
        requireOpenBatch(batch);
        requireActiveCoe(coeId);

        Long oldMentorId = session.getMentorId();
        UserResponse newMentor = loadMentor(request.getNewMentorId(), coeId);

        if (newMentor.getId().equals(oldMentorId)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "Mentor " + label(newMentor) + " already teaches this session");
        }

        // From here, one request at a time per COE until this transaction ends. Booking a
        // session and recording an absence take the same lock, so the new mentor cannot be
        // booked or marked away between this check and our own save.
        coeLockService.lock(coeId);
        checkMentorFree(session, newMentor);

        session.setMentorId(newMentor.getId());
        TrainingSession saved = sessionRepository.saveAndFlush(session);

        // The history row is written in the same transaction as the swap, so either both
        // happen or neither does
        SessionMentorChange change = changeRepository.save(SessionMentorChange.builder()
                .sessionId(saved.getId())
                .coeId(coeId)
                .oldMentorId(oldMentorId)
                .newMentorId(newMentor.getId())
                .reason(trimToNull(request.getReason()))
                .changedBy(caller.getUserId())
                .build());

        // Tells the rest of the application. Nothing listens yet. The notification module
        // will, and it will wait for the commit, so nobody is told about a swap that rolled back.
        eventPublisher.publishEvent(new SessionMentorChangedEvent(
                saved.getId(), saved.getSessionCode(), coeId, saved.getBatchId(),
                oldMentorId, newMentor.getId(), change.getReason(), caller.getUserId(),
                saved.getSessionDate(), saved.getStartTime(), saved.getEndTime()));

        return toResponses(List.of(change), Map.of(saved.getId(), saved)).get(0);
    }

    // ---------- history ----------

    @Override
    public List<MentorChangeResponse> getSessionHistory(Long sessionId, AuthenticatedUser caller) {
        TrainingSession session = findInScope(sessionId, caller);

        List<SessionMentorChange> changes = changeRepository.findBySessionIdOrderByIdDesc(session.getId());
        return toResponses(changes, Map.of(session.getId(), session));
    }

    @Override
    public Page<MentorChangeResponse> getChanges(Long coeId, Long sessionId, Long mentorId,
                                                 Pageable pageable, AuthenticatedUser caller) {
        Long effectiveCoeId = resolveListCoe(coeId, caller);

        Page<SessionMentorChange> page = changeRepository.search(effectiveCoeId, sessionId, mentorId, pageable);

        // One lookup for the whole page, not one per row
        Map<Long, TrainingSession> sessions = sessionsById(page.getContent());
        Map<Long, UserResponse> people = peopleFor(page.getContent());

        return page.map(change -> toResponse(change, sessions, people));
    }

    // ---------- checks ----------

    private void requireScheduled(TrainingSession session) {
        if (session.getStatus() != SessionStatus.SCHEDULED) {
            throw new BusinessException(HttpStatus.CONFLICT,
                    "A " + session.getStatus() + " session has no mentor to replace");
        }
    }

    private void requireOpenBatch(BatchResponse batch) {
        if (batch.getStatus() != BatchStatus.PLANNED && batch.getStatus() != BatchStatus.ONGOING) {
            throw new BusinessException(HttpStatus.CONFLICT,
                    "Cannot change the mentor of a session in a " + batch.getStatus()
                            + " batch. Cancel the session instead");
        }
    }

    private void requireActiveCoe(Long coeId) {
        if (!coeService.isActiveCoe(coeId)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "The COE is inactive, so sessions cannot be scheduled or changed");
        }
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

    // Must be called after the COE lock is taken. Only the new mentor is checked: the lab and
    // the batch already hold this slot with this very session, so nothing about them changes.
    private void checkMentorFree(TrainingSession session, UserResponse mentor) {
        List<String> lines = new ArrayList<>();
        String who = label(mentor);

        // The mentor's recorded absences that cover this day (only active records count)
        for (MentorUnavailability absence : unavailabilityRepository.findOverlapping(
                mentor.getId(), session.getSessionDate(), session.getSessionDate())) {
            lines.add("Mentor " + who + " is unavailable (" + absence.getType() + ", "
                    + absence.getFromDate() + " to " + absence.getToDate() + ")");
        }

        // Other SCHEDULED sessions at overlapping times. This session is left out so it does
        // not clash with itself. The query also returns lab and batch matches, so the lines
        // are kept only where the mentor is the match.
        List<TrainingSession> conflicts = sessionRepository.findConflicts(
                session.getCoeId(), session.getSessionDate(), session.getStartTime(), session.getEndTime(),
                mentor.getId(), session.getLabId(), session.getBatchId(),
                SessionStatus.SCHEDULED, session.getId());

        for (TrainingSession other : conflicts) {
            if (other.getMentorId().equals(mentor.getId())) {
                lines.add("Mentor " + who + " is already booked for " + other.getSessionCode()
                        + " (" + other.getTitle() + ") from " + other.getStartTime()
                        + " to " + other.getEndTime());
            }
        }

        if (lines.isEmpty()) {
            return;
        }

        List<String> shown = lines.size() > MAX_LINES ? lines.subList(0, MAX_LINES) : lines;
        String more = lines.size() > MAX_LINES ? " (and " + (lines.size() - MAX_LINES) + " more)" : "";

        throw new BusinessException(HttpStatus.CONFLICT,
                "Mentor cannot take this session on " + session.getSessionDate() + ": "
                        + String.join("; ", shown) + more);
    }

    // ---------- scope rules ----------

    // Read: a session outside the caller's COE looks like it does not exist
    private TrainingSession findInScope(Long id, AuthenticatedUser caller) {
        TrainingSession session = sessionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Session", id));
        return checkScope(session, caller);
    }

    // Reassign: the same check, and the row is locked until the transaction ends
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
                    "You can only view mentor changes in your own COE");
        }
        return caller.getCoeId();
    }

    // ---------- building responses ----------

    private Map<Long, TrainingSession> sessionsById(Collection<SessionMentorChange> changes) {
        Set<Long> ids = changes.stream().map(SessionMentorChange::getSessionId).collect(Collectors.toSet());
        return sessionRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(TrainingSession::getId, Function.identity()));
    }

    private Map<Long, UserResponse> peopleFor(Collection<SessionMentorChange> changes) {
        Set<Long> ids = new HashSet<>();
        for (SessionMentorChange change : changes) {
            ids.add(change.getOldMentorId());
            ids.add(change.getNewMentorId());
            ids.add(change.getChangedBy());
        }
        return userService.getUsersByIds(ids);
    }

    private List<MentorChangeResponse> toResponses(List<SessionMentorChange> changes,
                                                   Map<Long, TrainingSession> sessions) {
        Map<Long, UserResponse> people = peopleFor(changes);
        return changes.stream().map(change -> toResponse(change, sessions, people)).toList();
    }

    private MentorChangeResponse toResponse(SessionMentorChange change,
                                            Map<Long, TrainingSession> sessions,
                                            Map<Long, UserResponse> people) {
        return MentorChangeResponse.from(change,
                sessions.get(change.getSessionId()),
                summary(change.getOldMentorId(), people),
                summary(change.getNewMentorId(), people),
                summary(change.getChangedBy(), people));
    }

    // A person may be missing if the user record was removed. The row still shows, with
    // empty details.
    private PersonSummary summary(Long id, Map<Long, UserResponse> people) {
        UserResponse user = people.get(id);
        return user == null
                ? PersonSummary.of(id, null, null)
                : PersonSummary.of(id, user.getUserCode(), user.getFullName());
    }

    // ---------- helpers ----------

    private String label(UserResponse mentor) {
        return mentor.getFullName() + " (" + mentor.getUserCode() + ")";
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