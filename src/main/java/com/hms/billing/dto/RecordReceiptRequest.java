package com.hms.billing.dto;

import com.hms.billing.entity.PaymentMode;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record RecordReceiptRequest(@NotNull @DecimalMin("0.01") BigDecimal amount, @NotNull PaymentMode paymentMode) {}
