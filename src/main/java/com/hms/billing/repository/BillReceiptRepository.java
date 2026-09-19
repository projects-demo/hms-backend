package com.hms.billing.repository;

import com.hms.billing.entity.BillReceipt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface BillReceiptRepository extends JpaRepository<BillReceipt, Long> {
    List<BillReceipt> findByBillId(Long billId);

    @Query("""
           select r.paymentMode as mode, coalesce(sum(r.amount), 0) as amount, count(r) as txnCount
           from BillReceipt r
           where r.receivedAt between :from and :to
           group by r.paymentMode
           order by amount desc
           """)
    List<PaymentModeRow> paymentModeBreakdown(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    interface PaymentModeRow {
        com.hms.billing.entity.PaymentMode getMode();
        java.math.BigDecimal getAmount();
        Long getTxnCount();
    }
}