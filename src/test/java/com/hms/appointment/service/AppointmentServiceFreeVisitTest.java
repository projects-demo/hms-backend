package com.hms.appointment.service;

import com.hms.appointment.dto.BookAppointmentRequest;
import com.hms.appointment.entity.Appointment;
import com.hms.appointment.entity.AppointmentSlot;
import com.hms.appointment.entity.AppointmentStatus;
import com.hms.appointment.entity.SlotStatus;
import com.hms.appointment.repository.AppointmentPaymentRepository;
import com.hms.appointment.repository.AppointmentRepository;
import com.hms.appointment.repository.AppointmentSlotRepository;
import com.hms.billing.service.BillService;
import com.hms.common.codeseq.CodeGeneratorService;
import com.hms.doctor.entity.Doctor;
import com.hms.doctor.repository.DoctorAvailabilityRepository;
import com.hms.doctor.repository.DoctorRepository;
import com.hms.patient.entity.Patient;
import com.hms.patient.repository.PatientRepository;
import com.hms.security.jwt.AuthenticatedUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceFreeVisitTest {

    @Mock
    private AppointmentSlotRepository slotRepository;
    @Mock
    private AppointmentRepository appointmentRepository;
    @Mock
    private AppointmentPaymentRepository paymentRepository;
    @Mock
    private DoctorAvailabilityRepository availabilityRepository;
    @Mock
    private DoctorRepository doctorRepository;
    @Mock
    private PatientRepository patientRepository;
    @Mock
    private CodeGeneratorService codeGeneratorService;
    @Mock
    private BillService billService;

    @InjectMocks
    private AppointmentService appointmentService;

    private Doctor doctor;
    private Patient patient;
    private AuthenticatedUser actor;

    @BeforeEach
    void setUp() {
        actor = new AuthenticatedUser(1L, "receptionist", "Receptionist User", "RECEPTIONIST", "TENANT_1", "tenant_1", false);
        doctor = new Doctor();
        doctor.setId(10L);
        doctor.setConsultationFee(new BigDecimal("500.00"));

        patient = new Patient();
        patient.setId(20L);

        lenient().when(doctorRepository.findById(10L)).thenReturn(Optional.of(doctor));
        lenient().when(patientRepository.findById(20L)).thenReturn(Optional.of(patient));
    }

    @Test
    void book_whenFreeVisitTrue_generatesZeroAmountFeeAndTriggersBillGeneration() {
        BookAppointmentRequest request = new BookAppointmentRequest(
                20L, 10L, null, LocalDate.now(), com.hms.appointment.entity.AppointmentType.NEW, true, "First checkup"
        );
        when(codeGeneratorService.next("APPOINTMENT", "APT", 6)).thenReturn("APT000001");
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Appointment booked = appointmentService.book(request, actor);

        assertThat(booked).isNotNull();
        assertThat(booked.isFreeVisit()).isTrue();
        assertThat(booked.isPaid()).isTrue();
        assertThat(booked.getFeeAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(booked.getNotes()).isEqualTo("First checkup");

        verify(billService).generateFreeVisitBill(booked, actor);
    }

    @Test
    void book_whenFreeVisitFalse_doesNotTriggerFreeVisitBillGeneration() {
        BookAppointmentRequest request = new BookAppointmentRequest(
                20L, 10L, null, LocalDate.now(), com.hms.appointment.entity.AppointmentType.NEW, false, "Regular visit"
        );
        when(codeGeneratorService.next("APPOINTMENT", "APT", 6)).thenReturn("APT000002");
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Appointment booked = appointmentService.book(request, actor);

        assertThat(booked).isNotNull();
        assertThat(booked.isFreeVisit()).isFalse();
        assertThat(booked.isPaid()).isFalse();
        assertThat(booked.getFeeAmount()).isEqualByComparingTo(new BigDecimal("500.00"));

        verify(billService, never()).generateFreeVisitBill(any(), any());
    }

    @Test
    void changeStatus_whenFreeVisit_ensuresBillIsGenerated() {
        Appointment appt = new Appointment();
        appt.setId(99L);
        appt.setFreeVisit(true);
        appt.setPaid(true);
        appt.setStatus(AppointmentStatus.SCHEDULED);

        when(appointmentRepository.findById(99L)).thenReturn(Optional.of(appt));
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(i -> i.getArgument(0));

        Appointment updated = appointmentService.changeStatus(99L, AppointmentStatus.CHECKED_IN);

        assertThat(updated.getStatus()).isEqualTo(AppointmentStatus.CHECKED_IN);
        verify(billService).generateFreeVisitBill(appt, null);
    }
}
