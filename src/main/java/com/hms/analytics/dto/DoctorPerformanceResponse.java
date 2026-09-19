package com.hms.analytics.dto;

import java.math.BigDecimal;

public record DoctorPerformanceResponse(Long doctorId, String doctorName, String departmentName, long visitCount, BigDecimal revenue) {}