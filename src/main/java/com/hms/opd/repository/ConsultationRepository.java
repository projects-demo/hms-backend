package com.hms.opd.repository;

import com.hms.opd.entity.Consultation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ConsultationRepository extends JpaRepository<Consultation, Long> {
    Optional<Consultation> findByAppointmentId(Long appointmentId);
    Page<Consultation> findByPatientId(Long patientId, Pageable pageable);
    Page<Consultation> findByDoctorId(Long doctorId, Pageable pageable);
}
