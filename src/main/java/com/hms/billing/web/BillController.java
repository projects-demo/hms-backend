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
import org.springframework.transaction.annotation.Transactional;
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
    @Transactional("transactionManager")
    @Operation(summary = "Create a draft bill with line items")
    public ApiResponse<BillResponse> create(@Valid @RequestBody CreateBillRequest request,
                                             @AuthenticationPrincipal AuthenticatedUser actor) {
        return ApiResponse.ok("Bill created", billService.response(billService.create(request, actor)));
    }

    @PutMapping("/{id}")
    @Transactional("transactionManager")
    @Operation(summary = "Update a draft bill")
    public ApiResponse<BillResponse> updateDraft(@PathVariable Long id, @Valid @RequestBody CreateBillRequest request,
                                                  @AuthenticationPrincipal AuthenticatedUser actor) {
        return ApiResponse.ok("Draft bill updated", billService.response(billService.updateDraft(id, request, actor)));
    }

    @PatchMapping("/{id}/finalize")
    @Transactional("transactionManager")
    @Operation(summary = "Finalize a bill", description = "Applies discount/tax, locks line items, makes it payable.")
    public ApiResponse<BillResponse> finalizeBill(@PathVariable Long id, @RequestBody FinalizeBillRequest request,
                                                   @AuthenticationPrincipal AuthenticatedUser actor) {
        return ApiResponse.ok(billService.response(billService.finalizeBill(
            id, request.discountMasterId(), request.taxAmount(), request.paymentMode(), actor)));
    }

    @GetMapping("/patients/{patientId}/receipts")
    @Transactional(value = "transactionManager", readOnly = true)
    @Operation(summary = "List a patient's payment receipt history")
    public ApiResponse<PageResponse<PaymentReceiptResponse>> patientReceiptHistory(
            @PathVariable Long patientId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        var pageable = pageRequestFactory.of(page, size, "receivedAt", "desc");
        return ApiResponse.ok(PageResponse.of(billService.patientReceiptHistory(patientId, pageable)));
    }

    @PostMapping("/{id}/refunds")
    @PreAuthorize("hasRole('HOSPITAL_ADMIN')")
    @Operation(summary = "Issue a refund against a bill")
    public ApiResponse<Void> refund(@PathVariable Long id, @Valid @RequestBody RefundRequest request,
                                     @AuthenticationPrincipal AuthenticatedUser actor) {
        billService.refund(id, request.amount(), request.reason(), actor);
        return ApiResponse.ok("Refund issued", null);
    }

    @PatchMapping("/{id}/void")
    @PreAuthorize("hasRole('HOSPITAL_ADMIN')")
    @Transactional("transactionManager")
    @Operation(summary = "Void a bill after confirming the hospital admin password")
    public ApiResponse<BillResponse> voidBill(@PathVariable Long id, @Valid @RequestBody VoidBillRequest request,
                                               @AuthenticationPrincipal AuthenticatedUser actor) {
        return ApiResponse.ok("Bill voided", billService.response(billService.voidBill(id, request.password(), actor)));
    }

    @GetMapping("/{id}")
    @Transactional(value = "transactionManager", readOnly = true)
    @Operation(summary = "Get bill by id")
    public ApiResponse<BillResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(billService.response(billService.getOrThrow(id)));
    }

    @GetMapping
    @Transactional(value = "transactionManager", readOnly = true)
    @Operation(summary = "Search/list bills (paginated)")
    public ApiResponse<PageResponse<BillResponse>> search(
            @RequestParam(required = false) Long patientId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) BillStatus status,
            @RequestParam(required = false) Integer page, @RequestParam(required = false) Integer size) {
        var pageable = pageRequestFactory.of(page, size, "createdAt", "desc");
        return ApiResponse.ok(PageResponse.of(billService.search(patientId, q, status, pageable)));
    }
}
