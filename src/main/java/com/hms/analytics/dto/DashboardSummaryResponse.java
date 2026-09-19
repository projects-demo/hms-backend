package com.hms.analytics.dto;

import java.math.BigDecimal;

public record DashboardSummaryResponse(
        long todaysAppointments,
        long todaysCompletedAppointments,
        BigDecimal todaysRevenue,
        long activeAdmissions,
        long totalBeds,
        long occupiedBeds,
        double bedOccupancyPercent,
        long lowStockDrugBatches,
        long expiringDrugBatches30Days
) {}
