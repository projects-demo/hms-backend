package com.hms.appointment.service;

import com.hms.appointment.dto.BookAppointmentRequest;
import com.hms.appointment.dto.AppointmentResponse;
import com.hms.appointment.entity.*;
import com.hms.appointment.repository.AppointmentPaymentRepository;
import com.hms.appointment.repository.AppointmentRepository;
import com.hms.appointment.repository.AppointmentSlotRepository;
import com.hms.common.codeseq.CodeGeneratorService;
import com.hms.common.exception.BusinessRuleException;
import com.hms.common.exception.ResourceNotFoundException;
import com.hms.doctor.entity.Doctor;
import com.hms.doctor.entity.DoctorAvailability;
import com.hms.doctor.repository.DoctorAvailabilityRepository;
import com.hms.doctor.repository.DoctorRepository;
import com.hms.patient.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional("transactionManager")
public class AppointmentService {

    private final AppointmentSlotRepository slotRepository;
    private final AppointmentRepository appointmentRepository;
    private final AppointmentPaymentRepository paymentRepository;
    private final DoctorAvailabilityRepository availabilityRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;
    private final CodeGeneratorService codeGeneratorService;

    /** Materializes bookable slots for one doctor/date from their weekly availability template. Idempotent. */
    public List<AppointmentSlot> generateSlotsForDate(Long doctorId, LocalDate date) {
        // JPA's DayOfWeek.MONDAY=1 aligns with our 1=Mon..7=Sun convention already.
        short dow = (short) date.getDayOfWeek().getValue();
        List<DoctorAvailability> templates = availabilityRepository.findByDoctorIdAndActiveTrue(doctorId).stream()
                .filter(t -> t.getDayOfWeek() == dow)
                .toList();
        if (templates.isEmpty()) {
            throw new BusinessRuleException("Doctor has no availability template for this day of week");
        }

        List<AppointmentSlot> created = new java.util.ArrayList<>();
        for (DoctorAvailability t : templates) {
            LocalTime cursor = t.getStartTime();
            while (cursor.plusMinutes(t.getSlotDurationMins()).compareTo(t.getEndTime()) <= 0) {
                LocalTime slotStart = cursor;
                LocalTime slotEnd = cursor.plusMinutes(t.getSlotDurationMins());
                boolean exists = slotRepository.findByDoctorIdAndSlotDateAndStartTime(doctorId, date, slotStart).isPresent();
                if (!exists) {
                    AppointmentSlot slot = new AppointmentSlot();
                    slot.setDoctorId(doctorId);
                    slot.setSlotDate(date);
                    slot.setStartTime(slotStart);
                    slot.setEndTime(slotEnd);
                    slot.setStatus(SlotStatus.AVAILABLE);
                    created.add(slotRepository.save(slot));
                }
                cursor = slotEnd;
            }
        }
        return created;
    }

    @Transactional(value = "transactionManager", readOnly = true)
    public List<AppointmentSlot> availableSlots(Long doctorId, LocalDate date) {
        return slotRepository.findAvailable(doctorId, date, SlotStatus.AVAILABLE);
    }

    public Appointment book(BookAppointmentRequest r, Long actorUserId) {
        Doctor doctor = doctorRepository.findById(r.doctorId())
                .orElseThrow(() -> ResourceNotFoundException.of("Doctor", r.doctorId()));
        patientRepository.findById(r.patientId())
                .orElseThrow(() -> ResourceNotFoundException.of("Patient", r.patientId()));

        if (r.slotId() != null) {
            AppointmentSlot slot = slotRepository.findById(r.slotId())
                    .orElseThrow(() -> ResourceNotFoundException.of("Slot", r.slotId()));
            if (!slot.getDoctorId().equals(r.doctorId()) || !slot.getSlotDate().equals(r.appointmentDate())) {
                throw new BusinessRuleException("The selected slot does not match the doctor and appointment date");
            }
            if (slot.getStatus() != SlotStatus.AVAILABLE) {
                // @Version on AppointmentSlot also protects against the race where two
                // receptionists book the same slot in the same instant.
                throw new BusinessRuleException("This slot is no longer available - please pick another");
            }
            slot.setStatus(SlotStatus.BOOKED);
            slotRepository.save(slot);
        }

        Appointment appt = new Appointment();
        appt.setAppointmentCode(codeGeneratorService.next("APPOINTMENT", "APT", 6));
        appt.setPatientId(r.patientId());
        appt.setDoctorId(r.doctorId());
        appt.setSlotId(r.slotId());
        appt.setAppointmentDate(r.appointmentDate());
        appt.setAppointmentType(r.appointmentType());
        appt.setStatus(AppointmentStatus.SCHEDULED);
        appt.setFreeVisit(r.freeVisit());
        appt.setPaid(r.freeVisit());
        appt.setFeeAmount(r.freeVisit() ? java.math.BigDecimal.ZERO : doctor.getConsultationFee());
        appt.setNotes(r.notes());
        appt.setCreatedBy(actorUserId);
        return appointmentRepository.save(appt);
    }

