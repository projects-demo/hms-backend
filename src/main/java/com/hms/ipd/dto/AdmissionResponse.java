package com.hms.ipd.dto;

import com.hms.ipd.entity.Admission;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record AdmissionResponse(
        Long id, String admissionCode, Long patientId, Long admittingDoctorId, Long bedId, String bedNumber,
        String admissionType, String reasonForAdmission, String provisionalDiagnosis, String finalDiagnosis,
        String status, LocalDateTime admissionDate, LocalDate expectedDischargeDate, LocalDateTime dischargeDate
) {
    public static AdmissionResponse from(Admission a) {
        return new AdmissionResponse(a.getId(), a.getAdmissionCode(), a.getPatientId(), a.getAdmittingDoctorId(),
                a.getBed().getId(), a.getBed().getBedNumber(), a.getAdmissionType().name(), a.getReasonForAdmission(),
                a.getProvisionalDiagnosis(), a.getFinalDiagnosis(), a.getStatus().name(), a.getAdmissionDate(),
                a.getExpectedDischargeDate(), a.getDischargeDate());
    }
}
