package com.edtech.coe.iam.repository;

import com.edtech.coe.iam.entity.Role;
import com.edtech.coe.iam.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByUserCode(String userCode);

    boolean existsByRole(Role role);

    Page<User> findByRole(Role role, Pageable pageable);

    Page<User> findByCoeId(Long coeId, Pageable pageable);

    Page<User> findByCoeIdAndRole(Long coeId, Role role, Pageable pageable);

    // The active users of one role in one COE, sorted by name. A COE has a few dozen
    // mentors at most, so this list needs no paging.
    List<User> findByCoeIdAndRoleAndActiveTrueOrderByFullNameAsc(Long coeId, Role role);

    // Users of one role, optionally limited to a COE and optionally matched on
    // name, email or user code. A filter left null is ignored.
    @Query("""
            select u from User u
            where u.role = :role
              and (:coeId is null or u.coeId = :coeId)
              and (:search is null
                   or lower(u.fullName) like lower(concat('%', :search, '%'))
                   or lower(u.email) like lower(concat('%', :search, '%'))
                   or lower(u.userCode) like lower(concat('%', :search, '%')))
            """)
    Page<User> searchByRole(@Param("role") Role role,
                            @Param("coeId") Long coeId,
                            @Param("search") String search,
                            Pageable pageable);
}