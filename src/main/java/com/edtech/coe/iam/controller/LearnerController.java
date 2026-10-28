package com.edtech.coe.iam.controller;

import com.edtech.coe.iam.dto.AuthenticatedUser;
import com.edtech.coe.iam.dto.LearnerSummaryResponse;
import com.edtech.coe.iam.service.LearnerLookupService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/learners")
@RequiredArgsConstructor
public class LearnerController {

    private final LearnerLookupService learnerLookupService;

    @GetMapping
    public ResponseEntity<Page<LearnerSummaryResponse>> searchLearners(
            @RequestParam(required = false) Long coeId,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20) Pageable pageable,
            @AuthenticationPrincipal AuthenticatedUser caller) {
        return ResponseEntity.ok(
                learnerLookupService.searchLearners(coeId, search, pageable, caller));
    }
}