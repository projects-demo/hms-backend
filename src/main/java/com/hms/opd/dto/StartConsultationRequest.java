package com.hms.opd.dto;

import jakarta.validation.constraints.NotNull;

public record StartConsultationRequest(
        @NotNull Long appointmentId,
        @NotNull Long patientId,
        @NotNull Long doctorId,
        String chiefComplaint
) {}
