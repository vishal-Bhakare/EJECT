package com.edtech.coe.iam.controller;

import com.edtech.coe.iam.dto.AuthenticatedUser;
import com.edtech.coe.iam.dto.MentorSummaryResponse;
import com.edtech.coe.iam.service.MentorLookupService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/mentors")
@RequiredArgsConstructor
public class MentorController {

    private final MentorLookupService mentorLookupService;

    // The active mentors of one COE, sorted by name. A COE has a few dozen at most, so
    // there is no paging.
    @GetMapping
    public ResponseEntity<List<MentorSummaryResponse>> getMentors(
            @RequestParam(required = false) Long coeId,
            @AuthenticationPrincipal AuthenticatedUser caller) {
        return ResponseEntity.ok(mentorLookupService.searchActiveMentors(coeId, caller));
    }
}