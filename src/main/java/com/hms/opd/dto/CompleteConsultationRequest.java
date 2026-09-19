package com.hms.opd.dto;

import java.util.List;

public record CompleteConsultationRequest(
        VitalsRequest vitals,
        String diagnosis,
        String clinicalNotes,
        List<PrescriptionItemRequest> prescriptionItems
) {
    public record PrescriptionItemRequest(String drugName, String dosage, String frequency, String duration, String instructions) {}
}
