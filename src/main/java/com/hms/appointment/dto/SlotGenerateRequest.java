package com.hms.appointment.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/** Generates bookable slots for a doctor on a date from their weekly DoctorAvailability template. */
public record SlotGenerateRequest(@NotNull Long doctorId, @NotNull LocalDate date) {}