    public Appointment changeStatus(Long id, AppointmentStatus newStatus) {
        Appointment appt = getOrThrow(id);
        if (newStatus == AppointmentStatus.CHECKED_IN && !appt.isPaid() && !appt.isFreeVisit()) {
            throw new BusinessRuleException("Appointment cannot be confirmed until its bill is paid");
        }
        validateTransition(appt.getStatus(), newStatus);
        appt.setStatus(newStatus);
        if (newStatus == AppointmentStatus.CANCELLED && appt.getSlotId() != null) {
            slotRepository.findById(appt.getSlotId()).ifPresent(slot -> {
                slot.setStatus(SlotStatus.AVAILABLE);
                slotRepository.save(slot);
            });
        }
        return appointmentRepository.save(appt);
    }

    public AppointmentPayment recordPayment(Long appointmentId, java.math.BigDecimal amount, PaymentMode mode, Long actorUserId) {
        Appointment appt = getOrThrow(appointmentId);
        java.math.BigDecimal paidSoFar = paymentRepository.findByAppointmentId(appointmentId).stream()
                .map(AppointmentPayment::getAmount)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        java.math.BigDecimal outstanding = appt.getFeeAmount().subtract(paidSoFar);
        if (amount.compareTo(outstanding) > 0) {
            throw new BusinessRuleException("Payment amount exceeds the outstanding appointment fee of " + outstanding);
        }
        AppointmentPayment payment = new AppointmentPayment();
        payment.setAppointmentId(appointmentId);
        payment.setAmount(amount);
        payment.setPaymentMode(mode);
        payment.setReceivedBy(actorUserId);
        payment = paymentRepository.save(payment);

        appt.setPaid(paidSoFar.add(amount).compareTo(appt.getFeeAmount()) >= 0);
        appointmentRepository.save(appt);
        return payment;
    }

    @Transactional(value = "transactionManager", readOnly = true)
    public Appointment getOrThrow(Long id) {
        return appointmentRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Appointment", id));
    }

    @Transactional(value = "transactionManager", readOnly = true)
    public Page<Appointment> search(Long doctorId, Long patientId, LocalDate date, AppointmentStatus status, Pageable pageable) {
        return appointmentRepository.search(doctorId, patientId, date, status, pageable);
    }

    @Transactional(value = "transactionManager", readOnly = true)
    public Page<AppointmentResponse> searchResponses(Long doctorId, Long patientId, LocalDate date,
                                                       AppointmentStatus status, Pageable pageable) {
        Page<Appointment> appointments = search(doctorId, patientId, date, status, pageable);
        if (appointments.isEmpty()) return appointments.map(AppointmentResponse::from);

        Set<Long> doctorIds = appointments.stream().map(Appointment::getDoctorId).collect(Collectors.toSet());
        Map<Long, String> doctorNames = doctorRepository.findDisplayNamesByIds(doctorIds).stream()
                .collect(Collectors.toMap(DoctorRepository.DoctorDisplayRow::getId,
                        DoctorRepository.DoctorDisplayRow::getFullName));
        List<Long> appointmentIds = appointments.stream().map(Appointment::getId).toList();
        Map<Long, Long> queueNumbers = appointmentRepository.findQueueNumbers(appointmentIds).stream()
                .filter(row -> row.getQueueNumber() != null)
                .collect(Collectors.toMap(AppointmentRepository.QueueNumberRow::getAppointmentId,
                        AppointmentRepository.QueueNumberRow::getQueueNumber));

        return appointments.map(a -> AppointmentResponse.from(a, doctorNames.get(a.getDoctorId()), queueNumbers.get(a.getId())));
    }

    private void validateTransition(AppointmentStatus from, AppointmentStatus to) {
        boolean valid = switch (from) {
            case SCHEDULED -> to == AppointmentStatus.CHECKED_IN || to == AppointmentStatus.CANCELLED || to == AppointmentStatus.NO_SHOW;
            case CHECKED_IN -> to == AppointmentStatus.IN_CONSULTATION || to == AppointmentStatus.CANCELLED;
            case IN_CONSULTATION -> to == AppointmentStatus.COMPLETED;
            default -> false;
        };
        if (!valid) {
            throw new BusinessRuleException("Cannot move appointment from " + from + " to " + to);
        }
    }
}
