package com.edtech.coe.batch.service.impl;

import com.edtech.coe.batch.entity.Batch;
import com.edtech.coe.batch.repository.BatchRepository;
import com.edtech.coe.batch.service.BatchLookupService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BatchLookupServiceImpl implements BatchLookupService {

    private final BatchRepository batchRepository;

    @Override
    public Map<Long, String> getBatchNames(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        return batchRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Batch::getId, Batch::getName));
    }
}