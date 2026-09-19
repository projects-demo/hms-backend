package com.hms.ipd.dto;

import com.hms.ipd.entity.WardType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record WardRequest(@NotBlank String name, @NotNull WardType wardType, String floor) {}
