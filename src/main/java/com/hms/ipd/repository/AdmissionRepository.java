package com.hms.ipd.repository;

import com.hms.ipd.entity.Admission;
import com.hms.ipd.entity.AdmissionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AdmissionRepository extends JpaRepository<Admission, Long> {

    Optional<Admission> findByAdmissionCode(String code);

    @Query("""
           select a from Admission a
           where (:patientId is null or a.patientId = :patientId)
             and (:status is null or a.status = :status)
           """)
    Page<Admission> search(@Param("patientId") Long patientId, @Param("status") AdmissionStatus status, Pageable pageable);

    long countByStatus(AdmissionStatus status);
}
