package com.hms.pharmacy.dto;

import com.hms.pharmacy.entity.DrugBatch;

import java.math.BigDecimal;
import java.time.LocalDate;

public record BatchResponse(Long id, Long drugId, String drugName, String batchNumber, LocalDate expiryDate, int quantityAvailable, BigDecimal purchasePrice, BigDecimal sellingPrice) {
    public static BatchResponse from(DrugBatch b) {
        return new BatchResponse(b.getId(), b.getDrug().getId(), b.getDrug().getName(), b.getBatchNumber(), b.getExpiryDate(), b.getQuantityAvailable(), b.getPurchasePrice(), b.getSellingPrice());
    }
}
