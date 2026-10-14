package com.hms.billing.repository;

import com.hms.billing.entity.BillReceipt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface BillReceiptRepository extends JpaRepository<BillReceipt, Long> {
    List<BillReceipt> findByBillId(Long billId);

    @Query(value = """
            select r.id as receiptId, r.receipt_number as receiptNumber, b.id as billId,
                   b.bill_number as billNumber, p.id as patientId,
                   concat(p.first_name, ' ', p.last_name) as patientName, p.patient_code as patientCode,
                   p.primary_phone as patientPhone, b.appointment_id as appointmentId,
                     a.appointment_code as appointmentCode,
                   r.amount as amount, r.payment_mode as paymentMode, r.received_at as receivedAt
            from bill_receipts r
            join bills b on b.id = r.bill_id
            join patients p on p.id = b.patient_id
                 left join appointments a on a.id = b.appointment_id
            where b.patient_id = :patientId
            order by r.received_at desc, r.id desc
            """, countQuery = """
            select count(*)
            from bill_receipts r
            join bills b on b.id = r.bill_id
            where b.patient_id = :patientId
            """, nativeQuery = true)
    Page<PatientReceiptRow> findPatientReceiptHistory(@Param("patientId") Long patientId, Pageable pageable);

    interface PatientReceiptRow {
        Long getReceiptId();
        String getReceiptNumber();
        Long getBillId();
        String getBillNumber();
        Long getPatientId();
        String getPatientName();
        String getPatientCode();
        String getPatientPhone();
        Long getAppointmentId();
        String getAppointmentCode();
        java.math.BigDecimal getAmount();
        String getPaymentMode();
        LocalDateTime getReceivedAt();
    }

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