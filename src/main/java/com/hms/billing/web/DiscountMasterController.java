package com.hms.billing.web;

import com.hms.billing.dto.DiscountMasterRequest;
import com.hms.billing.dto.DiscountMasterResponse;
import com.hms.billing.service.DiscountMasterService;
import com.hms.common.dto.ApiResponse;
import com.hms.security.jwt.AuthenticatedUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/discount-master")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('HOSPITAL_ADMIN','BILLING_STAFF','RECEPTIONIST')")
public class DiscountMasterController {

    private final DiscountMasterService discountMasterService;

    @GetMapping
    public ApiResponse<List<DiscountMasterResponse>> listActive() {
        return ApiResponse.ok(discountMasterService.listActive().stream().map(DiscountMasterResponse::from).toList());
    }

    @PostMapping
    @PreAuthorize("hasRole('HOSPITAL_ADMIN')")
    public ApiResponse<DiscountMasterResponse> create(@Valid @RequestBody DiscountMasterRequest request,
                                                       @AuthenticationPrincipal AuthenticatedUser actor) {
        return ApiResponse.ok(DiscountMasterResponse.from(discountMasterService.create(request, actor)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('HOSPITAL_ADMIN')")
    public ApiResponse<DiscountMasterResponse> update(@PathVariable Long id, @Valid @RequestBody DiscountMasterRequest request,
                                                       @AuthenticationPrincipal AuthenticatedUser actor) {
        return ApiResponse.ok(DiscountMasterResponse.from(discountMasterService.update(id, request, actor)));
    }
}