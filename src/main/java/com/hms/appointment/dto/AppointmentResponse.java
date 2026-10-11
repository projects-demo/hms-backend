package com.hms.appointment.dto;

import com.hms.appointment.entity.Appointment;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AppointmentResponse(
        Long id, String appointmentCode, Long patientId, Long doctorId, String doctorName, Long slotId, Long queueNumber,
        LocalDate appointmentDate, String appointmentType, String status,
        BigDecimal feeAmount, boolean freeVisit, boolean paid, String notes
) {
    public static AppointmentResponse from(Appointment a) {
        return from(a, null, null);
    }

    public static AppointmentResponse from(Appointment a, String doctorName, Long queueNumber) {
        return new AppointmentResponse(a.getId(), a.getAppointmentCode(), a.getPatientId(), a.getDoctorId(), doctorName,
                a.getSlotId(), queueNumber, a.getAppointmentDate(), a.getAppointmentType().name(), a.getStatus().name(),
                a.getFeeAmount(), a.isFreeVisit(), a.isPaid(), a.getNotes());
    }
}
