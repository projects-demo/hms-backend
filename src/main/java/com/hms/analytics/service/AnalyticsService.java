package com.hms.analytics.service;

import com.hms.analytics.dto.*;
import com.hms.appointment.entity.AppointmentStatus;
import com.hms.appointment.repository.AppointmentRepository;
import com.hms.billing.entity.BillType;
import com.hms.billing.repository.BillReceiptRepository;
import com.hms.billing.repository.BillRepository;
import com.hms.ipd.entity.AdmissionStatus;
import com.hms.ipd.entity.BedStatus;
import com.hms.ipd.repository.AdmissionRepository;
import com.hms.ipd.repository.BedRepository;
import com.hms.patient.repository.PatientRepository;
import com.hms.pharmacy.repository.DrugBatchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Every method here is a read-only aggregate query - deliberately NOT built
 * by pulling full row sets into Java and summing in memory. Each one maps to
 * a small number of GROUP BY queries so the reports stay fast even with years
 * of history in a busy hospital's schema.
 */
@Service
@RequiredArgsConstructor
@Transactional(value = "transactionManager", readOnly = true)
public class AnalyticsService {

    private final AppointmentRepository appointmentRepository;
    private final BillRepository billRepository;
    private final BillReceiptRepository billReceiptRepository;
    private final PatientRepository patientRepository;
    private final AdmissionRepository admissionRepository;
    private final BedRepository bedRepository;
    private final DrugBatchRepository drugBatchRepository;

    private static final List<String> AGE_GROUP_ORDER = List.of("0-17", "18-30", "31-45", "46-60", "61+");

    public DashboardSummaryResponse dashboardSummary() {
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(LocalTime.MAX);

        long todaysAppointments = appointmentRepository.countByAppointmentDate(today);
        long completed = appointmentRepository.countByAppointmentDateAndStatus(today, AppointmentStatus.COMPLETED);
        BigDecimal todaysRevenue = billRepository.sumRevenueBetween(startOfDay, endOfDay);

        long activeAdmissions = admissionRepository.countByStatus(AdmissionStatus.ADMITTED);
        long totalBeds = bedRepository.count();
        long occupiedBeds = bedRepository.countByStatus(BedStatus.OCCUPIED);
        double occupancyPct = totalBeds == 0 ? 0.0 : Math.round((occupiedBeds * 10000.0 / totalBeds)) / 100.0;

        long lowStock = drugBatchRepository.findLowStock().size();
        long expiring = drugBatchRepository.findExpiringBy(today.plusDays(30)).size();

        return new DashboardSummaryResponse(todaysAppointments, completed, todaysRevenue, activeAdmissions,
                totalBeds, occupiedBeds, occupancyPct, lowStock, expiring);
    }

    public RevenueReportResponse revenueReport(LocalDate from, LocalDate to) {
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.atTime(LocalTime.MAX);
        BigDecimal total = billRepository.sumRevenueBetween(start, end);
        var daily = billRepository.dailyRevenueBetween(start, end).stream()
                .map(row -> new RevenueReportResponse.DailyPoint(row.getDay().toLocalDate(), row.getRevenue()))
                .toList();
        return new RevenueReportResponse(from, to, total, daily);
    }

    /** OPD visit volume over a date range, split new vs. follow-up - the "Daily OPD" / "Visit Trend" report. */
    public OpdTrendResponse opdTrend(LocalDate from, LocalDate to) {
        var rows = appointmentRepository.opdTrend(from, to);
        long total = 0, newTotal = 0, followupTotal = 0;
        var daily = new java.util.ArrayList<OpdTrendResponse.DailyPoint>();
        for (var row : rows) {
            long t = row.getTotal(), n = row.getNewCount(), f = row.getFollowupCount();
            total += t; newTotal += n; followupTotal += f;
            daily.add(new OpdTrendResponse.DailyPoint(row.getVisitDate().toLocalDate(), t, n, f));
        }
        double newPct = total == 0 ? 0.0 : Math.round(newTotal * 10000.0 / total) / 100.0;
        double followupPct = total == 0 ? 0.0 : Math.round(followupTotal * 10000.0 / total) / 100.0;
        return new OpdTrendResponse(from, to, total, newTotal, followupTotal, newPct, followupPct, daily);
    }

    /** Visit count + collected revenue per doctor, for the "Doctor-wise" report - sort client-side by whichever column. */
    public List<DoctorPerformanceResponse> doctorPerformance(LocalDate from, LocalDate to) {
        return appointmentRepository.doctorPerformance(from, to).stream()
                .map(r -> new DoctorPerformanceResponse(r.getDoctorId(), r.getDoctorName(), r.getDepartmentName(), r.getVisitCount(), r.getRevenue()))
                .toList();
    }

    /** Same, aggregated at department level - the "Department" report. */
    public List<DepartmentPerformanceResponse> departmentPerformance(LocalDate from, LocalDate to) {
        return appointmentRepository.departmentPerformance(from, to).stream()
                .map(r -> new DepartmentPerformanceResponse(r.getDepartmentId(), r.getDepartmentName(), r.getVisitCount(), r.getRevenue()))
                .toList();
    }

    /** Revenue by bill type + payment method + discount/tax totals - the "Billing Analytics" report. */
    public RevenueBreakdownResponse revenueBreakdown(LocalDate from, LocalDate to) {
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.atTime(LocalTime.MAX);

        Map<String, BigDecimal> byType = billRepository.revenueByType(start, end).stream()
                .collect(java.util.stream.Collectors.toMap(r -> r.getBillType(), r -> r.getRevenue()));
        BigDecimal opd = byType.getOrDefault(BillType.OPD.name(), BigDecimal.ZERO);
        BigDecimal ipd = byType.getOrDefault(BillType.IPD.name(), BigDecimal.ZERO);
        BigDecimal pharmacy = byType.getOrDefault(BillType.PHARMACY.name(), BigDecimal.ZERO);
        BigDecimal total = opd.add(ipd).add(pharmacy);

        var totals = billRepository.billTotals(start, end);
        BigDecimal discount = totals.getDiscountTotal();
        BigDecimal tax = totals.getTaxTotal();
        BigDecimal subtotal = totals.getSubtotalTotal();
        BigDecimal net = total.subtract(discount);

        var paymentModes = billReceiptRepository.paymentModeBreakdown(start, end).stream()
                .map(r -> new RevenueBreakdownResponse.PaymentModeSlice(r.getMode().name(), r.getAmount(), r.getTxnCount()))
                .toList();
        long transactionCount = paymentModes.stream().mapToLong(RevenueBreakdownResponse.PaymentModeSlice::count).sum();

        return new RevenueBreakdownResponse(from, to, opd, ipd, pharmacy, total, subtotal, discount, tax, net,
                totals.getBillCount(), transactionCount, paymentModes);
    }

    /** Patient population breakdown by gender and age band - the "Demographics" report. */
    public DemographicsResponse demographics() {
        var gender = patientRepository.genderBreakdown().stream()
                .map(r -> new DemographicsResponse.GenderSlice(r.getGender().name(), r.getPatientCount()))
                .toList();
        var ageGroups = patientRepository.ageGroupBreakdown().stream()
                .map(r -> new DemographicsResponse.AgeGroupSlice(r.getAgeGroup(), r.getPatientCount()))
                .sorted(Comparator.comparingInt(s -> AGE_GROUP_ORDER.indexOf(s.ageGroup())))
                .toList();
        return new DemographicsResponse(gender, ageGroups);
    }
}