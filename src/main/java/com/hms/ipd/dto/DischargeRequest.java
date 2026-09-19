package com.hms.ipd.dto;

import jakarta.validation.constraints.NotBlank;

public record DischargeRequest(@NotBlank String finalDiagnosis, @NotBlank String dischargeSummary) {}
