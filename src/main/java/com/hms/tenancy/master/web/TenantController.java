package com.hms.tenancy.master.web;

import com.hms.common.PageRequestFactory;
import com.hms.common.dto.ApiResponse;
import com.hms.common.dto.PageResponse;
import com.hms.common.exception.ResourceNotFoundException;
import com.hms.tenancy.master.dto.OnboardTenantRequest;
import com.hms.tenancy.master.dto.TenantResponse;
import com.hms.tenancy.master.entity.Tenant;
import com.hms.tenancy.master.repository.TenantRepository;
import com.hms.tenancy.master.service.TenantProvisioningService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/platform/tenants")
@RequiredArgsConstructor
@Tag(name = "Tenant Management", description = "Onboard and manage hospitals (platform-admin only)")
public class TenantController {

    private final TenantProvisioningService provisioningService;
    private final TenantRepository tenantRepository;
    private final PageRequestFactory pageRequestFactory;

    @PostMapping
    @Operation(summary = "Onboard a new hospital",
            description = "Creates a dedicated MySQL schema for the hospital, runs migrations, and seeds its first admin login.")
    public ApiResponse<TenantResponse> onboard(@Valid @RequestBody OnboardTenantRequest request) {
        Tenant tenant = provisioningService.onboard(request);
        return ApiResponse.ok("Hospital onboarded", TenantResponse.from(tenant));
    }

    @GetMapping
    @Operation(summary = "List hospitals (paginated)")
    public ApiResponse<PageResponse<TenantResponse>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        var pageable = pageRequestFactory.of(page, size, "createdAt", "desc");
        Page<Tenant> result = (status != null)
                ? tenantRepository.findByStatus(Tenant.Status.valueOf(status), pageable)
                : tenantRepository.findAll(pageable);
        return ApiResponse.ok(PageResponse.of(result.map(TenantResponse::from)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get hospital by id")
    public ApiResponse<TenantResponse> get(@PathVariable Long id) {
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Tenant", id));
        return ApiResponse.ok(TenantResponse.from(tenant));
    }

    @PatchMapping("/{id}/suspend")
    @CacheEvict(cacheNames = "tenantBySchemaCode", allEntries = true)
    @Operation(summary = "Suspend a hospital (blocks all its users from logging in)")
    public ApiResponse<TenantResponse> suspend(@PathVariable Long id) {
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Tenant", id));
        tenant.setStatus(Tenant.Status.SUSPENDED);
        return ApiResponse.ok(TenantResponse.from(tenantRepository.save(tenant)));
    }

    @PatchMapping("/{id}/reactivate")
    @CacheEvict(cacheNames = "tenantBySchemaCode", allEntries = true)
    @Operation(summary = "Reactivate a suspended or deactivated hospital")
    public ApiResponse<TenantResponse> reactivate(@PathVariable Long id) {
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Tenant", id));
        tenant.setStatus(Tenant.Status.ACTIVE);
        return ApiResponse.ok(TenantResponse.from(tenantRepository.save(tenant)));
    }

    @PatchMapping("/{id}/deactivate")
    @CacheEvict(cacheNames = "tenantBySchemaCode", allEntries = true)
    @Operation(summary = "Offboard a hospital", description = "Blocks all logins for this hospital indefinitely. " +
            "The schema and all its data are preserved (never hard-deleted) - a deactivated hospital can still be reactivated later.")
    public ApiResponse<TenantResponse> deactivate(@PathVariable Long id) {
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Tenant", id));
        tenant.setStatus(Tenant.Status.DEACTIVATED);
        return ApiResponse.ok(TenantResponse.from(tenantRepository.save(tenant)));
    }
}