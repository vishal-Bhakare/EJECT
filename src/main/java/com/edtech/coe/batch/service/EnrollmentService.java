package com.edtech.coe.batch.service;

import com.edtech.coe.batch.dto.ChangeEnrollmentStatusRequest;
import com.edtech.coe.batch.dto.EnrollLearnerRequest;
import com.edtech.coe.batch.dto.EnrollmentResponse;
import com.edtech.coe.batch.entity.EnrollmentStatus;
import com.edtech.coe.iam.dto.AuthenticatedUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface EnrollmentService {

    // Also brings a dropped learner back into the same batch
    EnrollmentResponse enrollLearner(Long batchId, EnrollLearnerRequest request,
                                     AuthenticatedUser caller);

    Page<EnrollmentResponse> getBatchEnrollments(Long batchId, EnrollmentStatus status,
                                                 Pageable pageable, AuthenticatedUser caller);

    // Drops a learner, or marks a learner as completed
    EnrollmentResponse changeStatus(Long batchId, Long enrollmentId,
                                    ChangeEnrollmentStatusRequest request,
                                    AuthenticatedUser caller);
}