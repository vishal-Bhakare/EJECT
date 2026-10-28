package com.edtech.coe.iam.service;

import com.edtech.coe.iam.dto.AuthenticatedUser;
import com.edtech.coe.iam.dto.LearnerSummaryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface LearnerLookupService {

    Page<LearnerSummaryResponse> searchLearners(Long coeId, String search,
                                                Pageable pageable, AuthenticatedUser caller);
}