package com.hms.billing.web;

import com.hms.billing.entity.ChargeType;
import com.hms.billing.repository.ChargeTypeRepository;
import com.hms.billing.service.BillingAuditService;
import com.hms.common.dto.ApiResponse;
import com.hms.common.exception.DuplicateResourceException;
import com.hms.security.jwt.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/charge-types")
@RequiredArgsConstructor
@Tag(name = "Billing - Charge Types", description = "Top-level categories (CONSULTATION, PHARMACY, LAB, BED_CHARGE, ...) that charge-master items belong to")
public class ChargeTypeController {

    private final ChargeTypeRepository chargeTypeRepository;
    private final BillingAuditService billingAuditService;

    public record ChargeTypeRequest(@NotBlank String name, String code, Boolean active) {}
    public record ChargeTypeResponse(Long id, String name, String code, boolean active) {
        static ChargeTypeResponse from(ChargeType c) { return new ChargeTypeResponse(c.getId(), c.getName(), c.getCode(), c.isActive()); }
    }

    @PostMapping
    @PreAuthorize("hasRole('HOSPITAL_ADMIN')")
    @Operation(summary = "Create a charge type")
    @Transactional("transactionManager")
    public ApiResponse<ChargeTypeResponse> create(@Valid @RequestBody ChargeTypeRequest request,
                                                    @AuthenticationPrincipal AuthenticatedUser actor) {
        ChargeType ct = new ChargeType();
        ct.setName(request.name());
        ct.setCode(resolveCode(request.code(), request.name(), null));
        ct.setActive(request.active() == null || request.active());
        ct = chargeTypeRepository.save(ct);
        billingAuditService.record("BILLING_CATEGORY_ADDED", "CHARGE_TYPE", ct.getId(), null, actor,
            java.util.Map.of("name", ct.getName(), "code", ct.getCode(), "active", ct.isActive()));
        return ApiResponse.ok(ChargeTypeResponse.from(ct));
    }

        @PutMapping("/{id}")
        @PreAuthorize("hasRole('HOSPITAL_ADMIN')")
        @Transactional("transactionManager")
        public ApiResponse<ChargeTypeResponse> update(@PathVariable Long id, @Valid @RequestBody ChargeTypeRequest request,
                                @AuthenticationPrincipal AuthenticatedUser actor) {
        ChargeType ct = chargeTypeRepository.findById(id)
            .orElseThrow(() -> com.hms.common.exception.ResourceNotFoundException.of("ChargeType", id));
        var before = java.util.Map.of("name", ct.getName(), "code", ct.getCode(), "active", ct.isActive());
        ct.setName(request.name());
        ct.setCode(resolveCode(request.code(), request.name(), id));
        if (request.active() != null) ct.setActive(request.active());
        ct = chargeTypeRepository.save(ct);
        billingAuditService.record("BILLING_CATEGORY_EDITED", "CHARGE_TYPE", ct.getId(), null, actor,
            java.util.Map.of("before", before, "after", java.util.Map.of("name", ct.getName(), "code", ct.getCode(), "active", ct.isActive())));
        return ApiResponse.ok(ChargeTypeResponse.from(ct));
        }

    @GetMapping
    @Operation(summary = "List all charge types", description = "Small, static reference list - not paginated.")
    public ApiResponse<List<ChargeTypeResponse>> list() {
        return ApiResponse.ok(chargeTypeRepository.findAll().stream().map(ChargeTypeResponse::from).toList());
    }

    private String resolveCode(String requestedCode, String name, Long currentId) {
        boolean autoGenerate = requestedCode == null || requestedCode.isBlank();
        String base = autoGenerate
            ? name.trim().toUpperCase().replaceAll("[^A-Z0-9]+", "_").replaceAll("^_+|_+$", "")
            : requestedCode.trim().toUpperCase();
        if (base.isBlank()) base = "TYPE";
        base = base.substring(0, Math.min(base.length(), 50));
        String candidate = base;
        int suffix = 2;
        while (true) {
            var existing = chargeTypeRepository.findByCode(candidate);
            if (existing.isEmpty() || existing.get().getId().equals(currentId)) return candidate;
            if (!autoGenerate) throw new DuplicateResourceException("Charge type code '" + candidate + "' already exists");
            String tail = "_" + suffix++;
            candidate = base.substring(0, Math.min(base.length(), 50 - tail.length())) + tail;
        }
    }
}
