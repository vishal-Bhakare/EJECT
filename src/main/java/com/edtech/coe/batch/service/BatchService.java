package com.edtech.coe.batch.service;

import com.edtech.coe.batch.dto.BatchResponse;
import com.edtech.coe.batch.dto.ChangeBatchStatusRequest;
import com.edtech.coe.batch.dto.CreateBatchRequest;
import com.edtech.coe.batch.dto.UpdateBatchRequest;
import com.edtech.coe.batch.entity.BatchStatus;
import com.edtech.coe.iam.dto.AuthenticatedUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface BatchService {

    BatchResponse createBatch(CreateBatchRequest request, AuthenticatedUser caller);

    BatchResponse getBatchById(Long id, AuthenticatedUser caller);

    Page<BatchResponse> getBatches(Long coeId, BatchStatus status, String trackName,
                                   Pageable pageable, AuthenticatedUser caller);

    BatchResponse updateBatch(Long id, UpdateBatchRequest request, AuthenticatedUser caller);

    BatchResponse changeStatus(Long id, ChangeBatchStatusRequest request, AuthenticatedUser caller);

    // "Delete" cancels the batch. The row is kept.
    void deleteBatch(Long id, AuthenticatedUser caller);
}