package com.edtech.coe.batch.service.impl;

import com.edtech.coe.batch.dto.ChangeEnrollmentStatusRequest;
import com.edtech.coe.batch.dto.EnrollLearnerRequest;
import com.edtech.coe.batch.dto.EnrollmentResponse;
import com.edtech.coe.batch.entity.Batch;
import com.edtech.coe.batch.entity.BatchStatus;
import com.edtech.coe.batch.entity.Enrollment;
import com.edtech.coe.batch.entity.EnrollmentStatus;
import com.edtech.coe.batch.repository.BatchRepository;
import com.edtech.coe.batch.repository.EnrollmentRepository;
import com.edtech.coe.batch.service.BatchService;
import com.edtech.coe.batch.service.EnrollmentService;
import com.edtech.coe.coe.service.CoeService;
import com.edtech.coe.common.exception.BusinessException;
import com.edtech.coe.common.exception.ResourceNotFoundException;
import com.edtech.coe.iam.dto.AuthenticatedUser;
import com.edtech.coe.iam.dto.UserResponse;
import com.edtech.coe.iam.entity.Role;
import com.edtech.coe.iam.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EnrollmentServiceImpl implements EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final BatchRepository batchRepository;
    private final BatchService batchService;
    private final UserService userService;
    private final CoeService coeService;

    // false = a learner can be ENROLLED in only one batch at a time
    @Value("${app.enrollment.allow-multiple-active-batches:false}")
    private boolean allowMultipleActiveBatches;

    @Override
    @Transactional
    public EnrollmentResponse enrollLearner(Long batchId, EnrollLearnerRequest request,
                                            AuthenticatedUser caller) {
        Batch batch = lockBatchInScope(batchId, caller);

        if (batch.getStatus() != BatchStatus.PLANNED && batch.getStatus() != BatchStatus.ONGOING) {
            throw new BusinessException(HttpStatus.CONFLICT,
                    "Cannot enroll learners into a " + batch.getStatus() + " batch");
        }

        // New learners cannot join a COE that is switched off
        if (!coeService.isActiveCoe(batch.getCoeId())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "Cannot enroll learners because the COE is inactive");
        }

        UserResponse learner = findLearner(request.getLearnerId(), batch.getCoeId());
        if (!learner.isActive()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "Learner " + learner.getUserCode() + " is inactive");
        }

        // One row per learner per batch, ever. A dropped learner returns to the same row.
        Enrollment enrollment = enrollmentRepository
                .findByBatchIdAndLearnerId(batchId, learner.getId())
                .orElse(null);

        if (enrollment != null) {
            if (enrollment.getStatus() == EnrollmentStatus.ENROLLED) {
                throw new BusinessException(HttpStatus.CONFLICT,
                        "Learner " + learner.getUserCode() + " is already enrolled in this batch");
            }
            if (enrollment.getStatus() == EnrollmentStatus.COMPLETED) {
                throw new BusinessException(HttpStatus.CONFLICT,
                        "Learner " + learner.getUserCode() + " has already completed this batch");
            }
        }

        if (!allowMultipleActiveBatches
                && enrollmentRepository.existsByLearnerIdAndStatusAndBatchIdNot(
                        learner.getId(), EnrollmentStatus.ENROLLED, batchId)) {
            throw new BusinessException(HttpStatus.CONFLICT,
                    "Learner " + learner.getUserCode() + " is already enrolled in another batch");
        }

        // Counted while the batch row is locked, so two requests cannot both take the last seat
        long taken = enrollmentRepository.countByBatchIdAndStatus(batchId, EnrollmentStatus.ENROLLED);
        if (taken >= batch.getMaxLearners()) {
            throw new BusinessException(HttpStatus.CONFLICT,
                    "Batch is full (" + taken + " of " + batch.getMaxLearners() + " seats taken)");
        }

        if (enrollment == null) {
            enrollment = Enrollment.builder()
                    .batchId(batchId)
                    .learnerId(learner.getId())
                    .build();   // status starts as ENROLLED
        } else {
            // A returning learner: same row, back to ENROLLED
            enrollment.setStatus(EnrollmentStatus.ENROLLED);
            enrollment.setStatusReason(null);
            enrollment.setStatusChangedAt(Instant.now());
        }

        // saveAndFlush runs auditing now, and reports a unique-key clash inside this method
        return toResponse(enrollmentRepository.saveAndFlush(enrollment), learner);
    }

    @Override
    public Page<EnrollmentResponse> getBatchEnrollments(Long batchId, EnrollmentStatus status,
                                                        Pageable pageable, AuthenticatedUser caller) {
        // Scope rule lives in BatchService only. It answers 404 for a batch outside the caller's COE.
        batchService.getBatchById(batchId, caller);

        Page<Enrollment> page = enrollmentRepository.searchByBatch(batchId, status, pageable);

        // One lookup for the whole page, not one per learner
        List<Long> learnerIds = page.getContent().stream().map(Enrollment::getLearnerId).toList();
        Map<Long, UserResponse> learners = userService.getUsersByIds(learnerIds);

        return page.map(enrollment -> toResponse(enrollment, learners.get(enrollment.getLearnerId())));
    }

    @Override
    @Transactional
    public EnrollmentResponse changeStatus(Long batchId, Long enrollmentId,
                                           ChangeEnrollmentStatusRequest request,
                                           AuthenticatedUser caller) {
        Batch batch = lockBatchInScope(batchId, caller);

        // The enrollment must belong to the batch in the URL
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .filter(found -> found.getBatchId().equals(batchId))
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment", enrollmentId));

        EnrollmentStatus current = enrollment.getStatus();
        EnrollmentStatus target = request.getStatus();

        if (target == EnrollmentStatus.ENROLLED) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "To bring a dropped learner back, enroll them again");
        }
        if (current == target) {
            throw new BusinessException(HttpStatus.CONFLICT, "Enrollment is already " + target);
        }
        if (current != EnrollmentStatus.ENROLLED) {
            throw new BusinessException(HttpStatus.CONFLICT,
                    "A " + current + " enrollment cannot be changed to " + target);
        }
        if (target == EnrollmentStatus.COMPLETED && batch.getStatus() != BatchStatus.ONGOING) {
            throw new BusinessException(HttpStatus.CONFLICT,
                    "A learner can only be marked completed while the batch is ONGOING");
        }

        enrollment.setStatus(target);
        // A reason only makes sense for a drop
        enrollment.setStatusReason(target == EnrollmentStatus.DROPPED ? trimToNull(request.getReason()) : null);
        enrollment.setStatusChangedAt(Instant.now());

        Enrollment saved = enrollmentRepository.saveAndFlush(enrollment);
        UserResponse learner = userService.getUsersByIds(Set.of(saved.getLearnerId()))
                .get(saved.getLearnerId());

        return toResponse(saved, learner);
    }

    // ---------- helpers ----------

    // Lock first, then check scope. Locking first means the batch is read fresh from the
    // database while we hold the lock, so its status and seat count cannot change under us.
    // BatchServiceImpl locks the batch before touching enrollments in the same order,
    // so the two can never wait on each other.
    private Batch lockBatchInScope(Long batchId, AuthenticatedUser caller) {
        Batch batch = batchRepository.findByIdForUpdate(batchId)
                .orElseThrow(() -> new ResourceNotFoundException("Batch", batchId));

        // Scope rule lives in BatchService only. It answers 404 for a batch outside the caller's COE.
        batchService.getBatchById(batchId, caller);
        return batch;
    }

    // One message for every way a learner can be unusable, so a caller cannot
    // find out which user ids exist in other COEs
    private UserResponse findLearner(Long learnerId, Long coeId) {
        UserResponse learner = userService.getUsersByIds(Set.of(learnerId)).get(learnerId);

        if (learner == null
                || learner.getRole() != Role.LEARNER
                || !Objects.equals(learner.getCoeId(), coeId)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "Learner with id " + learnerId + " was not found in this COE");
        }
        return learner;
    }

    // The learner may be missing if the user record was removed. The row still shows.
    private EnrollmentResponse toResponse(Enrollment enrollment, UserResponse learner) {
        return EnrollmentResponse.from(
                enrollment,
                learner == null ? null : learner.getUserCode(),
                learner == null ? null : learner.getFullName(),
                learner == null ? null : learner.getEmail());
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