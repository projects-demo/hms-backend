package com.hms.tenancy.master.dto;

import jakarta.validation.constraints.NotBlank;

public record PlatformLoginRequest(@NotBlank String username, @NotBlank String password) {}
