package com.edtech.coe.iam.service.impl;

import com.edtech.coe.common.exception.BusinessException;
import com.edtech.coe.iam.dto.AuthenticatedUser;
import com.edtech.coe.iam.dto.LearnerSummaryResponse;
import com.edtech.coe.iam.entity.Role;
import com.edtech.coe.iam.repository.UserRepository;
import com.edtech.coe.iam.service.LearnerLookupService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LearnerLookupServiceImpl implements LearnerLookupService {

    private static final int MAX_PAGE_SIZE = 100;

    private final UserRepository userRepository;

    @Override
    public Page<LearnerSummaryResponse> searchLearners(Long coeId, String search,
                                                       Pageable pageable, AuthenticatedUser caller) {
        Long effectiveCoeId = resolveCoe(coeId, caller);

        // The caller's own sort and oversized pages are ignored: always by name, at most 100 rows
        Pageable safe = PageRequest.of(
                pageable.getPageNumber(),
                Math.min(pageable.getPageSize(), MAX_PAGE_SIZE),
                Sort.by("fullName", "id"));

        return userRepository
                .searchByRole(Role.LEARNER, effectiveCoeId, trimToNull(search), safe)
                .map(LearnerSummaryResponse::from);
    }

    // Platform-wide roles may look in any COE. Everyone else is held to their own.
    private Long resolveCoe(Long requestedCoeId, AuthenticatedUser caller) {
        if (caller.isPlatformWide()) {
            return requestedCoeId;
        }
        // Without this check, a missing coeId would turn into "no filter" and list every COE
        if (caller.getCoeId() == null) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "Your account is not linked to a COE");
        }
        if (requestedCoeId != null && !requestedCoeId.equals(caller.getCoeId())) {
            throw new BusinessException(HttpStatus.FORBIDDEN,
                    "You can only view learners in your own COE");
        }
        return caller.getCoeId();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}