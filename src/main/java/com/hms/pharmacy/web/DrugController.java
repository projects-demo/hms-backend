package com.hms.pharmacy.web;

import com.hms.common.PageRequestFactory;
import com.hms.common.dto.ApiResponse;
import com.hms.common.dto.OptionDto;
import com.hms.common.dto.PageResponse;
import com.hms.pharmacy.dto.*;
import com.hms.pharmacy.service.DrugService;
import com.hms.security.jwt.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/pharmacy/drugs")
@RequiredArgsConstructor
@Tag(name = "Pharmacy - Drugs & Stock")
public class DrugController {

    private final DrugService drugService;
    private final PageRequestFactory pageRequestFactory;

    @PostMapping
    @PreAuthorize("hasAnyRole('HOSPITAL_ADMIN','PHARMACIST')")
    @Operation(summary = "Add a drug to the catalog")
    public ApiResponse<DrugResponse> create(@Valid @RequestBody DrugRequest request) {
        return ApiResponse.ok(DrugResponse.from(drugService.create(request)));
    }

    @GetMapping
    @Operation(summary = "Search/list drugs (paginated)")
    public ApiResponse<PageResponse<DrugResponse>> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Integer page, @RequestParam(required = false) Integer size) {
        var pageable = pageRequestFactory.of(page, size, "name", "asc");
        return ApiResponse.ok(PageResponse.of(drugService.search(q, pageable).map(DrugResponse::from)));
    }

    @GetMapping("/autocomplete")
    @Operation(summary = "Typeahead search for drug pickers (prescribing, dispensing)")
    public ApiResponse<List<OptionDto>> autocomplete(@RequestParam String q, @RequestParam(required = false, defaultValue = "10") int limit) {
        return ApiResponse.ok(drugService.autocomplete(q, limit));
    }

    @PostMapping("/batches")
    @PreAuthorize("hasAnyRole('HOSPITAL_ADMIN','PHARMACIST')")
    @Operation(summary = "Receive a new stock batch (purchase entry)")
    public ApiResponse<BatchResponse> receiveBatch(@Valid @RequestBody ReceiveBatchRequest request,
                                                    @AuthenticationPrincipal AuthenticatedUser actor) {
        return ApiResponse.ok("Batch received", BatchResponse.from(drugService.receiveBatch(request, actor.userId())));
    }

    @GetMapping("/low-stock")
    @Operation(summary = "Batches at/below reorder level", description = "Small result set by nature - not paginated.")
    public ApiResponse<List<BatchResponse>> lowStock() {
        return ApiResponse.ok(drugService.lowStock().stream().map(BatchResponse::from).toList());
    }

    @GetMapping("/expiring")
    @Operation(summary = "Batches expiring within N days (default 30)")
    public ApiResponse<List<BatchResponse>> expiring(@RequestParam(required = false, defaultValue = "30") int days) {
        return ApiResponse.ok(drugService.expiringWithinDays(days).stream().map(BatchResponse::from).toList());
    }
}
