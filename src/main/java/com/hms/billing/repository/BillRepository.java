package com.hms.billing.repository;

import com.hms.billing.entity.Bill;
import com.hms.billing.entity.BillStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BillRepository extends JpaRepository<Bill, Long> {

    Optional<Bill> findByBillNumber(String billNumber);

    Optional<Bill> findByAppointmentId(Long appointmentId);

    Optional<Bill> findFirstByAppointmentIdAndStatusNot(Long appointmentId, BillStatus status);

    boolean existsByAppointmentIdAndStatusNot(Long appointmentId, BillStatus status);

    boolean existsByAppointmentIdAndStatusNotAndIdNot(Long appointmentId, BillStatus status, Long id);

    @Query("""
           select b from Bill b
           where (:patientId is null or b.patientId = :patientId)
             and (:status is null or b.status = :status)
             and (:q is null or lower(b.billNumber) like lower(concat('%', :q, '%'))
                             or lower(b.comments) like lower(concat('%', :q, '%'))
                             or exists (
                                   select p.id from Patient p where p.id = b.patientId and (
                                         lower(concat(p.firstName, ' ', p.lastName)) like lower(concat('%', :q, '%'))
                                         or p.primaryPhone like concat('%', :q, '%')
                                         or lower(p.patientCode) like lower(concat('%', :q, '%'))
                                         or str(p.id) = :q
                                   )
                             ))
           """)
    Page<Bill> search(@Param("patientId") Long patientId, @Param("status") BillStatus status,
                      @Param("q") String q, Pageable pageable);

    @Query("select coalesce(sum(b.totalAmount), 0) from Bill b where b.status = 'FINALIZED' and b.createdAt between :from and :to")
    java.math.BigDecimal sumRevenueBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query(value = """
           select date(b.created_at) as day, sum(b.total_amount) as revenue
           from bills b
           where b.status = 'FINALIZED' and b.created_at between :from and :to
           group by date(b.created_at)
           order by day
           """, nativeQuery = true)
    List<DailyRevenueRow> dailyRevenueBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    interface DailyRevenueRow {
        java.sql.Date getDay();
        java.math.BigDecimal getRevenue();
    }

    // --- Revenue breakdown by bill type (OPD/IPD/PHARMACY), for the reporting module ---
    @Query(value = """
           select bill_type as billType, coalesce(sum(total_amount), 0) as revenue
           from bills
           where status = 'FINALIZED' and created_at between :from and :to
           group by bill_type
           """, nativeQuery = true)
    List<RevenueByTypeRow> revenueByType(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query(value = """
           select coalesce(sum(discount_amount), 0) as discountTotal,
                  coalesce(sum(tax_amount), 0) as taxTotal,
                  coalesce(sum(subtotal_amount), 0) as subtotalTotal,
                  count(*) as billCount,
                  coalesce(sum(case when comments = 'free visit' or comments like '%free visit%' then 1 else 0 end), 0) as freeBillCount
           from bills
           where status = 'FINALIZED' and created_at between :from and :to
           """, nativeQuery = true)
    BillTotalsRow billTotals(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    interface RevenueByTypeRow {
        String getBillType();
        java.math.BigDecimal getRevenue();
    }

    interface BillTotalsRow {
        java.math.BigDecimal getDiscountTotal();
        java.math.BigDecimal getTaxTotal();
        java.math.BigDecimal getSubtotalTotal();
        Long getBillCount();
        Long getFreeBillCount();
    }
}