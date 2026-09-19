package com.hms.pharmacy.dto;

import com.hms.pharmacy.entity.Dispense;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record DispenseResponse(Long id, String dispenseCode, Long patientId, BigDecimal totalAmount, LocalDateTime dispensedAt, List<ItemDto> items) {
    public record ItemDto(Long drugId, Long batchId, int quantity, BigDecimal unitPrice, BigDecimal amount) {}

    public static DispenseResponse from(Dispense d) {
        return new DispenseResponse(d.getId(), d.getDispenseCode(), d.getPatientId(), d.getTotalAmount(), d.getDispensedAt(),
                d.getItems().stream().map(i -> new ItemDto(i.getDrugId(), i.getBatchId(), i.getQuantity(), i.getUnitPrice(), i.getAmount())).toList());
    }
}
