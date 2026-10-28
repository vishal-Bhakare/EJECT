package com.edtech.coe.scheduling.repository;

import com.edtech.coe.scheduling.entity.SessionStatus;
import com.edtech.coe.scheduling.entity.TrainingSession;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TrainingSessionRepository extends JpaRepository<TrainingSession, Long> {

    // Finds every session of the given status that overlaps the time slot AND uses the same
    // mentor, the same lab or the same batch. The service passes SessionStatus.SCHEDULED,
    // because only scheduled sessions hold their slot.
    //
    // Two time ranges overlap when each one starts before the other one ends. The comparisons
    // are strict (< and >), so a class that ends at 10:00 and a class that starts at 10:00 do
    // not overlap.
    //
    // excludeId is the session being edited, so it does not clash with itself. It is null
    // when a new session is being created.
    @Query("""
            select s from TrainingSession s
            where s.coeId = :coeId
              and s.sessionDate = :sessionDate
              and s.status = :status
              and s.startTime < :endTime
              and s.endTime > :startTime
              and (s.mentorId = :mentorId or s.labId = :labId or s.batchId = :batchId)
              and (:excludeId is null or s.id <> :excludeId)
            order by s.startTime, s.id
            """)
    List<TrainingSession> findConflicts(@Param("coeId") Long coeId,
                                        @Param("sessionDate") LocalDate sessionDate,
                                        @Param("startTime") LocalTime startTime,
                                        @Param("endTime") LocalTime endTime,
                                        @Param("mentorId") Long mentorId,
                                        @Param("labId") Long labId,
                                        @Param("batchId") Long batchId,
                                        @Param("status") SessionStatus status,
                                        @Param("excludeId") Long excludeId);

    // Lists sessions filtered by any combination of the arguments. A filter left null is
    // ignored. fromDate and toDate are both inclusive.
    @Query("""
            select s from TrainingSession s
            where (:coeId is null or s.coeId = :coeId)
              and (:batchId is null or s.batchId = :batchId)
              and (:labId is null or s.labId = :labId)
              and (:mentorId is null or s.mentorId = :mentorId)
              and (:status is null or s.status = :status)
              and (:fromDate is null or s.sessionDate >= :fromDate)
              and (:toDate is null or s.sessionDate <= :toDate)
            """)
    Page<TrainingSession> search(@Param("coeId") Long coeId,
                                 @Param("batchId") Long batchId,
                                 @Param("labId") Long labId,
                                 @Param("mentorId") Long mentorId,
                                 @Param("status") SessionStatus status,
                                 @Param("fromDate") LocalDate fromDate,
                                 @Param("toDate") LocalDate toDate,
                                 Pageable pageable);

    // Loads the session and locks its row until the transaction ends.
    // Used when editing, cancelling or completing a session. It must be called inside
    // a @Transactional method.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from TrainingSession s where s.id = :id")
    Optional<TrainingSession> findByIdForUpdate(@Param("id") Long id);

    // The mentors of a COE who already have a session of the given status that overlaps the
    // time slot. Used to leave them out of the list of free mentors. The overlap rule is the
    // same as in findConflicts: strict comparisons, so back-to-back classes do not count.
    @Query("""
            select distinct s.mentorId from TrainingSession s
            where s.coeId = :coeId
              and s.sessionDate = :sessionDate
              and s.status = :status
              and s.startTime < :endTime
              and s.endTime > :startTime
            """)
    List<Long> findBusyMentorIds(@Param("coeId") Long coeId,
                                 @Param("sessionDate") LocalDate sessionDate,
                                 @Param("startTime") LocalTime startTime,
                                 @Param("endTime") LocalTime endTime,
                                 @Param("status") SessionStatus status);

    // Every session of the given status that a mentor has between two dates, both included,
    // in timetable order. Used to show which sessions a new absence leaves without a teacher.
    @Query("""
            select s from TrainingSession s
            where s.mentorId = :mentorId
              and s.status = :status
              and s.sessionDate >= :fromDate
              and s.sessionDate <= :toDate
            order by s.sessionDate, s.startTime, s.id
            """)
    List<TrainingSession> findByMentorAndDateRange(@Param("mentorId") Long mentorId,
                                                   @Param("fromDate") LocalDate fromDate,
                                                   @Param("toDate") LocalDate toDate,
                                                   @Param("status") SessionStatus status);
}