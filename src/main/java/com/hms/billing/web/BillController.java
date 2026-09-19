package com.hms.billing.web;

import com.hms.billing.dto.*;
import com.hms.billing.entity.BillStatus;
import com.hms.billing.service.BillService;
import com.hms.common.PageRequestFactory;
import com.hms.common.dto.ApiResponse;
import com.hms.common.dto.PageResponse;
import com.hms.security.jwt.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/bills")
@RequiredArgsConstructor
@Tag(name = "Billing - Bills")
@PreAuthorize("hasAnyRole('HOSPITAL_ADMIN','BILLING_STAFF','RECEPTIONIST')")
public class BillController {

    private final BillService billService;
    private final PageRequestFactory pageRequestFactory;

    @PostMapping
    @Operation(summary = "Create a draft bill with line items")
    public ApiResponse<BillResponse> create(@Valid @RequestBody CreateBillRequest request) {
        return ApiResponse.ok("Bill created", BillResponse.from(billService.create(request)));
    }

    @PatchMapping("/{id}/finalize")
    @Operation(summary = "Finalize a bill", description = "Applies discount/tax, locks line items, makes it payable.")
    public ApiResponse<BillResponse> finalizeBill(@PathVariable Long id, @RequestBody FinalizeBillRequest request) {
        return ApiResponse.ok(BillResponse.from(billService.finalizeBill(id, request.discountAmount(), request.taxAmount())));
    }

    @PostMapping("/{id}/receipts")
    @Operation(summary = "Record a payment receipt against a finalized bill")
    public ApiResponse<Void> recordReceipt(@PathVariable Long id, @Valid @RequestBody RecordReceiptRequest request,
                                            @AuthenticationPrincipal AuthenticatedUser actor) {
        billService.recordReceipt(id, request.amount(), request.paymentMode(), actor.userId());
        return ApiResponse.ok("Payment recorded", null);
    }

    @PostMapping("/{id}/refunds")
    @PreAuthorize("hasRole('HOSPITAL_ADMIN')")
    @Operation(summary = "Issue a refund against a bill")
    public ApiResponse<Void> refund(@PathVariable Long id, @Valid @RequestBody RefundRequest request,
                                     @AuthenticationPrincipal AuthenticatedUser actor) {
        billService.refund(id, request.amount(), request.reason(), actor.userId());
        return ApiResponse.ok("Refund issued", null);
    }

    @PatchMapping("/{id}/cancel")
    @Operation(summary = "Cancel a draft/unpaid bill")
    public ApiResponse<BillResponse> cancel(@PathVariable Long id) {
        return ApiResponse.ok(BillResponse.from(billService.cancel(id)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get bill by id")
    public ApiResponse<BillResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(BillResponse.from(billService.getOrThrow(id)));
    }

    @GetMapping
    @Operation(summary = "Search/list bills (paginated)")
    public ApiResponse<PageResponse<BillResponse>> search(
            @RequestParam(required = false) Long patientId,
            @RequestParam(required = false) BillStatus status,
            @RequestParam(required = false) Integer page, @RequestParam(required = false) Integer size) {
        var pageable = pageRequestFactory.of(page, size, "createdAt", "desc");
        return ApiResponse.ok(PageResponse.of(billService.search(patientId, status, pageable).map(BillResponse::from)));
    }
}
