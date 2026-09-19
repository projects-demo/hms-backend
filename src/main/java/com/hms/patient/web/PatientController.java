package com.hms.patient.web;

import com.hms.common.PageRequestFactory;
import com.hms.common.dto.ApiResponse;
import com.hms.common.dto.OptionDto;
import com.hms.common.dto.PageResponse;
import com.hms.patient.dto.PatientRequest;
import com.hms.patient.dto.PatientResponse;
import com.hms.patient.entity.Patient;
import com.hms.patient.service.PatientService;
import com.hms.security.jwt.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/patients")
@RequiredArgsConstructor
@Tag(name = "Patients", description = "Patient registration & search")
public class PatientController {

    private final PatientService patientService;
    private final PageRequestFactory pageRequestFactory;

    @PostMapping
    @PreAuthorize("hasAnyRole('HOSPITAL_ADMIN','RECEPTIONIST','NURSE','DOCTOR')")
    @Operation(summary = "Register a new patient")
    public ApiResponse<PatientResponse> create(@Valid @RequestBody PatientRequest request,
                                                @AuthenticationPrincipal AuthenticatedUser actor) {
        Patient p = patientService.create(request, actor.userId());
        return ApiResponse.ok("Patient registered", PatientResponse.from(p));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('HOSPITAL_ADMIN','RECEPTIONIST','NURSE','DOCTOR')")
    @Operation(summary = "Update patient details")
    public ApiResponse<PatientResponse> update(@PathVariable Long id, @Valid @RequestBody PatientRequest request,
                                                @AuthenticationPrincipal AuthenticatedUser actor) {
        return ApiResponse.ok(PatientResponse.from(patientService.update(id, request, actor.userId())));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get patient by id")
    public ApiResponse<PatientResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(PatientResponse.from(patientService.getOrThrow(id)));
    }

    @GetMapping
    @Operation(summary = "Search/list patients (paginated)",
            description = "Matches name, phone or patient code. Omit `q` to list all, paginated.")
    public ApiResponse<PageResponse<PatientResponse>> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        var pageable = pageRequestFactory.of(page, size, "lastName", "asc");
        return ApiResponse.ok(PageResponse.of(patientService.search(q, pageable).map(PatientResponse::from)));
    }

    @GetMapping("/autocomplete")
    @Operation(summary = "Typeahead search for patient pickers",
            description = "Returns at most `limit` (default 10, max 20) lightweight {id,label,subLabel} entries - " +
                    "use this instead of `search` for dropdowns/comboboxes so the UI never loads full patient records just to pick one.")
    public ApiResponse<java.util.List<OptionDto>> autocomplete(
            @RequestParam @Parameter(description = "Name, phone, or patient code prefix") String q,
            @RequestParam(required = false, defaultValue = "10") int limit) {
        return ApiResponse.ok(patientService.autocomplete(q, limit));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('HOSPITAL_ADMIN')")
    @Operation(summary = "Delete a patient record (admin only)")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        patientService.delete(id);
        return ApiResponse.ok("Deleted", null);
    }
}
