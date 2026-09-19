package com.hms.ipd.entity;

import com.hms.common.AuditableEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "admissions")
public class Admission extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "admission_code", nullable = false, unique = true, length = 20)
    private String admissionCode;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "admitting_doctor_id", nullable = false)
    private Long admittingDoctorId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bed_id", nullable = false)
    private Bed bed;

    @Enumerated(EnumType.STRING)
    @Column(name = "admission_type", nullable = false, length = 20)
    private AdmissionType admissionType = AdmissionType.PLANNED;

    @Column(name = "reason_for_admission", length = 1000)
    private String reasonForAdmission;

    @Column(name = "provisional_diagnosis", length = 1000)
    private String provisionalDiagnosis;

    @Column(name = "final_diagnosis", length = 1000)
    private String finalDiagnosis;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AdmissionStatus status = AdmissionStatus.ADMITTED;

    @Column(name = "admission_date", nullable = false)
    private LocalDateTime admissionDate = LocalDateTime.now();

    @Column(name = "expected_discharge_date")
    private LocalDate expectedDischargeDate;

    @Column(name = "discharge_date")
    private LocalDateTime dischargeDate;

    @Column(name = "discharge_summary", columnDefinition = "TEXT")
    private String dischargeSummary;

    @Column(name = "created_by")
    private Long createdBy;
}
