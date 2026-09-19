package com.hms.billing.repository;

import com.hms.billing.entity.ChargeType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChargeTypeRepository extends JpaRepository<ChargeType, Long> {
}
