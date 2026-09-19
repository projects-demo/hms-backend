package com.hms.pharmacy.repository;

import com.hms.pharmacy.entity.StockTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockTransactionRepository extends JpaRepository<StockTransaction, Long> {
    Page<StockTransaction> findByDrugIdOrderByTxnAtDesc(Long drugId, Pageable pageable);
}
