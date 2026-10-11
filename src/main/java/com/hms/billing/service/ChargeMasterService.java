package com.hms.billing.service;

import com.hms.billing.dto.ChargeMasterRequest;
import com.hms.billing.entity.ChargeMaster;
import com.hms.billing.entity.ChargeType;
import com.hms.billing.repository.ChargeMasterRepository;
import com.hms.billing.repository.ChargeTypeRepository;
import com.hms.common.dto.OptionDto;
import com.hms.common.exception.ResourceNotFoundException;
import com.hms.common.exception.DuplicateResourceException;
import com.hms.security.jwt.AuthenticatedUser;
import com.hms.doctor.entity.Doctor;
import com.hms.doctor.repository.DoctorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Charge master is reference data staff look up dozens of times a day when
 * building bills - cached aggressively (see hms.pagination + application.yml
 * caffeine spec) and evicted on any write.
 */
@Service
@RequiredArgsConstructor
@Transactional("transactionManager")
public class ChargeMasterService {

    private final ChargeMasterRepository chargeMasterRepository;
    private final ChargeTypeRepository chargeTypeRepository;
    private final DoctorRepository doctorRepository;
    private final BillingAuditService billingAuditService;

    @CacheEvict(cacheNames = "chargeMasterAutocomplete", allEntries = true)
    public ChargeMaster create(ChargeMasterRequest r, AuthenticatedUser actor) {
        String code = resolveCode(r.code(), r.name(), null);
        ChargeType type = chargeTypeRepository.findById(r.chargeTypeId())
                .orElseThrow(() -> ResourceNotFoundException.of("ChargeType", r.chargeTypeId()));
        Doctor doctor = resolveDoctor(r.applicableDoctorId());
        ChargeMaster cm = new ChargeMaster();
        cm.setChargeType(type);
        cm.setCode(code);
        cm.setName(r.name());
        cm.setDefaultPrice(r.defaultPrice());
        cm.setApplicableDoctorId(doctor == null ? null : doctor.getId());
        cm = chargeMasterRepository.save(cm);
        billingAuditService.record("STANDARD_ITEM_ADDED", "CHARGE_MASTER", cm.getId(), null, actor,
                java.util.Map.of("code", cm.getCode(), "name", cm.getName(), "category", type.getName(),
                        "defaultPrice", cm.getDefaultPrice(), "applicableDoctorId", cm.getApplicableDoctorId() == null ? "all" : cm.getApplicableDoctorId()));
        return cm;
    }

    @CacheEvict(cacheNames = "chargeMasterAutocomplete", allEntries = true)
    public ChargeMaster update(Long id, ChargeMasterRequest r, AuthenticatedUser actor) {
        ChargeMaster cm = chargeMasterRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("ChargeMaster", id));
        java.util.Map<String, Object> before = java.util.Map.of("code", cm.getCode(), "name", cm.getName(),
                "category", cm.getChargeType().getName(), "defaultPrice", cm.getDefaultPrice(),
                "applicableDoctorId", cm.getApplicableDoctorId() == null ? "all" : cm.getApplicableDoctorId());
        String code = resolveCode(r.code(), r.name(), id);
        ChargeType type = chargeTypeRepository.findById(r.chargeTypeId())
                .orElseThrow(() -> ResourceNotFoundException.of("ChargeType", r.chargeTypeId()));
        Doctor doctor = resolveDoctor(r.applicableDoctorId());
        cm.setChargeType(type);
        cm.setCode(code);
        cm.setName(r.name());
        cm.setDefaultPrice(r.defaultPrice());
        cm.setApplicableDoctorId(doctor == null ? null : doctor.getId());
        cm = chargeMasterRepository.save(cm);
        billingAuditService.record("STANDARD_ITEM_EDITED", "CHARGE_MASTER", cm.getId(), null, actor,
                java.util.Map.of("before", before, "after", java.util.Map.of("code", cm.getCode(), "name", cm.getName(),
                        "category", type.getName(), "defaultPrice", cm.getDefaultPrice(),
                        "applicableDoctorId", cm.getApplicableDoctorId() == null ? "all" : cm.getApplicableDoctorId())));
        return cm;
    }

    public ChargeMaster setActive(Long id, boolean active, AuthenticatedUser actor) {
        ChargeMaster cm = chargeMasterRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("ChargeMaster", id));
        boolean previous = cm.isActive();
        cm.setActive(active);
        cm = chargeMasterRepository.save(cm);
        billingAuditService.record(active ? "STANDARD_ITEM_ACTIVATED" : "STANDARD_ITEM_DEACTIVATED",
                "CHARGE_MASTER", cm.getId(), null, actor,
                java.util.Map.of("code", cm.getCode(), "name", cm.getName(), "previousActive", previous, "active", active));
        return cm;
    }

    @Transactional(value = "transactionManager", readOnly = true)
    public Page<ChargeMaster> search(String q, Long chargeTypeId, Boolean active, boolean includeInactive, Pageable pageable) {
        return chargeMasterRepository.search(q, chargeTypeId, active, includeInactive, pageable);
    }

    @Transactional(value = "transactionManager", readOnly = true)
    public ChargeMasterSummary summary() {
        return new ChargeMasterSummary(chargeMasterRepository.count(), chargeMasterRepository.countByActiveTrue(),
                chargeMasterRepository.countByActiveFalse(), chargeTypeRepository.countByActiveTrue());
    }

    @Cacheable(cacheNames = "chargeMasterAutocomplete", key = "#q")
    @Transactional(value = "transactionManager", readOnly = true)
    public List<OptionDto> autocomplete(String q, int limit) {
        return chargeMasterRepository.autocomplete(q, PageRequest.of(0, Math.min(limit, 20)));
    }

    private Doctor resolveDoctor(Long doctorId) {
        return doctorId == null ? null : doctorRepository.findById(doctorId)
                .orElseThrow(() -> ResourceNotFoundException.of("Doctor", doctorId));
    }

    private String resolveCode(String requestedCode, String name, Long currentId) {
        boolean autoGenerate = requestedCode == null || requestedCode.isBlank();
        String base = autoGenerate
            ? name.trim().toUpperCase().replaceAll("[^A-Z0-9]+", "_").replaceAll("^_+|_+$", "")
            : requestedCode.trim().toUpperCase();
        if (base.isBlank()) base = "CHARGE";
        base = base.substring(0, Math.min(base.length(), 30));
        String candidate = base;
        int suffix = 2;
        while (true) {
            var existing = chargeMasterRepository.findByCode(candidate);
            if (existing.isEmpty() || existing.get().getId().equals(currentId)) return candidate;
            if (!autoGenerate) throw new DuplicateResourceException("Charge code '" + candidate + "' already exists");
            String tail = "_" + suffix++;
            candidate = base.substring(0, Math.min(base.length(), 30 - tail.length())) + tail;
        }
    }

    public record ChargeMasterSummary(long totalCharges, long activeCharges, long inactiveCharges, long chargeTypes) {}
}
