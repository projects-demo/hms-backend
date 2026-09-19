package com.hms.staff.dto;

import com.hms.staff.entity.StaffProfile;

import java.time.LocalDate;

public record StaffResponse(
        Long id, String staffCode, Long userId, String staffType, Long departmentId,
        String shift, LocalDate joiningDate, boolean active
) {
    public static StaffResponse from(StaffProfile s) {
        return new StaffResponse(s.getId(), s.getStaffCode(), s.getUserId(), s.getStaffType().name(),
                s.getDepartmentId(), s.getShift(), s.getJoiningDate(), s.isActive());
    }
}
