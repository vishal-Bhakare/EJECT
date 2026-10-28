package com.edtech.coe.coe.service;

import com.edtech.coe.coe.dto.CoeResponse;
import com.edtech.coe.coe.dto.CreateCoeRequest;
import com.edtech.coe.coe.dto.UpdateCoeRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CoeService {

    CoeResponse createCoe(CreateCoeRequest request);

    CoeResponse getCoeById(Long id);

    Page<CoeResponse> getCoes(String city, String state, Boolean active, Pageable pageable);

    CoeResponse updateCoe(Long id, UpdateCoeRequest request);

    void deleteCoe(Long id);
    
    boolean isActiveCoe(Long id);
}