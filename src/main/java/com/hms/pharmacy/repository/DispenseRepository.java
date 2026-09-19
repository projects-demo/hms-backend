package com.hms.pharmacy.repository;

import com.hms.pharmacy.entity.Dispense;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DispenseRepository extends JpaRepository<Dispense, Long> {
    Page<Dispense> findByPatientId(Long patientId, Pageable pageable);
}
