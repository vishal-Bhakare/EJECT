package com.edtech.coe.batch.repository;

import com.edtech.coe.batch.entity.Enrollment;
import com.edtech.coe.batch.entity.EnrollmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    // One learner has at most one row per batch, so this finds a returning learner's old row
    Optional<Enrollment> findByBatchIdAndLearnerId(Long batchId, Long learnerId);

    // How many learners hold a seat in the batch (used for the capacity check)
    long countByBatchIdAndStatus(Long batchId, EnrollmentStatus status);

    // Is the learner already in a different batch with this status?
    boolean existsByLearnerIdAndStatusAndBatchIdNot(Long learnerId, EnrollmentStatus status, Long batchId);

    @Query("""
            select e from Enrollment e
            where e.batchId = :batchId
              and (:status is null or e.status = :status)
            """)
    Page<Enrollment> searchByBatch(@Param("batchId") Long batchId,
                                   @Param("status") EnrollmentStatus status,
                                   Pageable pageable);

    Page<Enrollment> findByLearnerId(Long learnerId, Pageable pageable);

    // Seat counts for a whole page of batches in one query, instead of one query per batch.
    // A batch with no matching rows is missing from the result, so the service treats it as 0.
    @Query("""
            select e.batchId as batchId, count(e) as total
            from Enrollment e
            where e.batchId in :batchIds
              and e.status = :status
            group by e.batchId
            """)
    List<BatchEnrollmentCount> countByBatchIds(@Param("batchIds") Collection<Long> batchIds,
                                               @Param("status") EnrollmentStatus status);

    // Moves every enrollment of a batch from one status to another in a single query.
    // A bulk update skips auditing, so updatedAt is set here by hand.
    // flushAutomatically saves pending changes first, so the caller's batch change is not lost.
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update Enrollment e
            set e.status = :newStatus,
                e.statusReason = :reason,
                e.statusChangedAt = :changedAt,
                e.updatedAt = :changedAt
            where e.batchId = :batchId
              and e.status = :currentStatus
            """)
    int changeStatusForBatch(@Param("batchId") Long batchId,
                             @Param("currentStatus") EnrollmentStatus currentStatus,
                             @Param("newStatus") EnrollmentStatus newStatus,
                             @Param("reason") String reason,
                             @Param("changedAt") Instant changedAt);
}