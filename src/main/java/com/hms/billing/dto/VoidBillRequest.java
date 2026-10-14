package com.hms.billing.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VoidBillRequest(
        @NotBlank @Size(max = 100) String password
) {}