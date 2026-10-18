package com.hms.billing.service;

import com.hms.appointment.entity.Appointment;
import com.hms.appointment.repository.AppointmentRepository;
import com.hms.billing.entity.Bill;
import com.hms.billing.entity.BillStatus;
import com.hms.billing.entity.BillType;
import com.hms.billing.repository.BillReceiptRepository;
import com.hms.billing.repository.BillRefundRepository;
import com.hms.billing.repository.BillRepository;
import com.hms.billing.repository.ChargeMasterRepository;
import com.hms.billing.repository.DiscountMasterRepository;
import com.hms.common.codeseq.CodeGeneratorService;
import com.hms.doctor.repository.DoctorRepository;
import com.hms.identity.entity.AppUser;
import com.hms.identity.entity.Role;
import com.hms.identity.repository.AppUserRepository;
import com.hms.patient.repository.PatientRepository;
import com.hms.security.jwt.AuthenticatedUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BillServiceFreeVisitTest {

    @Mock
    private BillRepository billRepository;
    @Mock
    private ChargeMasterRepository chargeMasterRepository;
    @Mock
    private DiscountMasterRepository discountMasterRepository;
    @Mock
    private BillReceiptRepository receiptRepository;
    @Mock
    private BillRefundRepository refundRepository;
    @Mock
    private PatientRepository patientRepository;
    @Mock
    private AppointmentRepository appointmentRepository;
    @Mock
    private DoctorRepository doctorRepository;
    @Mock
    private CodeGeneratorService codeGeneratorService;
    @Mock
    private BillingAuditService billingAuditService;
    @Mock
    private AppUserRepository appUserRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private BillService billService;

    private AuthenticatedUser actor;
    private Appointment appointment;

    @BeforeEach
    void setUp() {
        actor = new AuthenticatedUser(10L, "receptionist1", "Jane Receptionist", "RECEPTIONIST", "TENANT_1", "tenant_1", false);
        appointment = new Appointment();
        appointment.setId(100L);
        appointment.setPatientId(50L);
        appointment.setDoctorId(5L);
        appointment.setAppointmentCode("APT000100");
        appointment.setFreeVisit(true);
        appointment.setPaid(true);
        appointment.setFeeAmount(BigDecimal.ZERO);
        appointment.setCreatedBy(10L);
    }

    @Test
    void generateFreeVisitBill_createsFinalizedZeroAmountBillWithComments() {
        when(billRepository.existsByAppointmentIdAndStatusNot(100L, BillStatus.CANCELLED)).thenReturn(false);
        when(codeGeneratorService.next("BILL", "BL", 7)).thenReturn("BL0000001");
        when(billRepository.save(any(Bill.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(appointmentRepository.findById(100L)).thenReturn(Optional.of(appointment));

        Bill bill = billService.generateFreeVisitBill(appointment, actor);

        assertThat(bill).isNotNull();
        assertThat(bill.getBillNumber()).isEqualTo("BL0000001");
        assertThat(bill.getPatientId()).isEqualTo(50L);
        assertThat(bill.getAppointmentId()).isEqualTo(100L);
        assertThat(bill.getBillType()).isEqualTo(BillType.OPD);
        assertThat(bill.getStatus()).isEqualTo(BillStatus.FINALIZED);
        assertThat(bill.getComments()).isEqualTo("free visit");
        assertThat(bill.getTotalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(bill.getSubtotalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(bill.getDiscountAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(bill.getTaxAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(bill.getPaidAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(bill.getBalanceAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(bill.getFinalizedAt()).isNotNull();

        assertThat(bill.getItems()).hasSize(1);
        assertThat(bill.getItems().get(0).getDescription()).isEqualTo("Consultation (Free Visit)");
        assertThat(bill.getItems().get(0).getQuantity()).isEqualTo(1);
        assertThat(bill.getItems().get(0).getUnitPrice()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(bill.getItems().get(0).getAmount()).isEqualByComparingTo(BigDecimal.ZERO);

        verify(billRepository).save(any(Bill.class));
        verify(billingAuditService).record(eq("BILL_CREATED"), eq("BILL"), isNull(), isNull(), any(), any(), eq(actor), anyMap());
        verify(billingAuditService).record(eq("BILL_FINALIZED"), eq("BILL"), isNull(), isNull(), any(), any(), eq(actor), anyMap());
        verify(appointmentRepository).save(appointment);
        assertThat(appointment.isPaid()).isTrue();
    }

    @Test
    void generateFreeVisitBill_whenBillAlreadyExists_returnsExistingBillIdempotently() {
        Bill existingBill = new Bill();
        existingBill.setId(200L);
        existingBill.setComments("free visit");

        when(billRepository.existsByAppointmentIdAndStatusNot(100L, BillStatus.CANCELLED)).thenReturn(true);
        when(billRepository.findFirstByAppointmentIdAndStatusNot(100L, BillStatus.CANCELLED)).thenReturn(Optional.of(existingBill));

        Bill result = billService.generateFreeVisitBill(appointment, actor);

        assertThat(result).isSameAs(existingBill);
        verify(billRepository, never()).save(any(Bill.class));
        verify(billingAuditService, never()).record(anyString(), anyString(), any(), any(), any(), any(), any(), anyMap());
    }

    @Test
    void generateFreeVisitBill_whenActorNull_fallsBackToAppointmentCreatedBy() {
        AppUser createdByUser = new AppUser();
        createdByUser.setId(10L);
        createdByUser.setUsername("admin1");
        createdByUser.setFullName("Admin User");
        createdByUser.setRole(Role.HOSPITAL_ADMIN);

        when(billRepository.existsByAppointmentIdAndStatusNot(100L, BillStatus.CANCELLED)).thenReturn(false);
        when(codeGeneratorService.next("BILL", "BL", 7)).thenReturn("BL0000002");
        when(billRepository.save(any(Bill.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(appUserRepository.findById(10L)).thenReturn(Optional.of(createdByUser));
        when(appointmentRepository.findById(100L)).thenReturn(Optional.of(appointment));

        Bill bill = billService.generateFreeVisitBill(appointment, null);

        assertThat(bill).isNotNull();
        assertThat(bill.getComments()).isEqualTo("free visit");

        ArgumentCaptor<AuthenticatedUser> actorCaptor = ArgumentCaptor.forClass(AuthenticatedUser.class);
        verify(billingAuditService, atLeastOnce()).record(eq("BILL_CREATED"), eq("BILL"), isNull(), isNull(), any(), any(), actorCaptor.capture(), anyMap());
        assertThat(actorCaptor.getValue().userId()).isEqualTo(10L);
        assertThat(actorCaptor.getValue().username()).isEqualTo("admin1");
    }
}
