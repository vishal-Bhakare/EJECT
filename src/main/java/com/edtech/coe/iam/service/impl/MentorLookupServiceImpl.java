package com.edtech.coe.iam.service.impl;

import com.edtech.coe.common.exception.BusinessException;
import com.edtech.coe.iam.dto.AuthenticatedUser;
import com.edtech.coe.iam.dto.MentorSummaryResponse;
import com.edtech.coe.iam.dto.UserResponse;
import com.edtech.coe.iam.entity.Role;
import com.edtech.coe.iam.repository.UserRepository;
import com.edtech.coe.iam.service.MentorLookupService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MentorLookupServiceImpl implements MentorLookupService {

    private final UserRepository userRepository;

    @Override
    public List<UserResponse> getActiveMentors(Long coeId) {
        return userRepository
                .findByCoeIdAndRoleAndActiveTrueOrderByFullNameAsc(coeId, Role.MENTOR)
                .stream()
                .map(UserResponse::from)
                .toList();
    }

    @Override
    public List<MentorSummaryResponse> searchActiveMentors(Long coeId, AuthenticatedUser caller) {
        return getActiveMentors(resolveCoe(coeId, caller)).stream()
                .map(MentorSummaryResponse::from)
                .toList();
    }

    // Platform-wide roles must say which COE. Everyone else is held to their own.
    private Long resolveCoe(Long requestedCoeId, AuthenticatedUser caller) {
        if (caller.isPlatformWide()) {
            // Listing the mentors of every COE would be a long list nobody can use in a
            // dropdown, so the caller picks one
            if (requestedCoeId == null) {
                throw new BusinessException(HttpStatus.BAD_REQUEST,
                        "Choose a COE to list its mentors");
            }
            return requestedCoeId;
        }
        // Without this check, a missing coeId would turn into "no filter" and list every COE
        if (caller.getCoeId() == null) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "Your account is not linked to a COE");
        }
        if (requestedCoeId != null && !requestedCoeId.equals(caller.getCoeId())) {
            throw new BusinessException(HttpStatus.FORBIDDEN,
                    "You can only view mentors in your own COE");
        }
        return caller.getCoeId();
    }
}