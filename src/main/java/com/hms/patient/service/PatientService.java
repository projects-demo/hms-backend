package com.hms.patient.service;

import com.hms.common.codeseq.CodeGeneratorService;
import com.hms.common.dto.OptionDto;
import com.hms.common.exception.ResourceNotFoundException;
import com.hms.patient.dto.PatientRequest;
import com.hms.patient.entity.Patient;
import com.hms.patient.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional("transactionManager")
public class PatientService {

    private final PatientRepository patientRepository;
    private final CodeGeneratorService codeGeneratorService;

    public Patient create(PatientRequest request, Long actorUserId) {
        Patient p = new Patient();
        apply(p, request);
        p.setPatientCode(codeGeneratorService.next("PATIENT", "PT", 6));
        p.setCreatedBy(actorUserId);
        p.setUpdatedBy(actorUserId);
        return patientRepository.save(p);
    }

    public Patient update(Long id, PatientRequest request, Long actorUserId) {
        Patient p = getOrThrow(id);
        apply(p, request);
        p.setUpdatedBy(actorUserId);
        return patientRepository.save(p);
    }

    @Transactional(value = "transactionManager", readOnly = true)
    public Patient getOrThrow(Long id) {
        return patientRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Patient", id));
    }

    @Transactional(value = "transactionManager", readOnly = true)
    public Page<Patient> search(String q, Pageable pageable) {
        if (q == null || q.isBlank()) {
            return patientRepository.findAll(pageable);
        }
        return patientRepository.search(q.trim(), pageable);
    }

    @Transactional(value = "transactionManager", readOnly = true)
    public List<OptionDto> autocomplete(String q, int limit) {
        if (q == null || q.isBlank()) return List.of();
        Pageable cap = PageRequest.of(0, Math.min(limit, 20), Sort.by("firstName"));
        return patientRepository.autocomplete(q.trim(), cap);
    }

    public void delete(Long id) {
        Patient p = getOrThrow(id);
        patientRepository.delete(p);
    }

    private void apply(Patient p, PatientRequest r) {
        p.setFirstName(r.firstName());
        p.setLastName(r.lastName());
        p.setDob(r.dob());
        p.setAgeYears(r.ageYears());
        p.setGender(r.gender());
        p.setPrimaryPhone(r.primaryPhone());
        p.setSecondaryPhone(r.secondaryPhone());
        p.setEmail(r.email());
        p.setEmergencyName(r.emergencyName());
        p.setEmergencyContact(r.emergencyContact());
        p.setGovernmentIdType(r.governmentIdType());
        p.setGovernmentIdNumber(r.governmentIdNumber());
        p.setInsuranceProvider(r.insuranceProvider());
        p.setPolicyNumber(r.policyNumber());
        p.setGroupId(r.groupId());
        p.setStreet(r.street());
        p.setCity(r.city());
        p.setState(r.state());
        p.setCountry(r.country());
        p.setZipCode(r.zipCode());
        p.setPhotoUrl(r.photoUrl());
    }
}
