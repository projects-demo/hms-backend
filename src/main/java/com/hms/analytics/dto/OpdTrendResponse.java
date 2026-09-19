package com.hms.analytics.dto;

import java.time.LocalDate;
import java.util.List;

public record OpdTrendResponse(
        LocalDate from, LocalDate to,
        long totalVisits, long newVisits, long followupVisits,
        double newPercent, double followupPercent,
        List<DailyPoint> dailyBreakdown
) {
    public record DailyPoint(LocalDate date, long total, long newCount, long followupCount) {}
}