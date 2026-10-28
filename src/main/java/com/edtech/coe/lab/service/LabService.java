package com.edtech.coe.lab.service;

import com.edtech.coe.iam.dto.AuthenticatedUser;
import com.edtech.coe.lab.dto.CreateLabRequest;
import com.edtech.coe.lab.dto.LabResponse;
import com.edtech.coe.lab.dto.UpdateLabRequest;
import com.edtech.coe.lab.entity.LabType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface LabService {

    LabResponse createLab(CreateLabRequest request, AuthenticatedUser caller);

    LabResponse getLabById(Long id, AuthenticatedUser caller);

    Page<LabResponse> getLabs(Long coeId, LabType labType, Boolean active,
                              Pageable pageable, AuthenticatedUser caller);

    LabResponse updateLab(Long id, UpdateLabRequest request, AuthenticatedUser caller);

    void deleteLab(Long id, AuthenticatedUser caller);
}