package com.hms.billing.dto;

import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;

public record FinalizeBillRequest(
        Long discountMasterId,
        @DecimalMin("0.0") BigDecimal taxAmount
) {}
