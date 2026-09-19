package com.hms.appointment.dto;

import com.hms.appointment.entity.Appointment;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AppointmentResponse(
        Long id, String appointmentCode, Long patientId, Long doctorId, Long slotId,
        LocalDate appointmentDate, String appointmentType, String status,
        BigDecimal feeAmount, boolean freeVisit, boolean paid, String notes
) {
    public static AppointmentResponse from(Appointment a) {
        return new AppointmentResponse(a.getId(), a.getAppointmentCode(), a.getPatientId(), a.getDoctorId(),
                a.getSlotId(), a.getAppointmentDate(), a.getAppointmentType().name(), a.getStatus().name(),
                a.getFeeAmount(), a.isFreeVisit(), a.isPaid(), a.getNotes());
    }
}
