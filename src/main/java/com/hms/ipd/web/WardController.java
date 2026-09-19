package com.hms.ipd.web;

import com.hms.common.PageRequestFactory;
import com.hms.common.dto.ApiResponse;
import com.hms.common.dto.PageResponse;
import com.hms.ipd.dto.*;
import com.hms.ipd.service.WardBedService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "IPD - Wards & Beds")
public class WardController {

    private final WardBedService wardBedService;
    private final PageRequestFactory pageRequestFactory;

    @PostMapping("/wards")
    @PreAuthorize("hasRole('HOSPITAL_ADMIN')")
    @Operation(summary = "Create a ward")
    public ApiResponse<WardResponse> createWard(@Valid @RequestBody WardRequest request) {
        return ApiResponse.ok(WardResponse.from(wardBedService.createWard(request)));
    }

    @GetMapping("/wards")
    @Operation(summary = "List wards (paginated)")
    public ApiResponse<PageResponse<WardResponse>> listWards(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Integer page, @RequestParam(required = false) Integer size) {
        var pageable = pageRequestFactory.of(page, size, "name", "asc");
        return ApiResponse.ok(PageResponse.of(wardBedService.listWards(q, pageable).map(WardResponse::from)));
    }

    @PostMapping("/beds")
    @PreAuthorize("hasRole('HOSPITAL_ADMIN')")
    @Operation(summary = "Add a bed to a ward")
    public ApiResponse<BedResponse> createBed(@Valid @RequestBody BedRequest request) {
        return ApiResponse.ok(BedResponse.from(wardBedService.createBed(request)));
    }

    @GetMapping("/beds")
    @Operation(summary = "List beds (paginated), optionally filtered by ward")
    public ApiResponse<PageResponse<BedResponse>> listBeds(
            @RequestParam(required = false) Long wardId,
            @RequestParam(required = false) Integer page, @RequestParam(required = false) Integer size) {
        var pageable = pageRequestFactory.of(page, size, "bedNumber", "asc");
        return ApiResponse.ok(PageResponse.of(wardBedService.searchBeds(wardId, pageable).map(BedResponse::from)));
    }

    @GetMapping("/beds/available")
    @Operation(summary = "List currently available beds", description = "Small result set by nature - not paginated. Optionally filter by ward.")
    public ApiResponse<List<BedResponse>> availableBeds(@RequestParam(required = false) Long wardId) {
        return ApiResponse.ok(wardBedService.availableBeds(wardId).stream().map(BedResponse::from).toList());
    }

    @PatchMapping("/beds/{id}/maintenance")
    @PreAuthorize("hasAnyRole('HOSPITAL_ADMIN','NURSE')")
    @Operation(summary = "Toggle a bed's maintenance status")
    public ApiResponse<BedResponse> maintenance(@PathVariable Long id, @RequestParam boolean underMaintenance) {
        return ApiResponse.ok(BedResponse.from(wardBedService.setMaintenance(id, underMaintenance)));
    }
}
