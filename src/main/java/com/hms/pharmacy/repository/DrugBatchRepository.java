package com.hms.pharmacy.repository;

import com.hms.pharmacy.entity.DrugBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DrugBatchRepository extends JpaRepository<DrugBatch, Long> {

    // FEFO (first-expiry-first-out): batches ordered by soonest expiry, only those with stock left.
    @Query("select b from DrugBatch b where b.drug.id = :drugId and b.quantityAvailable > 0 order by b.expiryDate asc")
    List<DrugBatch> findDispensableBatches(@Param("drugId") Long drugId);

    @Query("select b from DrugBatch b join fetch b.drug where b.drug.id = :drugId order by b.expiryDate asc")
    List<DrugBatch> findByDrugId(@Param("drugId") Long drugId);

    @Query("select b from DrugBatch b join fetch b.drug where b.expiryDate <= :cutoff and b.quantityAvailable > 0 order by b.expiryDate asc")
    List<DrugBatch> findExpiringBy(@Param("cutoff") java.time.LocalDate cutoff);

    @Query("select b from DrugBatch b join fetch b.drug d where d.active = true and b.quantityAvailable <= d.reorderLevel")
    List<DrugBatch> findLowStock();
}
