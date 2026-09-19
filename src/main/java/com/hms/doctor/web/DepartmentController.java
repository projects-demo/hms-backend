package com.hms.doctor.web;

import com.hms.common.PageRequestFactory;
import com.hms.common.dto.ApiResponse;
import com.hms.common.dto.PageResponse;
import com.hms.doctor.dto.DepartmentRequest;
import com.hms.doctor.dto.DepartmentResponse;
import com.hms.doctor.service.DepartmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/departments")
@RequiredArgsConstructor
@Tag(name = "Departments")
public class DepartmentController {

    private final DepartmentService departmentService;
    private final PageRequestFactory pageRequestFactory;

    @PostMapping
    @PreAuthorize("hasRole('HOSPITAL_ADMIN')")
    @Operation(summary = "Create department")
    public ApiResponse<DepartmentResponse> create(@Valid @RequestBody DepartmentRequest request) {
        return ApiResponse.ok(DepartmentResponse.from(departmentService.create(request)));
    }

    @GetMapping
    @Operation(summary = "List departments (paginated)")
    public ApiResponse<PageResponse<DepartmentResponse>> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        var pageable = pageRequestFactory.of(page, size, "name", "asc");
        return ApiResponse.ok(PageResponse.of(departmentService.list(q, pageable).map(DepartmentResponse::from)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get department")
    public ApiResponse<DepartmentResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(DepartmentResponse.from(departmentService.getOrThrow(id)));
    }
}
