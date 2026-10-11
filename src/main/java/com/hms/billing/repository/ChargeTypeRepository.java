package com.hms.billing.repository;

import com.hms.billing.entity.ChargeType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ChargeTypeRepository extends JpaRepository<ChargeType, Long> {
	Optional<ChargeType> findByCode(String code);
	long countByActiveTrue();
}
