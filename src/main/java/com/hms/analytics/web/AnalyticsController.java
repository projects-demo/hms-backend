package com.hms.analytics.web;

import com.hms.analytics.dto.*;
import com.hms.analytics.service.AnalyticsService;
import com.hms.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
@Tag(name = "Analytics & Dashboard", description = "Hospital-wide reporting: OPD trends, doctor/department performance, revenue breakdown, demographics")
@PreAuthorize("hasAnyRole('HOSPITAL_ADMIN','BILLING_STAFF')")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/dashboard-summary")
    @Operation(summary = "Today's key operational numbers", description = "Appointments, revenue, bed occupancy, low-stock/expiring drugs - all single aggregate queries, no full-table scans.")
    public ApiResponse<DashboardSummaryResponse> dashboard() {
        return ApiResponse.ok(analyticsService.dashboardSummary());
    }

    @GetMapping("/revenue-report")
    @Operation(summary = "Revenue over a date range, with daily breakdown")
    public ApiResponse<RevenueReportResponse> revenue(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.ok(analyticsService.revenueReport(from, to));
    }

    @GetMapping("/opd-trend")
    @Operation(summary = "OPD visit volume over a date range", description = "Daily new-vs-follow-up visit counts, plus overall totals and percentages.")
    public ApiResponse<OpdTrendResponse> opdTrend(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.ok(analyticsService.opdTrend(from, to));
    }

    @GetMapping("/doctor-performance")
    @Operation(summary = "Visit count and collected revenue per doctor for a date range")
    public ApiResponse<List<DoctorPerformanceResponse>> doctorPerformance(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.ok(analyticsService.doctorPerformance(from, to));
    }

    @GetMapping("/department-performance")
    @Operation(summary = "Visit count and collected revenue per department for a date range")
    public ApiResponse<List<DepartmentPerformanceResponse>> departmentPerformance(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.ok(analyticsService.departmentPerformance(from, to));
    }

    @GetMapping("/revenue-breakdown")
    @Operation(summary = "Revenue by bill type and payment method", description = "OPD/IPD/Pharmacy split, discount/tax totals, net revenue, and payment-mode breakdown.")
    public ApiResponse<RevenueBreakdownResponse> revenueBreakdown(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.ok(analyticsService.revenueBreakdown(from, to));
    }

    @GetMapping("/demographics")
    @Operation(summary = "Patient population by gender and age band", description = "Hospital-wide, not date-filtered - reflects the current patient registry.")
    public ApiResponse<DemographicsResponse> demographics() {
        return ApiResponse.ok(analyticsService.demographics());
    }
}