package com.edtech.coe.scheduling.service;

import com.edtech.coe.iam.dto.AuthenticatedUser;
import com.edtech.coe.scheduling.dto.ChangeSessionStatusRequest;
import com.edtech.coe.scheduling.dto.CreateSessionRequest;
import com.edtech.coe.scheduling.dto.SessionResponse;
import com.edtech.coe.scheduling.dto.UpdateSessionRequest;
import com.edtech.coe.scheduling.entity.SessionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface SessionService {

    SessionResponse createSession(CreateSessionRequest request, AuthenticatedUser caller);

    SessionResponse getSessionById(Long id, AuthenticatedUser caller);

    Page<SessionResponse> getSessions(Long coeId, Long batchId, Long labId, Long mentorId,
                                      SessionStatus status, LocalDate fromDate, LocalDate toDate,
                                      Pageable pageable, AuthenticatedUser caller);

    SessionResponse updateSession(Long id, UpdateSessionRequest request, AuthenticatedUser caller);

    // Completes or cancels a session
    SessionResponse changeStatus(Long id, ChangeSessionStatusRequest request, AuthenticatedUser caller);

    // "Delete" cancels the session. The row is kept.
    void deleteSession(Long id, AuthenticatedUser caller);
}