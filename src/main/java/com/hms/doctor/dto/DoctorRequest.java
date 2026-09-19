package com.hms.doctor.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DoctorRequest(
        // Only required when creating a doctor profile for a brand-new user (see DoctorService.create)
        String username, String email, String password, String fullName, String phone,
        @NotNull Long departmentId,
        @NotBlank String specialization,
        @NotBlank String licenseNumber,
        LocalDate licenseIssueDate,
        LocalDate licenseExpiryDate,
        String qualifications,
        @Min(0) int experienceYears,
        @NotNull @DecimalMin("0.0") BigDecimal consultationFee,
        @Min(1) int maxPatientsPerDay,
        String officeRoomNumber
) {}
