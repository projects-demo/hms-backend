package com.hms.pharmacy.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ReceiveBatchRequest(
        @NotNull Long drugId,
        @NotBlank String batchNumber,
        @NotNull @Future LocalDate expiryDate,
        @Min(1) int quantity,
        @NotNull @DecimalMin("0.0") BigDecimal purchasePrice,
        @NotNull @DecimalMin("0.0") BigDecimal sellingPrice
) {}
