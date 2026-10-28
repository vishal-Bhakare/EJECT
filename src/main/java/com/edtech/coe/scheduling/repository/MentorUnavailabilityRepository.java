package com.edtech.coe.scheduling.repository;

import com.edtech.coe.scheduling.entity.MentorUnavailability;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface MentorUnavailabilityRepository extends JpaRepository<MentorUnavailability, Long> {

    // Active records of this mentor that overlap the date range. Both ends of every range are
    // included, so a record for 3 to 5 February and a new one for 5 to 7 February DO overlap
    // (they share the 5th). Two ranges overlap when each one starts on or before the day the
    // other one ends.
    @Query("""
            select u from MentorUnavailability u
            where u.mentorId = :mentorId
              and u.active = true
              and u.fromDate <= :toDate
              and u.toDate >= :fromDate
            order by u.fromDate, u.id
            """)
    List<MentorUnavailability> findOverlapping(@Param("mentorId") Long mentorId,
                                               @Param("fromDate") LocalDate fromDate,
                                               @Param("toDate") LocalDate toDate);

    // How many active records cover this day for this mentor. Zero means the mentor is
    // available. The service only asks whether the answer is above zero.
    @Query("""
            select count(u) from MentorUnavailability u
            where u.mentorId = :mentorId
              and u.active = true
              and u.fromDate <= :date
              and u.toDate >= :date
            """)
    long countCovering(@Param("mentorId") Long mentorId,
                       @Param("date") LocalDate date);

    // The mentors of a COE who are away on this day. Used to leave them out of the list of
    // free mentors.
    @Query("""
            select distinct u.mentorId from MentorUnavailability u
            where u.coeId = :coeId
              and u.active = true
              and u.fromDate <= :date
              and u.toDate >= :date
            """)
    List<Long> findUnavailableMentorIds(@Param("coeId") Long coeId,
                                        @Param("date") LocalDate date);

    // Lists records filtered by any combination of the arguments. A filter left null is
    // ignored. fromDate and toDate select records that touch that window, so a record for
    // 3 to 5 February shows up for a window of 4 to 10 February.
    @Query("""
            select u from MentorUnavailability u
            where (:coeId is null or u.coeId = :coeId)
              and (:mentorId is null or u.mentorId = :mentorId)
              and (:active is null or u.active = :active)
              and (:fromDate is null or u.toDate >= :fromDate)
              and (:toDate is null or u.fromDate <= :toDate)
            """)
    Page<MentorUnavailability> search(@Param("coeId") Long coeId,
                                      @Param("mentorId") Long mentorId,
                                      @Param("active") Boolean active,
                                      @Param("fromDate") LocalDate fromDate,
                                      @Param("toDate") LocalDate toDate,
                                      Pageable pageable);
}