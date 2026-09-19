package com.hms.billing.dto;

import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;

public record FinalizeBillRequest(
        @DecimalMin("0.0") BigDecimal discountAmount,
        @DecimalMin("0.0") BigDecimal taxAmount
) {}
