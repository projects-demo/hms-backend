package com.hms.billing.dto;

import com.hms.billing.entity.BillReceipt;
import com.hms.billing.entity.Bill;
import com.hms.patient.entity.Patient;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentReceiptResponse(
        Long id,
        String receiptNumber,
        Long billId,
        String billNumber,
        Long patientId,
        String patientName,
        String patientCode,
        String patientPhone,
        Long appointmentId,
        String appointmentCode,
        BigDecimal amount,
        String paymentMode,
        LocalDateTime receivedAt
) {
    public static PaymentReceiptResponse from(BillReceipt receipt, Bill bill, Patient patient, String appointmentCode) {
        return new PaymentReceiptResponse(receipt.getId(), receipt.getReceiptNumber(), bill.getId(), bill.getBillNumber(),
                patient.getId(), patient.getFirstName() + " " + patient.getLastName(), patient.getPatientCode(),
                patient.getPrimaryPhone(), bill.getAppointmentId(), appointmentCode, receipt.getAmount(), receipt.getPaymentMode().name(),
                receipt.getReceivedAt());
    }
}