package com.edtech.coe.scheduling.service;

import com.edtech.coe.iam.dto.AuthenticatedUser;
import com.edtech.coe.scheduling.dto.MentorChangeResponse;
import com.edtech.coe.scheduling.dto.PersonSummary;
import com.edtech.coe.scheduling.dto.ReassignMentorRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ReassignmentService {

    // The mentors of the session's COE who could take it: active, not away that day, and not
    // teaching another class at that time. The current mentor is left out.
    List<PersonSummary> getAvailableMentors(Long sessionId, AuthenticatedUser caller);

    // Swaps the mentor and writes a history row, in one transaction
    MentorChangeResponse reassignMentor(Long sessionId, ReassignMentorRequest request,
                                        AuthenticatedUser caller);

    // Everyone who has taught this session, newest change first
    List<MentorChangeResponse> getSessionHistory(Long sessionId, AuthenticatedUser caller);

    // The history across a COE, optionally for one session or one mentor
    Page<MentorChangeResponse> getChanges(Long coeId, Long sessionId, Long mentorId,
                                          Pageable pageable, AuthenticatedUser caller);
}