package com.hms.doctor.service;

import com.hms.common.codeseq.CodeGeneratorService;
import com.hms.common.dto.OptionDto;
import com.hms.common.exception.BusinessRuleException;
import com.hms.common.exception.DuplicateResourceException;
import com.hms.common.exception.ResourceNotFoundException;
import com.hms.doctor.dto.DoctorRequest;
import com.hms.doctor.entity.Department;
import com.hms.doctor.entity.Doctor;
import com.hms.doctor.repository.DepartmentRepository;
import com.hms.doctor.repository.DoctorRepository;
import com.hms.identity.entity.AppUser;
import com.hms.identity.entity.Role;
import com.hms.identity.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional("transactionManager")
public class DoctorService {

    private final DoctorRepository doctorRepository;
    private final DepartmentRepository departmentRepository;
    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final CodeGeneratorService codeGeneratorService;

    /** Creates the login (AppUser, role=DOCTOR) and the Doctor profile together, atomically. */
    public Doctor create(DoctorRequest r) {
        if (r.username() == null || r.password() == null || r.fullName() == null) {
            throw new BusinessRuleException("username, password and fullName are required to create a new doctor");
        }
        if (appUserRepository.existsByUsername(r.username())) {
            throw new DuplicateResourceException("Username '" + r.username() + "' is already taken");
        }
        Department dept = departmentRepository.findById(r.departmentId())
                .orElseThrow(() -> ResourceNotFoundException.of("Department", r.departmentId()));

        AppUser user = new AppUser();
        user.setUsername(r.username());
        user.setEmail(r.email());
        user.setPasswordHash(passwordEncoder.encode(r.password()));
        user.setFullName(r.fullName());
        user.setPhone(r.phone());
        user.setRole(Role.DOCTOR);
        user = appUserRepository.save(user);

        Doctor doctor = new Doctor();
        doctor.setDoctorCode(codeGeneratorService.next("DOCTOR", "DR", 5));
        doctor.setUserId(user.getId());
        doctor.setDepartment(dept);
        apply(doctor, r);
        return doctorRepository.save(doctor);
    }

    public Doctor update(Long id, DoctorRequest r) {
        Doctor doctor = getOrThrow(id);
        if (!doctor.getDepartment().getId().equals(r.departmentId())) {
            Department dept = departmentRepository.findById(r.departmentId())
                    .orElseThrow(() -> ResourceNotFoundException.of("Department", r.departmentId()));
            doctor.setDepartment(dept);
        }
        apply(doctor, r);
        return doctorRepository.save(doctor);
    }

    @Transactional(value = "transactionManager", readOnly = true)
    public Doctor getOrThrow(Long id) {
        return doctorRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Doctor", id));
    }

    @Transactional(value = "transactionManager", readOnly = true)
    public Page<Doctor> search(Long departmentId, String specialization, String q, Pageable pageable) {
        return doctorRepository.search(departmentId, specialization, q, pageable);
    }

    @Transactional(value = "transactionManager", readOnly = true)
    public List<OptionDto> autocompleteByName(String q, int limit) {
        int cap = Math.min(limit, 20);
        return doctorRepository.autocompleteByName(q, cap).stream()
                .map(row -> new OptionDto(row.getId(), row.getLabel(), row.getSubLabel()))
                .toList();
    }

    @Transactional(value = "transactionManager", readOnly = true)
    public List<OptionDto> autocompleteBySpecialization(String q, int limit) {
        return doctorRepository.autocompleteBySpecialization(q, PageRequest.of(0, Math.min(limit, 20)));
    }

    public void setAvailabilityStatus(Long id, String status) {
        Doctor doctor = getOrThrow(id);
        doctor.setAvailabilityStatus(com.hms.doctor.entity.AvailabilityStatus.valueOf(status));
        doctorRepository.save(doctor);
    }

    /**
     * Soft delete only - a doctor is referenced by appointments, consultations, and
     * admissions, so a hard DELETE would either violate a foreign key or silently
     * orphan historical clinical records. Deactivating also logs them out of new
     * bookings (search/autocomplete both filter on active=true) without touching history.
     */
    public void deactivate(Long id) {
        Doctor doctor = getOrThrow(id);
        doctor.setActive(false);
        doctor.setAvailabilityStatus(com.hms.doctor.entity.AvailabilityStatus.INACTIVE);
        doctorRepository.save(doctor);
    }

    private void apply(Doctor doctor, DoctorRequest r) {
        doctor.setSpecialization(r.specialization());
        doctor.setLicenseNumber(r.licenseNumber());
        doctor.setLicenseIssueDate(r.licenseIssueDate());
        doctor.setLicenseExpiryDate(r.licenseExpiryDate());
        doctor.setQualifications(r.qualifications());
        doctor.setExperienceYears(r.experienceYears());
        doctor.setConsultationFee(r.consultationFee());
        doctor.setMaxPatientsPerDay(r.maxPatientsPerDay() == 0 ? 20 : r.maxPatientsPerDay());
        doctor.setOfficeRoomNumber(r.officeRoomNumber());
    }
}