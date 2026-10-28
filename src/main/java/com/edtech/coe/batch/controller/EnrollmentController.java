package com.edtech.coe.batch.controller;

import com.edtech.coe.batch.dto.ChangeEnrollmentStatusRequest;
import com.edtech.coe.batch.dto.EnrollLearnerRequest;
import com.edtech.coe.batch.dto.EnrollmentResponse;
import com.edtech.coe.batch.entity.EnrollmentStatus;
import com.edtech.coe.batch.service.EnrollmentService;
import com.edtech.coe.iam.dto.AuthenticatedUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/batches/{batchId}/enrollments")
@RequiredArgsConstructor
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    // Also brings a dropped learner back into the same batch
    @PostMapping
    public ResponseEntity<EnrollmentResponse> enrollLearner(@PathVariable Long batchId,
                                                            @Valid @RequestBody EnrollLearnerRequest request,
                                                            @AuthenticationPrincipal AuthenticatedUser caller) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(enrollmentService.enrollLearner(batchId, request, caller));
    }

    @GetMapping
    public ResponseEntity<Page<EnrollmentResponse>> getBatchEnrollments(
            @PathVariable Long batchId,
            @RequestParam(required = false) EnrollmentStatus status,
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.ASC) Pageable pageable,
            @AuthenticationPrincipal AuthenticatedUser caller) {
        return ResponseEntity.ok(
                enrollmentService.getBatchEnrollments(batchId, status, pageable, caller));
    }

    @PatchMapping("/{enrollmentId}/status")
    public ResponseEntity<EnrollmentResponse> changeStatus(
            @PathVariable Long batchId,
            @PathVariable Long enrollmentId,
            @Valid @RequestBody ChangeEnrollmentStatusRequest request,
            @AuthenticationPrincipal AuthenticatedUser caller) {
        return ResponseEntity.ok(
                enrollmentService.changeStatus(batchId, enrollmentId, request, caller));
    }
}