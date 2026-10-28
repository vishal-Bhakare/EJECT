package com.edtech.coe.lab.service.impl;

import com.edtech.coe.lab.entity.Lab;
import com.edtech.coe.lab.repository.LabRepository;
import com.edtech.coe.lab.service.LabLookupService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LabLookupServiceImpl implements LabLookupService {

    private final LabRepository labRepository;

    @Override
    public Map<Long, String> getLabNames(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        return labRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Lab::getId, Lab::getName));
    }
}