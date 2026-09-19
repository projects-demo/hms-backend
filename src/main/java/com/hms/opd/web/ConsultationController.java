package com.hms.opd.web;

import com.hms.common.PageRequestFactory;
import com.hms.common.dto.ApiResponse;
import com.hms.common.dto.PageResponse;
import com.hms.common.exception.ResourceNotFoundException;
import com.hms.opd.dto.CompleteConsultationRequest;
import com.hms.opd.dto.ConsultationResponse;
import com.hms.opd.dto.PrescriptionResponse;
import com.hms.opd.dto.StartConsultationRequest;
import com.hms.opd.repository.PrescriptionRepository;
import com.hms.opd.service.ConsultationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/consultations")
@RequiredArgsConstructor
@Tag(name = "OPD Consultations", description = "Vitals capture, diagnosis, and prescriptions for an OPD visit")
public class ConsultationController {

    private final ConsultationService consultationService;
    private final PrescriptionRepository prescriptionRepository;
    private final PageRequestFactory pageRequestFactory;

    @PostMapping
    @PreAuthorize("hasAnyRole('DOCTOR','NURSE')")
    @Operation(summary = "Start a consultation for an appointment")
    public ApiResponse<ConsultationResponse> start(@Valid @RequestBody StartConsultationRequest request) {
        return ApiResponse.ok(ConsultationResponse.from(consultationService.start(request)));
    }

    @PatchMapping("/{id}/complete")
    @PreAuthorize("hasRole('DOCTOR')")
    @Operation(summary = "Complete a consultation", description = "Records vitals, diagnosis, notes and prescription items in one call; auto-completes the appointment.")
    public ApiResponse<ConsultationResponse> complete(@PathVariable Long id, @RequestBody CompleteConsultationRequest request) {
        return ApiResponse.ok(ConsultationResponse.from(consultationService.complete(id, request)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get consultation by id")
    public ApiResponse<ConsultationResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(ConsultationResponse.from(consultationService.getOrThrow(id)));
    }

    @GetMapping("/{id}/prescription")
    @Operation(summary = "Get the prescription for a consultation")
    public ApiResponse<PrescriptionResponse> prescription(@PathVariable Long id) {
        var p = prescriptionRepository.findByConsultationId(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Prescription for consultation", id));
        return ApiResponse.ok(PrescriptionResponse.from(p));
    }

    @GetMapping("/by-patient/{patientId}")
    @Operation(summary = "Patient's consultation history (paginated)")
    public ApiResponse<PageResponse<ConsultationResponse>> byPatient(
            @PathVariable Long patientId,
            @RequestParam(required = false) Integer page, @RequestParam(required = false) Integer size) {
        var pageable = pageRequestFactory.of(page, size, "startedAt", "desc");
        return ApiResponse.ok(PageResponse.of(consultationService.byPatient(patientId, pageable).map(ConsultationResponse::from)));
    }

    @GetMapping("/by-doctor/{doctorId}")
    @Operation(summary = "Doctor's consultation list (paginated)")
    public ApiResponse<PageResponse<ConsultationResponse>> byDoctor(
            @PathVariable Long doctorId,
            @RequestParam(required = false) Integer page, @RequestParam(required = false) Integer size) {
        var pageable = pageRequestFactory.of(page, size, "startedAt", "desc");
        return ApiResponse.ok(PageResponse.of(consultationService.byDoctor(doctorId, pageable).map(ConsultationResponse::from)));
    }
}
