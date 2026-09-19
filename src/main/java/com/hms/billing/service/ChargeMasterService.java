package com.hms.billing.service;

import com.hms.billing.dto.ChargeMasterRequest;
import com.hms.billing.entity.ChargeMaster;
import com.hms.billing.entity.ChargeType;
import com.hms.billing.repository.ChargeMasterRepository;
import com.hms.billing.repository.ChargeTypeRepository;
import com.hms.common.dto.OptionDto;
import com.hms.common.exception.DuplicateResourceException;
import com.hms.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Charge master is reference data staff look up dozens of times a day when
 * building bills - cached aggressively (see hms.pagination + application.yml
 * caffeine spec) and evicted on any write.
 */
@Service
@RequiredArgsConstructor
@Transactional("transactionManager")
public class ChargeMasterService {

    private final ChargeMasterRepository chargeMasterRepository;
    private final ChargeTypeRepository chargeTypeRepository;

    @CacheEvict(cacheNames = "chargeMasterAutocomplete", allEntries = true)
    public ChargeMaster create(ChargeMasterRequest r) {
        if (chargeMasterRepository.findByCode(r.code()).isPresent()) {
            throw new DuplicateResourceException("Charge code '" + r.code() + "' already exists");
        }
        ChargeType type = chargeTypeRepository.findById(r.chargeTypeId())
                .orElseThrow(() -> ResourceNotFoundException.of("ChargeType", r.chargeTypeId()));
        ChargeMaster cm = new ChargeMaster();
        cm.setChargeType(type);
        cm.setCode(r.code());
        cm.setName(r.name());
        cm.setDefaultPrice(r.defaultPrice());
        return chargeMasterRepository.save(cm);
    }

    @Transactional(value = "transactionManager", readOnly = true)
    public Page<ChargeMaster> search(String q, Pageable pageable) {
        return chargeMasterRepository.search(q, pageable);
    }

    @Cacheable(cacheNames = "chargeMasterAutocomplete", key = "#q")
    @Transactional(value = "transactionManager", readOnly = true)
    public List<OptionDto> autocomplete(String q, int limit) {
        return chargeMasterRepository.autocomplete(q, PageRequest.of(0, Math.min(limit, 20)));
    }
}
