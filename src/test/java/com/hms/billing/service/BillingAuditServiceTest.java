package com.hms.billing.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hms.billing.entity.Bill;
import com.hms.billing.entity.BillingAuditLog;
import com.hms.billing.repository.BillingAuditLogRepository;
import com.hms.billing.repository.BillRepository;
import com.hms.patient.entity.Patient;
import com.hms.patient.repository.PatientRepository;
import com.hms.security.jwt.AuthenticatedUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BillingAuditServiceTest {

    @Mock
    private BillingAuditLogRepository auditLogRepository;
    @Mock
    private BillRepository billRepository;
    @Mock
    private PatientRepository patientRepository;

    private ObjectMapper objectMapper = new ObjectMapper();

    private BillingAuditService billingAuditService;

    private AuthenticatedUser actor;

    @BeforeEach
    void setUp() {
        billingAuditService = new BillingAuditService(auditLogRepository, billRepository, patientRepository, objectMapper);
        actor = new AuthenticatedUser(1L, "admin", "Admin User", "HOSPITAL_ADMIN", "TENANT_1", "tenant_1", false);
    }

    @Test
    void record_withExplicitPatientDetails_setsPatientNameAndMobileNumber() {
        billingAuditService.record(
                "BILL_CREATED", "BILL", 100L, 100L,
                "John Doe", "9876543210", actor, Map.of("amount", 500)
        );

        ArgumentCaptor<BillingAuditLog> captor = ArgumentCaptor.forClass(BillingAuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        BillingAuditLog saved = captor.getValue();
        assertThat(saved.getPatientName()).isEqualTo("John Doe");
        assertThat(saved.getMobileNumber()).isEqualTo("9876543210");
        assertThat(saved.getPatientPhone()).isEqualTo("9876543210");
        assertThat(saved.getAction()).isEqualTo("BILL_CREATED");
        assertThat(saved.getActorName()).isEqualTo("Admin User");
    }

    @Test
    void record_withoutExplicitPatientDetails_resolvesFromBillId() {
        Bill bill = new Bill();
        bill.setId(100L);
        bill.setPatientId(50L);

        Patient patient = new Patient();
        patient.setId(50L);
        patient.setFirstName("Alice");
        patient.setLastName("Smith");
        patient.setPrimaryPhone("9123456789");

        when(billRepository.findById(100L)).thenReturn(Optional.of(bill));
        when(patientRepository.findById(50L)).thenReturn(Optional.of(patient));

        billingAuditService.record("BILL_FINALIZED", "BILL", 100L, 100L, actor, Map.of("status", "FINALIZED"));

        ArgumentCaptor<BillingAuditLog> captor = ArgumentCaptor.forClass(BillingAuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        BillingAuditLog saved = captor.getValue();
        assertThat(saved.getPatientName()).isEqualTo("Alice Smith");
        assertThat(saved.getMobileNumber()).isEqualTo("9123456789");
    }

    @Test
    void search_enrichesMissingPatientDetailsFromBillsAndPatients() {
        BillingAuditLog logWithoutPatient = new BillingAuditLog();
        logWithoutPatient.setId(1L);
        logWithoutPatient.setBillId(22L);
        logWithoutPatient.setAction("BILL_FINALIZED");

        Page<BillingAuditLog> page = new PageImpl<>(List.of(logWithoutPatient));
        when(auditLogRepository.search(eq(22L), isNull(), any())).thenReturn(page);

        Bill bill = new Bill();
        bill.setId(22L);
        bill.setPatientId(4L);
        when(billRepository.findAllById(Set.of(22L))).thenReturn(List.of(bill));

        PatientRepository.PatientBillingRow billingRow = mock(PatientRepository.PatientBillingRow.class);
        when(billingRow.getId()).thenReturn(4L);
        when(billingRow.getFullName()).thenReturn("Patient4 Test");
        when(billingRow.getPrimaryPhone()).thenReturn("9988776655");
        when(patientRepository.findBillingDetailsByIds(Set.of(4L))).thenReturn(List.of(billingRow));

        Page<BillingAuditLog> result = billingAuditService.search(22L, PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        BillingAuditLog enriched = result.getContent().get(0);
        assertThat(enriched.getPatientName()).isEqualTo("Patient4 Test");
        assertThat(enriched.getMobileNumber()).isEqualTo("9988776655");
        assertThat(enriched.getPatientPhone()).isEqualTo("9988776655");
    }
}

