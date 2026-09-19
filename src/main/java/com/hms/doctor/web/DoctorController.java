package com.hms.doctor.web;

import com.hms.common.PageRequestFactory;
import com.hms.common.dto.ApiResponse;
import com.hms.common.dto.OptionDto;
import com.hms.common.dto.PageResponse;
import com.hms.doctor.dto.DoctorRequest;
import com.hms.doctor.dto.DoctorResponse;
import com.hms.doctor.entity.Doctor;
import com.hms.doctor.service.DoctorService;
import com.hms.identity.repository.AppUserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/doctors")
@RequiredArgsConstructor
@Tag(name = "Doctors")
public class DoctorController {

    private final DoctorService doctorService;
    private final PageRequestFactory pageRequestFactory;
    private final AppUserRepository appUserRepository;

    @PostMapping
    @PreAuthorize("hasRole('HOSPITAL_ADMIN')")
    @Operation(summary = "Onboard a new doctor", description = "Creates the doctor's login (username/password) and clinical profile together.")
    public ApiResponse<DoctorResponse> create(@Valid @RequestBody DoctorRequest request) {
        Doctor doctor = doctorService.create(request);
        return ApiResponse.ok("Doctor onboarded", toResponse(doctor));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('HOSPITAL_ADMIN')")
    @Operation(summary = "Update doctor profile")
    public ApiResponse<DoctorResponse> update(@PathVariable Long id, @Valid @RequestBody DoctorRequest request) {
        Doctor doctor = doctorService.update(id, request);
        return ApiResponse.ok(toResponse(doctor));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get doctor by id")
    public ApiResponse<DoctorResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(toResponse(doctorService.getOrThrow(id)));
    }

    @GetMapping
    @Operation(summary = "Search/list doctors (paginated)", description = "Filter by departmentId and/or specialization, or free-text `q`.")
    @Transactional(value = "transactionManager", readOnly = true)
    public ApiResponse<PageResponse<DoctorResponse>> search(
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) String specialization,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        var pageable = pageRequestFactory.of(page, size, "doctorCode", "asc");
        Page<Doctor> result = doctorService.search(departmentId, specialization, q, pageable);

        // Batch-fetch names for every doctor on this page in one query instead of
        // one lookup per row (N+1) - full name lives on AppUser, not on Doctor.
        Map<Long, String> namesByUserId = appUserRepository
                .findAllById(result.getContent().stream().map(Doctor::getUserId).toList())
                .stream()
                .collect(Collectors.toMap(u -> u.getId(), u -> u.getFullName(), (a, b) -> a));

        return ApiResponse.ok(PageResponse.of(result.map(d ->
                DoctorResponse.from(d, namesByUserId.getOrDefault(d.getUserId(), "—")))));
    }

    @GetMapping("/autocomplete")
    @Operation(summary = "Typeahead search doctors by name", description = "Used by appointment-booking screens.")
    public ApiResponse<List<OptionDto>> autocomplete(@RequestParam String q,
                                                       @RequestParam(required = false, defaultValue = "10") int limit) {
        return ApiResponse.ok(doctorService.autocompleteByName(q, limit));
    }

    @PatchMapping("/{id}/availability-status")
    @PreAuthorize("hasAnyRole('HOSPITAL_ADMIN','DOCTOR')")
    @Operation(summary = "Set availability status", description = "AVAILABLE, ON_LEAVE, or INACTIVE")
    public ApiResponse<Void> setStatus(@PathVariable Long id, @RequestParam String status) {
        doctorService.setAvailabilityStatus(id, status);
        return ApiResponse.ok("Updated", null);
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('HOSPITAL_ADMIN')")
    @Operation(summary = "Deactivate a doctor", description = "Soft delete - hides them from search/booking but preserves all historical records.")
    public ApiResponse<Void> deactivate(@PathVariable Long id) {
        doctorService.deactivate(id);
        return ApiResponse.ok("Doctor deactivated", null);
    }

    private DoctorResponse toResponse(Doctor doctor) {
        String fullName = appUserRepository.findById(doctor.getUserId())
                .map(u -> u.getFullName())
                .orElse("—");
        return DoctorResponse.from(doctor, fullName);
    }
}