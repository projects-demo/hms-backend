package com.hms.staff.web;

import com.hms.common.PageRequestFactory;
import com.hms.common.dto.ApiResponse;
import com.hms.common.dto.PageResponse;
import com.hms.staff.dto.StaffRequest;
import com.hms.staff.dto.StaffResponse;
import com.hms.staff.entity.StaffType;
import com.hms.staff.service.StaffService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/staff")
@RequiredArgsConstructor
@Tag(name = "Staff", description = "Non-doctor hospital staff: nurses, receptionists, billing, pharmacists, lab techs")
public class StaffController {

    private final StaffService staffService;
    private final PageRequestFactory pageRequestFactory;

    @PostMapping
    @PreAuthorize("hasRole('HOSPITAL_ADMIN')")
    @Operation(summary = "Onboard a new staff member (creates login + profile)")
    public ApiResponse<StaffResponse> create(@Valid @RequestBody StaffRequest request) {
        return ApiResponse.ok("Staff onboarded", StaffResponse.from(staffService.create(request)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get staff by id")
    public ApiResponse<StaffResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(StaffResponse.from(staffService.getOrThrow(id)));
    }

    @GetMapping
    @Operation(summary = "Search/list staff (paginated)")
    public ApiResponse<PageResponse<StaffResponse>> search(
            @RequestParam(required = false) StaffType staffType,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        var pageable = pageRequestFactory.of(page, size, "staffCode", "asc");
        return ApiResponse.ok(PageResponse.of(staffService.search(staffType, q, pageable).map(StaffResponse::from)));
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('HOSPITAL_ADMIN')")
    @Operation(summary = "Deactivate a staff member")
    public ApiResponse<Void> deactivate(@PathVariable Long id) {
        staffService.deactivate(id);
        return ApiResponse.ok("Deactivated", null);
    }
}
