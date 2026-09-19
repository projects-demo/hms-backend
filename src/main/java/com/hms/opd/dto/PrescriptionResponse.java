package com.hms.opd.dto;

import com.hms.opd.entity.Prescription;

import java.util.List;

public record PrescriptionResponse(Long id, Long consultationId, List<ItemDto> items) {
    public record ItemDto(Long id, String drugName, String dosage, String frequency, String duration, String instructions) {}

    public static PrescriptionResponse from(Prescription p) {
        return new PrescriptionResponse(p.getId(), p.getConsultationId(),
                p.getItems().stream().map(i -> new ItemDto(i.getId(), i.getDrugName(), i.getDosage(), i.getFrequency(), i.getDuration(), i.getInstructions())).toList());
    }
}
