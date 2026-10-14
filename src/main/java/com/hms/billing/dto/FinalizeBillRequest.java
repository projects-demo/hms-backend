package com.hms.billing.dto;

import com.hms.billing.entity.PaymentMode;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record FinalizeBillRequest(
        Long discountMasterId,
        @DecimalMin("0.0") BigDecimal taxAmount,
        @NotNull PaymentMode paymentMode
) {}
