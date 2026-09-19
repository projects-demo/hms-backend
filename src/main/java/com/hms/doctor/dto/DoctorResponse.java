package com.hms.doctor.dto;

import com.hms.doctor.entity.Doctor;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DoctorResponse(
        Long id, String doctorCode, String fullName, Long userId, Long departmentId, String departmentName,
        String specialization, String licenseNumber, LocalDate licenseExpiryDate, String qualifications,
        int experienceYears, BigDecimal consultationFee, int maxPatientsPerDay, String officeRoomNumber,
        String availabilityStatus, boolean active
) {
    // fullName lives on the linked AppUser, not on Doctor itself - callers must resolve it
    // (see DoctorController, which batch-fetches names to avoid an N+1 lookup per row).
    public static DoctorResponse from(Doctor d, String fullName) {
        return new DoctorResponse(d.getId(), d.getDoctorCode(), fullName, d.getUserId(),
                d.getDepartment().getId(), d.getDepartment().getName(), d.getSpecialization(),
                d.getLicenseNumber(), d.getLicenseExpiryDate(), d.getQualifications(),
                d.getExperienceYears(), d.getConsultationFee(), d.getMaxPatientsPerDay(),
                d.getOfficeRoomNumber(), d.getAvailabilityStatus().name(), d.isActive());
    }
}