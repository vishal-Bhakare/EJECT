package com.edtech.coe.coe.service.impl;

import com.edtech.coe.coe.repository.CoeRepository;
import com.edtech.coe.coe.service.CoeLockService;
import com.edtech.coe.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CoeLockServiceImpl implements CoeLockService {

    private final CoeRepository coeRepository;

    // MANDATORY: a lock only means something inside the caller's transaction. Called
    // without one, this fails loudly instead of locking for a few microseconds and releasing.
    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void lock(Long coeId) {
        coeRepository.findByIdForUpdate(coeId)
                .orElseThrow(() -> new ResourceNotFoundException("COE", coeId));
    }
}