package com.hms.billing.repository;

import com.hms.billing.entity.BillRefund;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BillRefundRepository extends JpaRepository<BillRefund, Long> {
    List<BillRefund> findByBillId(Long billId);
}
