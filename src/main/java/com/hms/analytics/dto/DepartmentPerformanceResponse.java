package com.hms.analytics.dto;

import java.math.BigDecimal;

public record DepartmentPerformanceResponse(Long departmentId, String departmentName, long visitCount, BigDecimal revenue) {}