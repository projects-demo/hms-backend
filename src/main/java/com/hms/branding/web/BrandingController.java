package com.hms.branding.web;

import com.hms.branding.dto.BrandingRequest;
import com.hms.branding.dto.BrandingResponse;
import com.hms.branding.service.BrandingService;
import com.hms.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/branding")
@RequiredArgsConstructor
@Tag(name = "Branding", description = "Per-hospital light UI customization: logo, accent color, header banner, footer text")
public class BrandingController {

    private final BrandingService brandingService;

    @GetMapping
    @Operation(summary = "Get this hospital's branding", description = "Readable by any logged-in staff member - the whole UI applies it, not just admins.")
    public ApiResponse<BrandingResponse> get() {
        return ApiResponse.ok(brandingService.get());
    }

    @PutMapping
    @PreAuthorize("hasRole('HOSPITAL_ADMIN')")
    @Operation(summary = "Update this hospital's branding")
    public ApiResponse<BrandingResponse> update(@Valid @RequestBody BrandingRequest request) {
        return ApiResponse.ok("Branding updated", brandingService.update(request));
    }
}