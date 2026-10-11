package com.hms.billing.repository;

import com.hms.billing.entity.DiscountMaster;
import com.hms.billing.entity.DiscountKind;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DiscountMasterRepository extends JpaRepository<DiscountMaster, Long> {
	List<DiscountMaster> findByActiveTrueAndDiscountKindOrderByNameAsc(DiscountKind discountKind);
	Optional<DiscountMaster> findByNameIgnoreCase(String name);
}
