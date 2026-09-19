package com.hms.ipd.service;

import com.hms.common.codeseq.CodeGeneratorService;
import com.hms.common.exception.BusinessRuleException;
import com.hms.common.exception.ResourceNotFoundException;
import com.hms.ipd.dto.AdmitPatientRequest;
import com.hms.ipd.dto.DischargeRequest;
import com.hms.ipd.dto.RoundNoteRequest;
import com.hms.ipd.entity.*;
import com.hms.ipd.repository.AdmissionRepository;
import com.hms.ipd.repository.BedRepository;
import com.hms.ipd.repository.IpdRoundNoteRepository;
import com.hms.patient.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional("transactionManager")
public class AdmissionService {

    private final AdmissionRepository admissionRepository;
    private final BedRepository bedRepository;
    private final IpdRoundNoteRepository roundNoteRepository;
    private final PatientRepository patientRepository;
    private final CodeGeneratorService codeGeneratorService;

    public Admission admit(AdmitPatientRequest r, Long actorUserId) {
        patientRepository.findById(r.patientId()).orElseThrow(() -> ResourceNotFoundException.of("Patient", r.patientId()));
        Bed bed = bedRepository.findById(r.bedId()).orElseThrow(() -> ResourceNotFoundException.of("Bed", r.bedId()));
        if (bed.getStatus() != BedStatus.AVAILABLE) {
            // @Version on Bed guards the race where two receptionists admit to the same bed at once.
            throw new BusinessRuleException("Bed " + bed.getBedNumber() + " is not available (status=" + bed.getStatus() + ")");
        }
        bed.setStatus(BedStatus.OCCUPIED);
        bedRepository.save(bed);

        Admission admission = new Admission();
        admission.setAdmissionCode(codeGeneratorService.next("ADMISSION", "ADM", 6));
        admission.setPatientId(r.patientId());
        admission.setAdmittingDoctorId(r.admittingDoctorId());
        admission.setBed(bed);
        admission.setAdmissionType(r.admissionType());
        admission.setReasonForAdmission(r.reasonForAdmission());
        admission.setProvisionalDiagnosis(r.provisionalDiagnosis());
        admission.setStatus(AdmissionStatus.ADMITTED);
        admission.setCreatedBy(actorUserId);
        return admissionRepository.save(admission);
    }

    public Admission discharge(Long admissionId, DischargeRequest r) {
        Admission admission = getOrThrow(admissionId);
        if (admission.getStatus() != AdmissionStatus.ADMITTED) {
            throw new BusinessRuleException("Only an ADMITTED patient can be discharged (current status: " + admission.getStatus() + ")");
        }
        admission.setStatus(AdmissionStatus.DISCHARGED);
        admission.setFinalDiagnosis(r.finalDiagnosis());
        admission.setDischargeSummary(r.dischargeSummary());
        admission.setDischargeDate(LocalDateTime.now());
        admission = admissionRepository.save(admission);

        Bed bed = admission.getBed();
        bed.setStatus(BedStatus.AVAILABLE);
        bedRepository.save(bed);
        return admission;
    }

    public IpdRoundNote addRoundNote(Long admissionId, RoundNoteRequest r, Long actorUserId) {
        Admission admission = getOrThrow(admissionId);
        if (admission.getStatus() != AdmissionStatus.ADMITTED) {
            throw new BusinessRuleException("Cannot add round notes to a non-active admission");
        }
        IpdRoundNote note = new IpdRoundNote();
        note.setAdmissionId(admissionId);
        note.setRecordedBy(actorUserId);
        note.setBp(r.bp());
        note.setPulse(r.pulse());
        note.setTemperature(r.temperature());
        note.setSpo2(r.spo2());
        note.setNotes(r.notes());
        return roundNoteRepository.save(note);
    }

    @Transactional(value = "transactionManager", readOnly = true)
    public Page<IpdRoundNote> roundNotes(Long admissionId, Pageable pageable) {
        return roundNoteRepository.findByAdmissionIdOrderByRecordedAtDesc(admissionId, pageable);
    }

    @Transactional(value = "transactionManager", readOnly = true)
    public Admission getOrThrow(Long id) {
        return admissionRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Admission", id));
    }

    @Transactional(value = "transactionManager", readOnly = true)
    public Page<Admission> search(Long patientId, AdmissionStatus status, Pageable pageable) {
        return admissionRepository.search(patientId, status, pageable);
    }
}
