package com.hms.doctor.dto;

import com.hms.doctor.entity.Department;

public record DepartmentResponse(Long id, String name, String description, boolean active) {
    public static DepartmentResponse from(Department d) {
        return new DepartmentResponse(d.getId(), d.getName(), d.getDescription(), d.isActive());
    }
}
