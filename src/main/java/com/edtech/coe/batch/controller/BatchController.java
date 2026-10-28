package com.edtech.coe.batch.controller;

import com.edtech.coe.batch.dto.BatchResponse;
import com.edtech.coe.batch.dto.ChangeBatchStatusRequest;
import com.edtech.coe.batch.dto.CreateBatchRequest;
import com.edtech.coe.batch.dto.UpdateBatchRequest;
import com.edtech.coe.batch.entity.BatchStatus;
import com.edtech.coe.batch.service.BatchService;
import com.edtech.coe.iam.dto.AuthenticatedUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/batches")
@RequiredArgsConstructor
public class BatchController {

    private final BatchService batchService;

    @PostMapping
    public ResponseEntity<BatchResponse> createBatch(@Valid @RequestBody CreateBatchRequest request,
                                                     @AuthenticationPrincipal AuthenticatedUser caller) {
        return ResponseEntity.status(HttpStatus.CREATED).body(batchService.createBatch(request, caller));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BatchResponse> getBatchById(@PathVariable Long id,
                                                      @AuthenticationPrincipal AuthenticatedUser caller) {
        return ResponseEntity.ok(batchService.getBatchById(id, caller));
    }

    @GetMapping
    public ResponseEntity<Page<BatchResponse>> getBatches(
            @RequestParam(required = false) Long coeId,
            @RequestParam(required = false) BatchStatus status,
            @RequestParam(required = false) String trackName,
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.ASC) Pageable pageable,
            @AuthenticationPrincipal AuthenticatedUser caller) {
        return ResponseEntity.ok(batchService.getBatches(coeId, status, trackName, pageable, caller));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BatchResponse> updateBatch(@PathVariable Long id,
                                                     @Valid @RequestBody UpdateBatchRequest request,
                                                     @AuthenticationPrincipal AuthenticatedUser caller) {
        return ResponseEntity.ok(batchService.updateBatch(id, request, caller));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<BatchResponse> changeStatus(@PathVariable Long id,
                                                      @Valid @RequestBody ChangeBatchStatusRequest request,
                                                      @AuthenticationPrincipal AuthenticatedUser caller) {
        return ResponseEntity.ok(batchService.changeStatus(id, request, caller));
    }

    // "Delete" cancels the batch. The row is kept.
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBatch(@PathVariable Long id,
                                            @AuthenticationPrincipal AuthenticatedUser caller) {
        batchService.deleteBatch(id, caller);
        return ResponseEntity.noContent().build();
    }
}