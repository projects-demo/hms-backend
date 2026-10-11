package com.hms.billing.web;

import com.hms.billing.entity.BillingAuditLog;
import com.hms.billing.service.BillingAuditService;
import com.hms.common.PageRequestFactory;
import com.hms.common.dto.ApiResponse;
import com.hms.common.dto.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/billing-audit-logs")
@RequiredArgsConstructor
@PreAuthorize("hasRole('HOSPITAL_ADMIN')")
public class BillingAuditController {

    private final BillingAuditService billingAuditService;
    private final PageRequestFactory pageRequestFactory;

    @GetMapping
    public ApiResponse<PageResponse<BillingAuditLog>> list(
            @RequestParam(required = false) Long billId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        var pageable = pageRequestFactory.of(page, size, "createdAt", "desc");
        return ApiResponse.ok(PageResponse.of(billingAuditService.search(billId, pageable)));
    }
}