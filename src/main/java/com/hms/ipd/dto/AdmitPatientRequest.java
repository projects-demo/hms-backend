package com.hms.ipd.dto;

import com.hms.ipd.entity.AdmissionType;
import jakarta.validation.constraints.NotNull;

public record AdmitPatientRequest(
        @NotNull Long patientId,
        @NotNull Long admittingDoctorId,
        @NotNull Long bedId,
        @NotNull AdmissionType admissionType,
        String reasonForAdmission,
        String provisionalDiagnosis
) {}
