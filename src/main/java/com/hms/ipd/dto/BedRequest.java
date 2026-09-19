package com.hms.ipd.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record BedRequest(@NotNull Long wardId, @NotBlank String bedNumber, @NotNull @DecimalMin("0.0") BigDecimal dailyRate) {}
