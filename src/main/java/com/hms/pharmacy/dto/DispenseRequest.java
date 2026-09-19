package com.hms.pharmacy.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record DispenseRequest(
        @NotNull Long patientId,
        Long prescriptionId,
        @NotEmpty @Valid List<ItemRequest> items
) {
    public record ItemRequest(@NotNull Long drugId, @Min(1) int quantity) {}
}
