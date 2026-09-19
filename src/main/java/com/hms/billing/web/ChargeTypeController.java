package com.hms.billing.web;

import com.hms.billing.entity.ChargeType;
import com.hms.billing.repository.ChargeTypeRepository;
import com.hms.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/charge-types")
@RequiredArgsConstructor
@Tag(name = "Billing - Charge Types", description = "Top-level categories (CONSULTATION, PHARMACY, LAB, BED_CHARGE, ...) that charge-master items belong to")
public class ChargeTypeController {

    private final ChargeTypeRepository chargeTypeRepository;

    public record ChargeTypeRequest(@NotBlank String name) {}
    public record ChargeTypeResponse(Long id, String name) {
        static ChargeTypeResponse from(ChargeType c) { return new ChargeTypeResponse(c.getId(), c.getName()); }
    }

    @PostMapping
    @PreAuthorize("hasRole('HOSPITAL_ADMIN')")
    @Operation(summary = "Create a charge type")
    public ApiResponse<ChargeTypeResponse> create(@org.springframework.web.bind.annotation.RequestBody ChargeTypeRequest request) {
        ChargeType ct = new ChargeType();
        ct.setName(request.name());
        return ApiResponse.ok(ChargeTypeResponse.from(chargeTypeRepository.save(ct)));
    }

    @GetMapping
    @Operation(summary = "List all charge types", description = "Small, static reference list - not paginated.")
    public ApiResponse<List<ChargeTypeResponse>> list() {
        return ApiResponse.ok(chargeTypeRepository.findAll().stream().map(ChargeTypeResponse::from).toList());
    }
}
