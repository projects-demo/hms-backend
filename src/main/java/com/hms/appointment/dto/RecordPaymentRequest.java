package com.hms.appointment.dto;

import com.hms.appointment.entity.PaymentMode;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record RecordPaymentRequest(@NotNull @DecimalMin("0.01") BigDecimal amount, @NotNull PaymentMode paymentMode) {}
