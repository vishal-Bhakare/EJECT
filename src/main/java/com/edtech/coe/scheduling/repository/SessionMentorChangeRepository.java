package com.edtech.coe.scheduling.repository;

import com.edtech.coe.scheduling.entity.SessionMentorChange;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SessionMentorChangeRepository extends JpaRepository<SessionMentorChange, Long> {

    // Everyone who has taught this session, newest change first. A session is rarely
    // reassigned more than a few times, so this returns a short list and needs no paging.
    // Ordered by id, not by time, because ids always go up and two changes made in the same
    // second would otherwise come back in an unpredictable order.
    List<SessionMentorChange> findBySessionIdOrderByIdDesc(Long sessionId);

    // Lists changes filtered by any combination of the arguments. A filter left null is
    // ignored. mentorId matches a change in which that mentor was either replaced or
    // brought in.
    @Query("""
            select c from SessionMentorChange c
            where (:coeId is null or c.coeId = :coeId)
              and (:sessionId is null or c.sessionId = :sessionId)
              and (:mentorId is null or c.oldMentorId = :mentorId or c.newMentorId = :mentorId)
            """)
    Page<SessionMentorChange> search(@Param("coeId") Long coeId,
                                     @Param("sessionId") Long sessionId,
                                     @Param("mentorId") Long mentorId,
                                     Pageable pageable);
}