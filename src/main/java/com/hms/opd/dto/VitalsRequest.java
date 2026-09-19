package com.hms.opd.dto;

import java.math.BigDecimal;

public record VitalsRequest(String bp, String pulse, String temperature, BigDecimal weightKg, BigDecimal heightCm, String spo2) {}
