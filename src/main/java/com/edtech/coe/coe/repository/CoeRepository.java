package com.edtech.coe.coe.repository;

import com.edtech.coe.coe.entity.Coe;
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
public interface CoeRepository extends JpaRepository<Coe, Long> {

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);

    boolean existsByIdAndActiveTrue(Long id);

    @Query("""
            select c from Coe c
            where (:city is null or lower(c.city) = lower(:city))
              and (:state is null or lower(c.state) = lower(:state))
              and (:active is null or c.active = :active)
            """)
    Page<Coe> search(@Param("city") String city,
                     @Param("state") String state,
                     @Param("active") Boolean active,
                     Pageable pageable);

    // Loads the COE and locks its row until the transaction ends.
    // Used when scheduling, so two requests cannot book the same mentor or lab at once.
    // It must be called inside a @Transactional method.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Coe c where c.id = :id")
    Optional<Coe> findByIdForUpdate(@Param("id") Long id);
}