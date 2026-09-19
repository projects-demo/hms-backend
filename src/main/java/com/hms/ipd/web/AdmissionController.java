package com.hms.ipd.web;

import com.hms.common.PageRequestFactory;
import com.hms.common.dto.ApiResponse;
import com.hms.common.dto.PageResponse;
import com.hms.ipd.dto.*;
import com.hms.ipd.entity.AdmissionStatus;
import com.hms.ipd.service.AdmissionService;
import com.hms.security.jwt.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admissions")
@RequiredArgsConstructor
@Tag(name = "IPD - Admissions")
public class AdmissionController {

    private final AdmissionService admissionService;
    private final PageRequestFactory pageRequestFactory;

    @PostMapping
    @PreAuthorize("hasAnyRole('HOSPITAL_ADMIN','DOCTOR','NURSE','RECEPTIONIST')")
    @Operation(summary = "Admit a patient", description = "Assigns the bed and creates the admission record atomically.")
    public ApiResponse<AdmissionResponse> admit(@Valid @RequestBody AdmitPatientRequest request,
                                                 @AuthenticationPrincipal AuthenticatedUser actor) {
        return ApiResponse.ok("Patient admitted", AdmissionResponse.from(admissionService.admit(request, actor.userId())));
    }

    @PatchMapping("/{id}/discharge")
    @PreAuthorize("hasAnyRole('HOSPITAL_ADMIN','DOCTOR')")
    @Operation(summary = "Discharge a patient", description = "Frees the bed and records final diagnosis + discharge summary.")
    public ApiResponse<AdmissionResponse> discharge(@PathVariable Long id, @Valid @RequestBody DischargeRequest request) {
        return ApiResponse.ok(AdmissionResponse.from(admissionService.discharge(id, request)));
    }

    @PostMapping("/{id}/round-notes")
    @PreAuthorize("hasAnyRole('DOCTOR','NURSE')")
    @Operation(summary = "Add a nursing/doctor round note (vitals + notes)")
    public ApiResponse<RoundNoteResponse> addRoundNote(@PathVariable Long id, @RequestBody RoundNoteRequest request,
                                                        @AuthenticationPrincipal AuthenticatedUser actor) {
        return ApiResponse.ok(RoundNoteResponse.from(admissionService.addRoundNote(id, request, actor.userId())));
    }

    @GetMapping("/{id}/round-notes")
    @Operation(summary = "List round notes for an admission (paginated)")
    public ApiResponse<PageResponse<RoundNoteResponse>> roundNotes(
            @PathVariable Long id,
            @RequestParam(required = false) Integer page, @RequestParam(required = false) Integer size) {
        var pageable = pageRequestFactory.of(page, size, null, null);
        return ApiResponse.ok(PageResponse.of(admissionService.roundNotes(id, pageable).map(RoundNoteResponse::from)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get admission by id")
    public ApiResponse<AdmissionResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(AdmissionResponse.from(admissionService.getOrThrow(id)));
    }

    @GetMapping
    @Operation(summary = "Search/list admissions (paginated)")
    public ApiResponse<PageResponse<AdmissionResponse>> search(
            @RequestParam(required = false) Long patientId,
            @RequestParam(required = false) AdmissionStatus status,
            @RequestParam(required = false) Integer page, @RequestParam(required = false) Integer size) {
        var pageable = pageRequestFactory.of(page, size, "admissionDate", "desc");
        return ApiResponse.ok(PageResponse.of(admissionService.search(patientId, status, pageable).map(AdmissionResponse::from)));
    }
}
