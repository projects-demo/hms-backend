package com.hms.ipd.dto;

import com.hms.ipd.entity.IpdRoundNote;

import java.time.LocalDateTime;

public record RoundNoteResponse(Long id, Long admissionId, LocalDateTime recordedAt, String bp, String pulse, String temperature, String spo2, String notes) {
    public static RoundNoteResponse from(IpdRoundNote n) {
        return new RoundNoteResponse(n.getId(), n.getAdmissionId(), n.getRecordedAt(), n.getBp(), n.getPulse(), n.getTemperature(), n.getSpo2(), n.getNotes());
    }
}
