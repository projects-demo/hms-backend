package com.hms.billing.dto;

import com.hms.billing.entity.Bill;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record BillResponse(
        Long id, String billNumber, Long patientId, String patientName, String patientCode, String patientPhone,
        Long admissionId, Long appointmentId, String appointmentCode, String doctorName,
        String billType, String status, BigDecimal subtotalAmount, BigDecimal discountAmount,
        Long discountMasterId, BigDecimal discountPercentage, BigDecimal taxAmount,
        BigDecimal totalAmount, BigDecimal paidAmount, BigDecimal balanceAmount,
        LocalDateTime finalizedAt, List<ItemDto> items
) {
        public record ItemDto(Long id, Long chargeMasterId, String description, int quantity, BigDecimal unitPrice, BigDecimal amount) {}

    public static BillResponse from(Bill b) {
        List<ItemDto> items = b.getItems().stream()
                .map(i -> new ItemDto(i.getId(), i.getChargeMasterId(), i.getDescription(), i.getQuantity(), i.getUnitPrice(), i.getAmount()))
                .toList();
        return new BillResponse(b.getId(), b.getBillNumber(), b.getPatientId(), null, null, null, b.getAdmissionId(), b.getAppointmentId(), null, null,
                b.getBillType().name(), b.getStatus().name(), b.getSubtotalAmount(), b.getDiscountAmount(),
                b.getDiscountMasterId(), b.getDiscountPercentage(), b.getTaxAmount(), b.getTotalAmount(),
                b.getPaidAmount(), b.getBalanceAmount(), b.getFinalizedAt(), items);
    }

        public static BillResponse from(Bill b, String patientName, String patientCode, String patientPhone, String appointmentCode, String doctorName) {
        List<ItemDto> items = b.getItems().stream()
                .map(i -> new ItemDto(i.getId(), i.getChargeMasterId(), i.getDescription(), i.getQuantity(), i.getUnitPrice(), i.getAmount()))
                .toList();
        return new BillResponse(b.getId(), b.getBillNumber(), b.getPatientId(), patientName, patientCode, patientPhone,
                b.getAdmissionId(), b.getAppointmentId(), appointmentCode, doctorName, b.getBillType().name(), b.getStatus().name(),
                b.getSubtotalAmount(), b.getDiscountAmount(), b.getDiscountMasterId(), b.getDiscountPercentage(),
                b.getTaxAmount(), b.getTotalAmount(), b.getPaidAmount(), b.getBalanceAmount(), b.getFinalizedAt(), items);
    }
}
