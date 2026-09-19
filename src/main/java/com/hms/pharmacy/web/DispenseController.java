package com.hms.pharmacy.web;

import com.hms.common.PageRequestFactory;
import com.hms.common.dto.ApiResponse;
import com.hms.common.dto.PageResponse;
import com.hms.pharmacy.dto.DispenseRequest;
import com.hms.pharmacy.dto.DispenseResponse;
import com.hms.pharmacy.service.DispenseService;
import com.hms.security.jwt.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/pharmacy/dispenses")
@RequiredArgsConstructor
@Tag(name = "Pharmacy - Dispensing")
@PreAuthorize("hasAnyRole('HOSPITAL_ADMIN','PHARMACIST')")
public class DispenseController {

    private final DispenseService dispenseService;
    private final PageRequestFactory pageRequestFactory;

    @PostMapping
    @Operation(summary = "Dispense drugs to a patient", description = "Deducts stock FEFO (first-expiry-first-out) across batches.")
    public ApiResponse<DispenseResponse> dispense(@Valid @RequestBody DispenseRequest request,
                                                   @AuthenticationPrincipal AuthenticatedUser actor) {
        return ApiResponse.ok("Dispensed", DispenseResponse.from(dispenseService.dispense(request, actor.userId())));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get dispense record by id")
    public ApiResponse<DispenseResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(DispenseResponse.from(dispenseService.getOrThrow(id)));
    }

    @GetMapping("/by-patient/{patientId}")
    @Operation(summary = "Patient's dispense history (paginated)")
    public ApiResponse<PageResponse<DispenseResponse>> byPatient(
            @PathVariable Long patientId,
            @RequestParam(required = false) Integer page, @RequestParam(required = false) Integer size) {
        var pageable = pageRequestFactory.of(page, size, "dispensedAt", "desc");
        return ApiResponse.ok(PageResponse.of(dispenseService.byPatient(patientId, pageable).map(DispenseResponse::from)));
    }
}
