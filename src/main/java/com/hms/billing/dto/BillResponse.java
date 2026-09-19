package com.hms.billing.dto;

import com.hms.billing.entity.Bill;
import com.hms.billing.entity.BillItem;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record BillResponse(
        Long id, String billNumber, Long patientId, Long admissionId, Long appointmentId,
        String billType, String status, BigDecimal subtotalAmount, BigDecimal discountAmount,
        BigDecimal taxAmount, BigDecimal totalAmount, BigDecimal paidAmount, BigDecimal balanceAmount,
        LocalDateTime finalizedAt, List<ItemDto> items
) {
    public record ItemDto(Long id, String description, int quantity, BigDecimal unitPrice, BigDecimal amount) {}

    public static BillResponse from(Bill b) {
        List<ItemDto> items = b.getItems().stream()
                .map(i -> new ItemDto(i.getId(), i.getDescription(), i.getQuantity(), i.getUnitPrice(), i.getAmount()))
                .toList();
        return new BillResponse(b.getId(), b.getBillNumber(), b.getPatientId(), b.getAdmissionId(), b.getAppointmentId(),
                b.getBillType().name(), b.getStatus().name(), b.getSubtotalAmount(), b.getDiscountAmount(),
                b.getTaxAmount(), b.getTotalAmount(), b.getPaidAmount(), b.getBalanceAmount(), b.getFinalizedAt(), items);
    }
}
