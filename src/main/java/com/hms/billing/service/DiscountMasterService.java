package com.hms.billing.service;

import com.hms.billing.dto.DiscountMasterRequest;
import com.hms.billing.entity.DiscountKind;
import com.hms.billing.entity.DiscountMaster;
import com.hms.billing.repository.DiscountMasterRepository;
import com.hms.common.exception.DuplicateResourceException;
import com.hms.common.exception.ResourceNotFoundException;
import com.hms.security.jwt.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional("transactionManager")
public class DiscountMasterService {

    private final DiscountMasterRepository discountMasterRepository;
    private final BillingAuditService billingAuditService;

    @Transactional(value = "transactionManager", readOnly = true)
    public List<DiscountMaster> listActive() {
        return discountMasterRepository.findByActiveTrueAndDiscountKindOrderByNameAsc(DiscountKind.PERCENT);
    }

    public DiscountMaster create(DiscountMasterRequest request, AuthenticatedUser actor) {
        ensureUniqueName(request.name(), null);
        DiscountMaster discount = new DiscountMaster();
        discount.setName(request.name().trim());
        discount.setDiscountKind(DiscountKind.PERCENT);
        discount.setValue(request.percentage());
        discount.setActive(true);
        discount = discountMasterRepository.save(discount);
        billingAuditService.record("DISCOUNT_ADDED", "DISCOUNT_MASTER", discount.getId(), null, actor,
            java.util.Map.of("name", discount.getName(), "percentage", discount.getValue()));
        return discount;
    }

        public DiscountMaster update(Long id, DiscountMasterRequest request, AuthenticatedUser actor) {
        DiscountMaster discount = discountMasterRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Discount", id));
        ensureUniqueName(request.name(), id);
        String previousName = discount.getName();
        var previousPercentage = discount.getValue();
        discount.setName(request.name().trim());
        discount.setDiscountKind(DiscountKind.PERCENT);
        discount.setValue(request.percentage());
        discount = discountMasterRepository.save(discount);
        billingAuditService.record("DISCOUNT_EDITED", "DISCOUNT_MASTER", discount.getId(), null, actor,
            java.util.Map.of("before", java.util.Map.of("name", previousName, "percentage", previousPercentage),
                "after", java.util.Map.of("name", discount.getName(), "percentage", discount.getValue())));
        return discount;
    }

    private void ensureUniqueName(String name, Long currentId) {
        discountMasterRepository.findByNameIgnoreCase(name.trim())
                .filter(existing -> !existing.getId().equals(currentId))
                .ifPresent(existing -> { throw new DuplicateResourceException("Discount '" + name + "' already exists"); });
    }
}