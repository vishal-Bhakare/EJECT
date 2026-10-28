package com.edtech.coe.batch.repository;

import com.edtech.coe.batch.entity.Batch;
import com.edtech.coe.batch.entity.BatchStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BatchRepository extends JpaRepository<Batch, Long> {

    boolean existsByCoeIdAndName(Long coeId, String name);

    boolean existsByCoeIdAndNameAndIdNot(Long coeId, String name, Long id);

    @Query("""
            select b from Batch b
            where (:coeId is null or b.coeId = :coeId)
              and (:status is null or b.status = :status)
              and (:trackName is null or lower(b.trackName) = lower(:trackName))
            """)
    Page<Batch> search(@Param("coeId") Long coeId,
                       @Param("status") BatchStatus status,
                       @Param("trackName") String trackName,
                       Pageable pageable);

    // Loads the batch and locks its row until the transaction ends.
    // Used when enrolling, so two requests cannot both take the last seat.
    // It must be called inside a @Transactional method.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Batch b where b.id = :id")
    Optional<Batch> findByIdForUpdate(@Param("id") Long id);
}