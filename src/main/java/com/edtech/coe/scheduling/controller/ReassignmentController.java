package com.edtech.coe.scheduling.controller;

import com.edtech.coe.iam.dto.AuthenticatedUser;
import com.edtech.coe.scheduling.dto.MentorChangeResponse;
import com.edtech.coe.scheduling.dto.PersonSummary;
import com.edtech.coe.scheduling.dto.ReassignMentorRequest;
import com.edtech.coe.scheduling.service.ReassignmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ReassignmentController {

    private final ReassignmentService reassignmentService;

    // The mentors who could take this session: active, not away that day, not teaching then
    @GetMapping("/api/v1/sessions/{sessionId}/available-mentors")
    public ResponseEntity<List<PersonSummary>> getAvailableMentors(
            @PathVariable Long sessionId,
            @AuthenticationPrincipal AuthenticatedUser caller) {
        return ResponseEntity.ok(reassignmentService.getAvailableMentors(sessionId, caller));
    }

    // Swaps the mentor and writes a history row, in one transaction
    @PostMapping("/api/v1/sessions/{sessionId}/reassign-mentor")
    public ResponseEntity<MentorChangeResponse> reassignMentor(
            @PathVariable Long sessionId,
            @Valid @RequestBody ReassignMentorRequest request,
            @AuthenticationPrincipal AuthenticatedUser caller) {
        return ResponseEntity.ok(reassignmentService.reassignMentor(sessionId, request, caller));
    }

    // Everyone who has taught this session, newest change first
    @GetMapping("/api/v1/sessions/{sessionId}/mentor-changes")
    public ResponseEntity<List<MentorChangeResponse>> getSessionHistory(
            @PathVariable Long sessionId,
            @AuthenticationPrincipal AuthenticatedUser caller) {
        return ResponseEntity.ok(reassignmentService.getSessionHistory(sessionId, caller));
    }

    // The history across a COE, newest change first
    @GetMapping("/api/v1/mentor-changes")
    public ResponseEntity<Page<MentorChangeResponse>> getChanges(
            @RequestParam(required = false) Long coeId,
            @RequestParam(required = false) Long sessionId,
            @RequestParam(required = false) Long mentorId,
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal AuthenticatedUser caller) {
        return ResponseEntity.ok(reassignmentService.getChanges(coeId, sessionId, mentorId, pageable, caller));
    }
}