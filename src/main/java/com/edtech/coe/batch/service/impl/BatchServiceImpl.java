package com.edtech.coe.batch.service.impl;

import com.edtech.coe.batch.dto.BatchResponse;
import com.edtech.coe.batch.dto.ChangeBatchStatusRequest;
import com.edtech.coe.batch.dto.CreateBatchRequest;
import com.edtech.coe.batch.dto.UpdateBatchRequest;
import com.edtech.coe.batch.entity.Batch;
import com.edtech.coe.batch.entity.BatchStatus;
import com.edtech.coe.batch.entity.EnrollmentStatus;
import com.edtech.coe.batch.repository.BatchEnrollmentCount;
import com.edtech.coe.batch.repository.BatchRepository;
import com.edtech.coe.batch.repository.EnrollmentRepository;
import com.edtech.coe.batch.service.BatchService;
import com.edtech.coe.coe.service.CoeService;
import com.edtech.coe.common.exception.BusinessException;
import com.edtech.coe.common.exception.ResourceNotFoundException;
import com.edtech.coe.iam.dto.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BatchServiceImpl implements BatchService {

    // The lifecycle: which status may follow which. COMPLETED and CANCELLED are final.
    private static final Map<BatchStatus, Set<BatchStatus>> ALLOWED_TRANSITIONS = Map.of(
            BatchStatus.PLANNED, EnumSet.of(BatchStatus.ONGOING, BatchStatus.CANCELLED),
            BatchStatus.ONGOING, EnumSet.of(BatchStatus.COMPLETED, BatchStatus.CANCELLED),
            BatchStatus.COMPLETED, EnumSet.noneOf(BatchStatus.class),
            BatchStatus.CANCELLED, EnumSet.noneOf(BatchStatus.class)
    );

    private final BatchRepository batchRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final CoeService coeService;

    @Override
    @Transactional
    public BatchResponse createBatch(CreateBatchRequest request, AuthenticatedUser caller) {
        Long coeId = request.getCoeId();

        // Scope first: a caller must not learn anything about other COEs
        requireOwnCoe(caller, coeId);

        if (!coeService.isActiveCoe(coeId)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "COE with id " + coeId + " does not exist or is inactive");
        }

        validateDates(request.getStartDate(), request.getEndDate());

        String name = request.getName().trim();
        if (batchRepository.existsByCoeIdAndName(coeId, name)) {
            throw duplicateName(name);
        }

        Batch batch = Batch.builder()
                .batchCode(temporaryCode())
                .coeId(coeId)
                .name(name)
                .trackName(request.getTrackName().trim())
                .description(trimToNull(request.getDescription()))
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .maxLearners(request.getMaxLearners())
                .build();   // status starts as PLANNED

        // First save gives us the auto-generated id
        Batch saved = batchRepository.save(batch);

        // Build the real code from the id, e.g. BAT-0001
        saved.setBatchCode(buildBatchCode(saved.getId()));

        return BatchResponse.from(saved, 0);   // a new batch has no learners
    }

    @Override
    public BatchResponse getBatchById(Long id, AuthenticatedUser caller) {
        Batch batch = findInScope(id, caller);
        return BatchResponse.from(batch, countEnrolled(batch.getId()));
    }

    @Override
    public Page<BatchResponse> getBatches(Long coeId, BatchStatus status, String trackName,
                                          Pageable pageable, AuthenticatedUser caller) {
        Long effectiveCoeId = resolveListCoe(coeId, caller);

        Page<Batch> batches = batchRepository.search(
                effectiveCoeId, status, trackName == null ? null : trimToNull(trackName), pageable);

        // One grouped query for the whole page, not one query per batch
        Map<Long, Long> counts = enrolledCounts(batches.getContent());

        return batches.map(batch ->
                BatchResponse.from(batch, counts.getOrDefault(batch.getId(), 0L)));
    }

    @Override
    @Transactional
    public BatchResponse updateBatch(Long id, UpdateBatchRequest request, AuthenticatedUser caller) {
        // Locked, so the seat count below cannot change while we check it
        Batch batch = findInScopeForUpdate(id, caller);

        if (batch.getStatus() == BatchStatus.COMPLETED || batch.getStatus() == BatchStatus.CANCELLED) {
            throw new BusinessException(HttpStatus.CONFLICT,
                    "A " + batch.getStatus() + " batch cannot be edited");
        }

        validateDates(request.getStartDate(), request.getEndDate());

        String name = request.getName().trim();

        // The batch may keep its own name, but not take another batch's name in the same COE
        if (batchRepository.existsByCoeIdAndNameAndIdNot(batch.getCoeId(), name, id)) {
            throw duplicateName(name);
        }

        // The batch cannot be made smaller than the number of learners already in it
        long enrolled = countEnrolled(id);
        if (request.getMaxLearners() < enrolled) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "Maximum learners cannot be lower than the " + enrolled
                            + " learners already enrolled");
        }

        // Full replacement: optional fields that are not sent become null
        batch.setName(name);
        batch.setTrackName(request.getTrackName().trim());
        batch.setDescription(trimToNull(request.getDescription()));
        batch.setStartDate(request.getStartDate());
        batch.setEndDate(request.getEndDate());
        batch.setMaxLearners(request.getMaxLearners());

        // saveAndFlush runs auditing now, so the response shows the new updatedAt
        // and a unique-key clash is reported inside this method
        return BatchResponse.from(batchRepository.saveAndFlush(batch), enrolled);
    }

    @Override
    @Transactional
    public BatchResponse changeStatus(Long id, ChangeBatchStatusRequest request, AuthenticatedUser caller) {
        Batch batch = findInScopeForUpdate(id, caller);
        return moveTo(batch, request.getStatus());
    }

    @Override
    @Transactional
    public void deleteBatch(Long id, AuthenticatedUser caller) {
        Batch batch = findInScopeForUpdate(id, caller);

        // Cancelling twice is safe: nothing to do the second time
        if (batch.getStatus() == BatchStatus.CANCELLED) {
            return;
        }

        // A completed batch is rejected here by the lifecycle rules
        moveTo(batch, BatchStatus.CANCELLED);
    }

    // ---------- status lifecycle ----------

    private BatchResponse moveTo(Batch batch, BatchStatus target) {
        BatchStatus current = batch.getStatus();

        if (current == target) {
            throw new BusinessException(HttpStatus.CONFLICT, "Batch is already " + target);
        }
        if (!ALLOWED_TRANSITIONS.get(current).contains(target)) {
            throw new BusinessException(HttpStatus.CONFLICT,
                    "A " + current + " batch cannot be changed to " + target);
        }

        // A batch cannot start inside a COE that is switched off
        if (target == BatchStatus.ONGOING && !coeService.isActiveCoe(batch.getCoeId())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "Cannot start this batch because its COE is inactive");
        }

        batch.setStatus(target);
        Batch saved = batchRepository.saveAndFlush(batch);

        // The learners follow the batch, so none are left "enrolled" in a closed batch.
        // This also frees them to join another batch.
        Instant now = Instant.now();
        if (target == BatchStatus.COMPLETED) {
            enrollmentRepository.changeStatusForBatch(
                    saved.getId(), EnrollmentStatus.ENROLLED, EnrollmentStatus.COMPLETED, null, now);
        } else if (target == BatchStatus.CANCELLED) {
            enrollmentRepository.changeStatusForBatch(
                    saved.getId(), EnrollmentStatus.ENROLLED, EnrollmentStatus.DROPPED,
                    "Batch cancelled", now);
        }

        return BatchResponse.from(saved, countEnrolled(saved.getId()));
    }

    // ---------- scope rules ----------

    // Create: the caller names the COE, so a wrong COE is a clear "not allowed"
    private void requireOwnCoe(AuthenticatedUser caller, Long coeId) {
        if (caller.isPlatformWide()) {
            return;
        }
        if (caller.getCoeId() == null || !caller.getCoeId().equals(coeId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN,
                    "You can only manage batches in your own COE");
        }
    }

    // Get: a batch outside the caller's COE looks like it does not exist
    private Batch findInScope(Long id, AuthenticatedUser caller) {
        Batch batch = batchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Batch", id));
        return checkScope(batch, caller);
    }

    // Update and status changes: the same check, and the row is locked until the
    // transaction ends so an enrollment cannot slip in at the same moment
    private Batch findInScopeForUpdate(Long id, AuthenticatedUser caller) {
        Batch batch = batchRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Batch", id));
        return checkScope(batch, caller);
    }

    private Batch checkScope(Batch batch, AuthenticatedUser caller) {
        if (!caller.isPlatformWide() && !Objects.equals(caller.getCoeId(), batch.getCoeId())) {
            throw new ResourceNotFoundException("Batch", batch.getId());
        }
        return batch;
    }

    // List: platform-wide roles may filter by any COE, everyone else is held to their own
    private Long resolveListCoe(Long requestedCoeId, AuthenticatedUser caller) {
        if (caller.isPlatformWide()) {
            return requestedCoeId;
        }
        // Without this check, a missing coeId would turn into "no filter" and list every COE
        if (caller.getCoeId() == null) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "Your account is not linked to a COE");
        }
        if (requestedCoeId != null && !requestedCoeId.equals(caller.getCoeId())) {
            throw new BusinessException(HttpStatus.FORBIDDEN,
                    "You can only view batches in your own COE");
        }
        return caller.getCoeId();
    }

    // ---------- private helpers ----------

    private long countEnrolled(Long batchId) {
        return enrollmentRepository.countByBatchIdAndStatus(batchId, EnrollmentStatus.ENROLLED);
    }

    private Map<Long, Long> enrolledCounts(List<Batch> batches) {
        if (batches.isEmpty()) {
            return Map.of();
        }
        List<Long> ids = batches.stream().map(Batch::getId).toList();
        return enrollmentRepository.countByBatchIds(ids, EnrollmentStatus.ENROLLED).stream()
                .collect(Collectors.toMap(BatchEnrollmentCount::getBatchId, BatchEnrollmentCount::getTotal));
    }

    private void validateDates(LocalDate start, LocalDate end) {
        // One-day batches are fine, so the same day is allowed
        if (end.isBefore(start)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "End date must not be before start date");
        }
    }

    private BusinessException duplicateName(String name) {
        return new BusinessException(HttpStatus.CONFLICT,
                "A batch named '" + name + "' already exists in this COE");
    }

    private String temporaryCode() {
        // max 30 chars: "TMP-" (4) + 20 = 24
        return "TMP-" + UUID.randomUUID().toString().replace("-", "").substring(0, 20);
    }

    private String buildBatchCode(Long id) {
        return "BAT-" + String.format("%04d", id);
    }

    // Blank or whitespace-only text becomes null, so it is stored as "no value"
    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}