package com.edtech.coe.scheduling.controller;

import com.edtech.coe.iam.dto.AuthenticatedUser;
import com.edtech.coe.scheduling.dto.ChangeSessionStatusRequest;
import com.edtech.coe.scheduling.dto.CreateSessionRequest;
import com.edtech.coe.scheduling.dto.SessionResponse;
import com.edtech.coe.scheduling.dto.UpdateSessionRequest;
import com.edtech.coe.scheduling.entity.SessionStatus;
import com.edtech.coe.scheduling.service.SessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/sessions")
@RequiredArgsConstructor
public class SessionController {

    private final SessionService sessionService;

    @PostMapping
    public ResponseEntity<SessionResponse> createSession(@Valid @RequestBody CreateSessionRequest request,
                                                         @AuthenticationPrincipal AuthenticatedUser caller) {
        return ResponseEntity.status(HttpStatus.CREATED).body(sessionService.createSession(request, caller));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SessionResponse> getSessionById(@PathVariable Long id,
                                                          @AuthenticationPrincipal AuthenticatedUser caller) {
        return ResponseEntity.ok(sessionService.getSessionById(id, caller));
    }

    // Sorted like a timetable: by date, then start time. fromDate and toDate are both inclusive.
    @GetMapping
    public ResponseEntity<Page<SessionResponse>> getSessions(
            @RequestParam(required = false) Long coeId,
            @RequestParam(required = false) Long batchId,
            @RequestParam(required = false) Long labId,
            @RequestParam(required = false) Long mentorId,
            @RequestParam(required = false) SessionStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @PageableDefault(size = 20, sort = {"sessionDate", "startTime", "id"},
                    direction = Sort.Direction.ASC) Pageable pageable,
            @AuthenticationPrincipal AuthenticatedUser caller) {
        return ResponseEntity.ok(sessionService.getSessions(
                coeId, batchId, labId, mentorId, status, fromDate, toDate, pageable, caller));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SessionResponse> updateSession(@PathVariable Long id,
                                                         @Valid @RequestBody UpdateSessionRequest request,
                                                         @AuthenticationPrincipal AuthenticatedUser caller) {
        return ResponseEntity.ok(sessionService.updateSession(id, request, caller));
    }

    // Completes or cancels a session
    @PatchMapping("/{id}/status")
    public ResponseEntity<SessionResponse> changeStatus(@PathVariable Long id,
                                                        @Valid @RequestBody ChangeSessionStatusRequest request,
                                                        @AuthenticationPrincipal AuthenticatedUser caller) {
        return ResponseEntity.ok(sessionService.changeStatus(id, request, caller));
    }

    // "Delete" cancels the session. The row is kept.
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSession(@PathVariable Long id,
                                              @AuthenticationPrincipal AuthenticatedUser caller) {
        sessionService.deleteSession(id, caller);
        return ResponseEntity.noContent().build();
    }
}