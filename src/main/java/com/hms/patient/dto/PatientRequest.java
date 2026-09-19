package com.hms.patient.dto;

import com.hms.patient.entity.Gender;
import com.hms.patient.entity.GovernmentIdType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;

import java.time.LocalDate;

public record PatientRequest(
        @NotBlank String firstName,
        @NotBlank String lastName,
        @Past LocalDate dob,
        Short ageYears,
        @NotNull Gender gender,
        @NotBlank String primaryPhone,
        String secondaryPhone,
        String email,
        String emergencyName,
        String emergencyContact,
        GovernmentIdType governmentIdType,
        String governmentIdNumber,
        String insuranceProvider,
        String policyNumber,
        String groupId,
        String street,
        String city,
        String state,
        String country,
        String zipCode,
        String photoUrl
) {}
