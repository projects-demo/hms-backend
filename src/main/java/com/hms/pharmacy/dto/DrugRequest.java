package com.hms.pharmacy.dto;

import jakarta.validation.constraints.NotBlank;

public record DrugRequest(@NotBlank String name, String genericName, String manufacturer, String category, String unit, int reorderLevel) {}
