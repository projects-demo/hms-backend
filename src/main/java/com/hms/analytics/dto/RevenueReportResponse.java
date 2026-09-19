package com.hms.analytics.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record RevenueReportResponse(LocalDate from, LocalDate to, BigDecimal totalRevenue, List<DailyPoint> dailyBreakdown) {
    public record DailyPoint(LocalDate date, BigDecimal revenue) {}
}
