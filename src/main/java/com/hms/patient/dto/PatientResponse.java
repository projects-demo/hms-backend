package com.hms.patient.dto;

import com.hms.patient.entity.Patient;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record PatientResponse(
        Long id, String patientCode, String firstName, String lastName, LocalDate dob, Integer age,
        String gender, String primaryPhone, String secondaryPhone, String email,
        String emergencyName, String emergencyContact,
        String governmentIdType, String governmentIdNumber,
        String insuranceProvider, String policyNumber, String groupId,
        String street, String city, String state, String country, String zipCode, String photoUrl,
        LocalDateTime createdAt, LocalDateTime updatedAt
) {
    public static PatientResponse from(Patient p) {
        return new PatientResponse(p.getId(), p.getPatientCode(), p.getFirstName(), p.getLastName(),
                p.getDob(), p.getAge(), p.getGender().name(), p.getPrimaryPhone(), p.getSecondaryPhone(),
                p.getEmail(), p.getEmergencyName(), p.getEmergencyContact(),
                p.getGovernmentIdType() != null ? p.getGovernmentIdType().name() : null,
                p.getGovernmentIdNumber(), p.getInsuranceProvider(), p.getPolicyNumber(), p.getGroupId(),
                p.getStreet(), p.getCity(), p.getState(), p.getCountry(), p.getZipCode(), p.getPhotoUrl(),
                p.getCreatedAt(), p.getUpdatedAt());
    }
}
