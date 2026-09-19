package com.hms.billing.dto;

import com.hms.billing.entity.ChargeMaster;

import java.math.BigDecimal;

public record ChargeMasterResponse(Long id, Long chargeTypeId, String chargeTypeName, String code, String name, BigDecimal defaultPrice, boolean active) {
    public static ChargeMasterResponse from(ChargeMaster c) {
        return new ChargeMasterResponse(c.getId(), c.getChargeType().getId(), c.getChargeType().getName(), c.getCode(), c.getName(), c.getDefaultPrice(), c.isActive());
    }
}
