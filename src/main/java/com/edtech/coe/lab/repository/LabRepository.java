package com.edtech.coe.lab.repository;

import com.edtech.coe.lab.entity.Lab;
import com.edtech.coe.lab.entity.LabType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface LabRepository extends JpaRepository<Lab, Long> {

	boolean existsByCoeIdAndName(Long coeId, String name);

	boolean existsByCoeIdAndNameAndIdNot(Long coeId, String name, Long id);

	@Query("""
			select l from Lab l
			where (:coeId is null or l.coeId = :coeId)
			  and (:labType is null or l.labType = :labType)
			  and (:active is null or l.active = :active)
			""")
	Page<Lab> search(@Param("coeId") Long coeId, @Param("labType") LabType labType, @Param("active") Boolean active,
			Pageable pageable);

	
}