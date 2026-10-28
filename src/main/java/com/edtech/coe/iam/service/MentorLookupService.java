package com.edtech.coe.iam.service;

import com.edtech.coe.iam.dto.AuthenticatedUser;
import com.edtech.coe.iam.dto.MentorSummaryResponse;
import com.edtech.coe.iam.dto.UserResponse;

import java.util.List;

public interface MentorLookupService {

    // The active mentors of one COE, sorted by name. No scope check: for other services.
    List<UserResponse> getActiveMentors(Long coeId);

    // The same list for a screen. It applies the caller's scope rules and returns only
    // an id, a code and a name.
    List<MentorSummaryResponse> searchActiveMentors(Long coeId, AuthenticatedUser caller);
}