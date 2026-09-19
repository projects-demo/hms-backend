package com.hms.billing.dto;

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
        @NotEmpty @Valid List<BillItemRequest> items
) {
    public record BillItemRequest(Long chargeMasterId, @jakarta.validation.constraints.NotBlank String description,
                                   @jakarta.validation.constraints.Min(1) int quantity,
                                   @NotNull @jakarta.validation.constraints.DecimalMin("0.0") java.math.BigDecimal unitPrice) {}
}
