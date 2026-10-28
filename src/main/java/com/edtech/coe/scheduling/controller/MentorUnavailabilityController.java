package com.edtech.coe.scheduling.controller;

import com.edtech.coe.iam.dto.AuthenticatedUser;
import com.edtech.coe.scheduling.dto.CreateUnavailabilityRequest;
import com.edtech.coe.scheduling.dto.UnavailabilityResponse;
import com.edtech.coe.scheduling.service.AvailabilityService;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/mentor-unavailability")
@RequiredArgsConstructor
public class MentorUnavailabilityController {

    private final AvailabilityService availabilityService;

    // The response lists the classes the absence leaves without a teacher (affectedSessions)
    @PostMapping
    public ResponseEntity<UnavailabilityResponse> recordUnavailability(
            @Valid @RequestBody CreateUnavailabilityRequest request,
            @AuthenticationPrincipal AuthenticatedUser caller) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(availabilityService.recordUnavailability(request, caller));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UnavailabilityResponse> getUnavailabilityById(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser caller) {
        return ResponseEntity.ok(availabilityService.getUnavailabilityById(id, caller));
    }

    // Newest start date first. fromDate and toDate select records that touch that window.
    @GetMapping
    public ResponseEntity<Page<UnavailabilityResponse>> getUnavailability(
            @RequestParam(required = false) Long coeId,
            @RequestParam(required = false) Long mentorId,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @PageableDefault(size = 20, sort = {"fromDate", "id"},
                    direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal AuthenticatedUser caller) {
        return ResponseEntity.ok(availabilityService.getUnavailability(
                coeId, mentorId, active, fromDate, toDate, pageable, caller));
    }

    // "Delete" cancels the record. The row is kept.
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelUnavailability(@PathVariable Long id,
                                                     @AuthenticationPrincipal AuthenticatedUser caller) {
        availabilityService.cancelUnavailability(id, caller);
        return ResponseEntity.noContent().build();
    }
}