package com.hms.billing.web;

import com.hms.billing.dto.ChargeMasterRequest;
import com.hms.billing.dto.ChargeMasterResponse;
import com.hms.billing.service.ChargeMasterService;
import com.hms.common.PageRequestFactory;
import com.hms.common.dto.ApiResponse;
import com.hms.common.dto.OptionDto;
import com.hms.common.dto.PageResponse;
import com.hms.security.jwt.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
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
    @Transactional("transactionManager")
    public ApiResponse<ChargeMasterResponse> create(@Valid @RequestBody ChargeMasterRequest request,
                                                     @AuthenticationPrincipal AuthenticatedUser actor) {
        return ApiResponse.ok(ChargeMasterResponse.from(chargeMasterService.create(request, actor)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('HOSPITAL_ADMIN')")
    @Operation(summary = "Update a standard chargeable item and price")
    @Transactional("transactionManager")
    public ApiResponse<ChargeMasterResponse> update(@PathVariable Long id, @Valid @RequestBody ChargeMasterRequest request,
                                                     @AuthenticationPrincipal AuthenticatedUser actor) {
        return ApiResponse.ok(ChargeMasterResponse.from(chargeMasterService.update(id, request, actor)));
    }

    @PatchMapping("/{id}/active")
    @PreAuthorize("hasRole('HOSPITAL_ADMIN')")
    @Transactional("transactionManager")
    public ApiResponse<ChargeMasterResponse> setActive(@PathVariable Long id, @RequestParam boolean active,
                                                        @AuthenticationPrincipal AuthenticatedUser actor) {
        return ApiResponse.ok(ChargeMasterResponse.from(chargeMasterService.setActive(id, active, actor)));
    }

    @GetMapping("/summary")
    @PreAuthorize("hasRole('HOSPITAL_ADMIN')")
    public ApiResponse<ChargeMasterService.ChargeMasterSummary> summary() {
        return ApiResponse.ok(chargeMasterService.summary());
    }

    @GetMapping
    @Operation(summary = "Search/list charge master (paginated)")
    public ApiResponse<PageResponse<ChargeMasterResponse>> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long chargeTypeId,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false, defaultValue = "false") boolean includeInactive,
            @AuthenticationPrincipal AuthenticatedUser actor,
            @RequestParam(required = false) Integer page, @RequestParam(required = false) Integer size) {
        if (includeInactive && (actor == null || !"HOSPITAL_ADMIN".equals(actor.role()))) {
            throw new AccessDeniedException("Only a hospital admin can view inactive billing charges");
        }
        var pageable = pageRequestFactory.of(page, size, "name", "asc");
        return ApiResponse.ok(PageResponse.of(chargeMasterService.search(q, chargeTypeId, active, includeInactive, pageable)
                .map(ChargeMasterResponse::from)));
    }

    @GetMapping("/autocomplete")
    @Operation(summary = "Typeahead search for billing screens")
    public ApiResponse<List<OptionDto>> autocomplete(@RequestParam String q, @RequestParam(required = false, defaultValue = "10") int limit) {
        return ApiResponse.ok(chargeMasterService.autocomplete(q, limit));
    }
}
