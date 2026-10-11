package com.hms.billing.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hms.billing.entity.BillingAuditLog;
import com.hms.billing.repository.BillingAuditLogRepository;
import com.hms.security.jwt.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional("transactionManager")
public class BillingAuditService {

    private final BillingAuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    public void record(String action, String entityType, Long entityId, Long billId,
                       AuthenticatedUser actor, Map<String, ?> details) {
        BillingAuditLog log = new BillingAuditLog();
        log.setAction(action);
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setBillId(billId);
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

    @Transactional(value = "transactionManager", readOnly = true)
    public Page<BillingAuditLog> search(Long billId, Pageable pageable) {
        return billId == null ? auditLogRepository.findAll(pageable) : auditLogRepository.findByBillId(billId, pageable);
    }
}