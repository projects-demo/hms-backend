package com.hms.billing.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMax;

import java.math.BigDecimal;

public record DiscountMasterRequest(
        @NotBlank String name,
        @NotNull @DecimalMin("0.01") @DecimalMax("100.00") BigDecimal percentage
) {}