package com.hms.ipd.dto;

import com.hms.ipd.entity.Bed;

import java.math.BigDecimal;

public record BedResponse(Long id, Long wardId, String wardName, String bedNumber, String status, BigDecimal dailyRate) {
    public static BedResponse from(Bed b) {
        return new BedResponse(b.getId(), b.getWard().getId(), b.getWard().getName(), b.getBedNumber(), b.getStatus().name(), b.getDailyRate());
    }
}
