package com.hms.billing.dto;

import com.hms.billing.entity.AdvisorType;
import com.hms.billing.entity.BillType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CreateBillRequest(
        @NotNull Long patientId,
        Long admissionId,
        Long appointmentId,
        @NotNull BillType billType,
        String comments,
        /** Who referred/advised the patient: INTERNAL_DOCTOR | SELF | EXTERNAL_DOCTOR */
        AdvisorType advisorType,
        /** When advisorType=INTERNAL_DOCTOR, the id of the referring doctor. */
        Long advisorDoctorId,
        /** When advisorType=EXTERNAL_DOCTOR, the free-text name of the external doctor. */
        String externalAdvisorName,
        @NotEmpty @Valid List<BillItemRequest> items
) {
    public record BillItemRequest(Long chargeMasterId, @jakarta.validation.constraints.NotBlank String description,
                                   @jakarta.validation.constraints.Min(1) int quantity,
                                   @NotNull @jakarta.validation.constraints.DecimalMin("0.0") java.math.BigDecimal unitPrice) {}
}
