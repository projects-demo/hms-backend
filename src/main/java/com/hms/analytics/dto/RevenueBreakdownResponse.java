package com.hms.analytics.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record RevenueBreakdownResponse(
        LocalDate from, LocalDate to,
        BigDecimal opdRevenue, BigDecimal ipdRevenue, BigDecimal pharmacyRevenue, BigDecimal totalRevenue,
        BigDecimal subtotalAmount, BigDecimal discountTotal, BigDecimal taxTotal, BigDecimal netRevenue,
        long billCount, long transactionCount,
        List<PaymentModeSlice> paymentModeBreakdown
) {
    public record PaymentModeSlice(String mode, BigDecimal amount, long count) {}
}