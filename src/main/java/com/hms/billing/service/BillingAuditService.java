package com.hms.billing.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hms.billing.entity.Bill;
import com.hms.billing.entity.BillingAuditLog;
import com.hms.billing.repository.BillingAuditLogRepository;
import com.hms.billing.repository.BillRepository;
import com.hms.patient.repository.PatientRepository;
import com.hms.security.jwt.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional("transactionManager")
public class BillingAuditService {

    private final BillingAuditLogRepository auditLogRepository;
    private final BillRepository billRepository;
    private final PatientRepository patientRepository;
    private final ObjectMapper objectMapper;

    public void record(String action, String entityType, Long entityId, Long billId,
                       String patientName, String mobileNumber,
                       AuthenticatedUser actor, Map<String, ?> details) {
        if (actor == null) {
            return;
        }
        BillingAuditLog log = new BillingAuditLog();
        log.setAction(action);
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setBillId(billId);

        String resolvedName = patientName;
        String resolvedPhone = mobileNumber;

        if ((resolvedName == null || resolvedPhone == null) && billId != null) {
            var billOpt = billRepository.findById(billId);
            if (billOpt.isPresent()) {
                var patientOpt = patientRepository.findById(billOpt.get().getPatientId());
                if (patientOpt.isPresent()) {
                    var p = patientOpt.get();
                    if (resolvedName == null) resolvedName = p.getFirstName() + " " + p.getLastName();
                    if (resolvedPhone == null) resolvedPhone = p.getPrimaryPhone();
                }
            }
        }

        log.setPatientName(resolvedName);
        log.setMobileNumber(resolvedPhone);
        log.setActorUserId(actor.userId());
        log.setActorUsername(actor.username());
        log.setActorName(actor.fullName());
        log.setActorRole(actor.role());
        try {
            log.setDetails(objectMapper.writeValueAsString(details));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not serialize billing audit details", e);
        }
        auditLogRepository.save(log);
    }

    public void record(String action, String entityType, Long entityId, Long billId,
                       AuthenticatedUser actor, Map<String, ?> details) {
        record(action, entityType, entityId, billId, null, null, actor, details);
    }

    @Transactional(value = "transactionManager", readOnly = true)
    public Page<BillingAuditLog> search(Long billId, Pageable pageable) {
        return search(billId, null, pageable);
    }

    @Transactional(value = "transactionManager", readOnly = true)
    public Page<BillingAuditLog> search(Long billId, String q, Pageable pageable) {
        String normalizedQuery = q == null || q.isBlank() ? null : q.trim();
        Page<BillingAuditLog> page = auditLogRepository.search(billId, normalizedQuery, pageable);
        enrichPatientDetails(page.getContent());
        return page;
    }

    private void enrichPatientDetails(List<BillingAuditLog> logs) {
        List<BillingAuditLog> missing = logs.stream()
                .filter(l -> l.getBillId() != null && (l.getPatientName() == null || l.getMobileNumber() == null))
                .toList();
        if (missing.isEmpty()) return;

        Set<Long> billIds = missing.stream().map(BillingAuditLog::getBillId).collect(Collectors.toSet());
        Map<Long, Long> patientIdByBill = billRepository.findAllById(billIds).stream()
                .collect(Collectors.toMap(Bill::getId, Bill::getPatientId));
        Set<Long> patientIds = Set.copyOf(patientIdByBill.values());
        Map<Long, PatientRepository.PatientBillingRow> patientRows = patientRepository.findBillingDetailsByIds(patientIds).stream()
                .collect(Collectors.toMap(PatientRepository.PatientBillingRow::getId, r -> r));

        for (BillingAuditLog log : missing) {
            Long patientId = patientIdByBill.get(log.getBillId());
            if (patientId != null) {
                PatientRepository.PatientBillingRow row = patientRows.get(patientId);
                if (row != null) {
                    if (log.getPatientName() == null) log.setPatientName(row.getFullName());
                    if (log.getMobileNumber() == null) log.setMobileNumber(row.getPrimaryPhone());
                }
            }
        }
    }
}