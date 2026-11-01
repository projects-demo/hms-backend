package com.hms.opd.dto;

public record NurseVitalsRequest(
        Long appointmentId,
        Long patientId,
        Long doctorId,
        VitalsRequest vitals
) {}
