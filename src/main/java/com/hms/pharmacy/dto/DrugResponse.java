package com.hms.pharmacy.dto;

import com.hms.pharmacy.entity.Drug;

public record DrugResponse(Long id, String drugCode, String name, String genericName, String manufacturer, String category, String unit, int reorderLevel, boolean active) {
    public static DrugResponse from(Drug d) {
        return new DrugResponse(d.getId(), d.getDrugCode(), d.getName(), d.getGenericName(), d.getManufacturer(), d.getCategory(), d.getUnit(), d.getReorderLevel(), d.isActive());
    }
}
