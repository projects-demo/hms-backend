package com.hms.staff.repository;

import com.hms.staff.entity.StaffProfile;
import com.hms.staff.entity.StaffType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface StaffProfileRepository extends JpaRepository<StaffProfile, Long> {

    Optional<StaffProfile> findByStaffCode(String staffCode);
    Optional<StaffProfile> findByUserId(Long userId);

    @Query("""
           select s from StaffProfile s
           where (:staffType is null or s.staffType = :staffType)
             and (:q is null or lower(s.staffCode) like lower(concat('%', :q, '%')))
           """)
    Page<StaffProfile> search(@Param("staffType") StaffType staffType, @Param("q") String q, Pageable pageable);
}
