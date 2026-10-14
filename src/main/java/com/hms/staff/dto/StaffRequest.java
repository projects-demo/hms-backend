package com.hms.staff.dto;

import com.hms.staff.entity.StaffType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record StaffRequest(
        String username,
        @NotBlank @Email String email,
        String password, String fullName, String phone,
        @NotNull StaffType staffType,
        Long departmentId,
        String shift,
        LocalDate joiningDate
) {}
