package com.hms.billing.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ChargeMasterRequest(
        @NotNull Long chargeTypeId, @NotBlank String code, @NotBlank String name, @NotNull @DecimalMin("0.0") BigDecimal defaultPrice
) {}
