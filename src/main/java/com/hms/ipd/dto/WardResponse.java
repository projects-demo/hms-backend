package com.hms.ipd.dto;

import com.hms.ipd.entity.Ward;

public record WardResponse(Long id, String name, String wardType, String floor, int totalBeds, boolean active) {
    public static WardResponse from(Ward w) {
        return new WardResponse(w.getId(), w.getName(), w.getWardType().name(), w.getFloor(), w.getTotalBeds(), w.isActive());
    }
}
