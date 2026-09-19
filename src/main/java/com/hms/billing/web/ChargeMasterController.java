package com.hms.billing.web;

import com.hms.billing.dto.ChargeMasterRequest;
import com.hms.billing.dto.ChargeMasterResponse;
import com.hms.billing.service.ChargeMasterService;
import com.hms.common.PageRequestFactory;
import com.hms.common.dto.ApiResponse;
import com.hms.common.dto.OptionDto;
import com.hms.common.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/charge-master")
@RequiredArgsConstructor
@Tag(name = "Billing - Charge Master")
public class ChargeMasterController {

    private final ChargeMasterService chargeMasterService;
    private final PageRequestFactory pageRequestFactory;

    @PostMapping
    @PreAuthorize("hasRole('HOSPITAL_ADMIN')")
    @Operation(summary = "Add a chargeable item/service")
    public ApiResponse<ChargeMasterResponse> create(@Valid @RequestBody ChargeMasterRequest request) {
        return ApiResponse.ok(ChargeMasterResponse.from(chargeMasterService.create(request)));
    }

    @GetMapping
    @Operation(summary = "Search/list charge master (paginated)")
    public ApiResponse<PageResponse<ChargeMasterResponse>> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Integer page, @RequestParam(required = false) Integer size) {
        var pageable = pageRequestFactory.of(page, size, "name", "asc");
        return ApiResponse.ok(PageResponse.of(chargeMasterService.search(q, pageable).map(ChargeMasterResponse::from)));
    }

    @GetMapping("/autocomplete")
    @Operation(summary = "Typeahead search for billing screens")
    public ApiResponse<List<OptionDto>> autocomplete(@RequestParam String q, @RequestParam(required = false, defaultValue = "10") int limit) {
        return ApiResponse.ok(chargeMasterService.autocomplete(q, limit));
    }
}
