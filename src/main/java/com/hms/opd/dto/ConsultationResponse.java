package com.hms.opd.dto;

import com.hms.opd.entity.Consultation;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ConsultationResponse(
        Long id, Long appointmentId, Long patientId, Long doctorId, String chiefComplaint,
        String bp, String pulse, String temperature, BigDecimal weightKg, BigDecimal heightCm, String spo2,
        String diagnosis, String clinicalNotes, String status, LocalDateTime startedAt, LocalDateTime completedAt
) {
    public static ConsultationResponse from(Consultation c) {
        return new ConsultationResponse(c.getId(), c.getAppointmentId(), c.getPatientId(), c.getDoctorId(),
                c.getChiefComplaint(), c.getBp(), c.getPulse(), c.getTemperature(), c.getWeightKg(), c.getHeightCm(),
                c.getSpo2(), c.getDiagnosis(), c.getClinicalNotes(), c.getStatus().name(), c.getStartedAt(), c.getCompletedAt());
    }
}
