package com.hms.billing.dto;

import com.hms.billing.entity.DiscountMaster;

import java.math.BigDecimal;

public record DiscountMasterResponse(Long id, String name, BigDecimal percentage, boolean active) {
    public static DiscountMasterResponse from(DiscountMaster discount) {
    return new DiscountMasterResponse(discount.getId(), discount.getName(), discount.getValue(), discount.isActive());
    }
}