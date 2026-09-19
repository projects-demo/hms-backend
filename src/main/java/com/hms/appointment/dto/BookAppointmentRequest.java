package com.hms.appointment.dto;

import com.hms.appointment.entity.AppointmentType;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record BookAppointmentRequest(
        @NotNull Long patientId,
        @NotNull Long doctorId,
        Long slotId,
        @NotNull LocalDate appointmentDate,
        @NotNull AppointmentType appointmentType,
        boolean freeVisit,
        String notes
) {}
