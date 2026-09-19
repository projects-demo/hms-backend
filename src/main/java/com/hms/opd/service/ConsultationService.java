package com.hms.opd.service;

import com.hms.appointment.entity.AppointmentStatus;
import com.hms.appointment.service.AppointmentService;
import com.hms.common.exception.BusinessRuleException;
import com.hms.common.exception.DuplicateResourceException;
import com.hms.common.exception.ResourceNotFoundException;
import com.hms.opd.dto.CompleteConsultationRequest;
import com.hms.opd.dto.StartConsultationRequest;
import com.hms.opd.entity.Consultation;
import com.hms.opd.entity.ConsultationStatus;
import com.hms.opd.entity.Prescription;
import com.hms.opd.entity.PrescriptionItem;
import com.hms.opd.repository.ConsultationRepository;
import com.hms.opd.repository.PrescriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional("transactionManager")
public class ConsultationService {

    private final ConsultationRepository consultationRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final AppointmentService appointmentService;

    public Consultation start(StartConsultationRequest r) {
        if (consultationRepository.findByAppointmentId(r.appointmentId()).isPresent()) {
            throw new DuplicateResourceException("A consultation already exists for this appointment");
        }
        Consultation c = new Consultation();
        c.setAppointmentId(r.appointmentId());
        c.setPatientId(r.patientId());
        c.setDoctorId(r.doctorId());
        c.setChiefComplaint(r.chiefComplaint());
        c.setStatus(ConsultationStatus.DRAFT);
        c = consultationRepository.save(c);

        // Doctor is now actively seeing the patient - keep the appointment board in sync.
        appointmentService.changeStatus(r.appointmentId(), AppointmentStatus.IN_CONSULTATION);
        return c;
    }

    public Consultation complete(Long consultationId, CompleteConsultationRequest r) {
        Consultation c = getOrThrow(consultationId);
        if (c.getStatus() == ConsultationStatus.COMPLETED) {
            throw new BusinessRuleException("Consultation is already completed");
        }
        if (r.vitals() != null) {
            c.setBp(r.vitals().bp());
            c.setPulse(r.vitals().pulse());
            c.setTemperature(r.vitals().temperature());
            c.setWeightKg(r.vitals().weightKg());
            c.setHeightCm(r.vitals().heightCm());
            c.setSpo2(r.vitals().spo2());
        }
        c.setDiagnosis(r.diagnosis());
        c.setClinicalNotes(r.clinicalNotes());
        c.setStatus(ConsultationStatus.COMPLETED);
        c.setCompletedAt(LocalDateTime.now());
        c = consultationRepository.save(c);

        if (r.prescriptionItems() != null && !r.prescriptionItems().isEmpty()) {
            Prescription prescription = prescriptionRepository.findByConsultationId(consultationId)
                    .orElseGet(() -> {
                        Prescription p = new Prescription();
                        p.setConsultationId(consultationId);
                        return p;
                    });
            for (var itemReq : r.prescriptionItems()) {
                PrescriptionItem item = new PrescriptionItem();
                item.setDrugName(itemReq.drugName());
                item.setDosage(itemReq.dosage());
                item.setFrequency(itemReq.frequency());
                item.setDuration(itemReq.duration());
                item.setInstructions(itemReq.instructions());
                prescription.addItem(item);
            }
            prescriptionRepository.save(prescription);
        }

        appointmentService.changeStatus(c.getAppointmentId(), AppointmentStatus.COMPLETED);
        return c;
    }

    @Transactional(value = "transactionManager", readOnly = true)
    public Consultation getOrThrow(Long id) {
        return consultationRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Consultation", id));
    }

    @Transactional(value = "transactionManager", readOnly = true)
    public Page<Consultation> byPatient(Long patientId, Pageable pageable) {
        return consultationRepository.findByPatientId(patientId, pageable);
    }

    @Transactional(value = "transactionManager", readOnly = true)
    public Page<Consultation> byDoctor(Long doctorId, Pageable pageable) {
        return consultationRepository.findByDoctorId(doctorId, pageable);
    }
}
