package com.hms.appointment.dto;

import com.hms.appointment.entity.Appointment;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

public record AppointmentResponse(
        Long id, String appointmentCode, Long patientId, String patientName, Integer patientAge,
        String patientGender, String patientPhone, Long doctorId, String doctorName, Long slotId,
        LocalTime slotStartTime, LocalTime slotEndTime, Long queueNumber,
        LocalDate appointmentDate, String appointmentType, String status,
        BigDecimal feeAmount, boolean freeVisit, boolean paid, String notes
) {
    public static AppointmentResponse from(Appointment a) {
        return from(a, null, null);
    }

    public static AppointmentResponse from(Appointment a, String doctorName, Long queueNumber) {
        return from(a, null, null, null, null, doctorName, queueNumber, null, null);
    }

    public static AppointmentResponse from(Appointment a, String doctorName, Long queueNumber,
                                           LocalTime slotStartTime, LocalTime slotEndTime) {
        return from(a, null, null, null, null, doctorName, queueNumber, slotStartTime, slotEndTime);
    }

    public static AppointmentResponse from(Appointment a, String patientName, String doctorName, Long queueNumber,
                                           LocalTime slotStartTime, LocalTime slotEndTime) {
        return from(a, patientName, null, null, null, doctorName, queueNumber, slotStartTime, slotEndTime);
    }

    public static AppointmentResponse from(Appointment a, String patientName, Integer patientAge,
                                           String patientGender, String patientPhone, String doctorName,
                                           Long queueNumber, LocalTime slotStartTime, LocalTime slotEndTime) {
        return new AppointmentResponse(a.getId(), a.getAppointmentCode(), a.getPatientId(), patientName,
                patientAge, patientGender, patientPhone, a.getDoctorId(), doctorName,
                a.getSlotId(), slotStartTime, slotEndTime, queueNumber, a.getAppointmentDate(),
                a.getAppointmentType().name(), a.getStatus().name(), a.getFeeAmount(), a.isFreeVisit(),
                a.isPaid(), a.getNotes());
    }
}
