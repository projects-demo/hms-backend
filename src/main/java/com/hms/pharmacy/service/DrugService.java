package com.hms.pharmacy.service;

import com.hms.common.codeseq.CodeGeneratorService;
import com.hms.common.dto.OptionDto;
import com.hms.common.exception.ResourceNotFoundException;
import com.hms.pharmacy.dto.DrugRequest;
import com.hms.pharmacy.dto.ReceiveBatchRequest;
import com.hms.pharmacy.entity.Drug;
import com.hms.pharmacy.entity.DrugBatch;
import com.hms.pharmacy.entity.StockTransaction;
import com.hms.pharmacy.entity.TxnType;
import com.hms.pharmacy.repository.DrugBatchRepository;
import com.hms.pharmacy.repository.DrugRepository;
import com.hms.pharmacy.repository.StockTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional("transactionManager")
public class DrugService {

    private final DrugRepository drugRepository;
    private final DrugBatchRepository drugBatchRepository;
    private final StockTransactionRepository stockTransactionRepository;
    private final CodeGeneratorService codeGeneratorService;

    public Drug create(DrugRequest r) {
        Drug d = new Drug();
        d.setDrugCode(codeGeneratorService.next("DRUG", "DG", 6));
        d.setName(r.name());
        d.setGenericName(r.genericName());
        d.setManufacturer(r.manufacturer());
        d.setCategory(r.category());
        d.setUnit(r.unit() == null || r.unit().isBlank() ? "UNIT" : r.unit());
        d.setReorderLevel(r.reorderLevel());
        return drugRepository.save(d);
    }

    @Transactional(value = "transactionManager", readOnly = true)
    public Page<Drug> search(String q, Pageable pageable) {
        return (q == null || q.isBlank()) ? drugRepository.findAll(pageable) : drugRepository.search(q, pageable);
    }

    @Transactional(value = "transactionManager", readOnly = true)
    public List<OptionDto> autocomplete(String q, int limit) {
        return drugRepository.autocomplete(q, PageRequest.of(0, Math.min(limit, 20)));
    }

    public DrugBatch receiveBatch(ReceiveBatchRequest r, Long actorUserId) {
        Drug drug = drugRepository.findById(r.drugId()).orElseThrow(() -> ResourceNotFoundException.of("Drug", r.drugId()));
        DrugBatch batch = new DrugBatch();
        batch.setDrug(drug);
        batch.setBatchNumber(r.batchNumber());
        batch.setExpiryDate(r.expiryDate());
        batch.setQuantityAvailable(r.quantity());
        batch.setPurchasePrice(r.purchasePrice());
        batch.setSellingPrice(r.sellingPrice());
        batch = drugBatchRepository.save(batch);

        StockTransaction txn = new StockTransaction();
        txn.setDrugId(drug.getId());
        txn.setBatchId(batch.getId());
        txn.setTxnType(TxnType.IN);
        txn.setQuantity(r.quantity());
        txn.setReferenceType("PURCHASE");
        txn.setPerformedBy(actorUserId);
        stockTransactionRepository.save(txn);

        return batch;
    }

    @Transactional(value = "transactionManager", readOnly = true)
    public List<DrugBatch> lowStock() {
        return drugBatchRepository.findLowStock();
    }

    @Transactional(value = "transactionManager", readOnly = true)
    public List<DrugBatch> expiringWithinDays(int days) {
        return drugBatchRepository.findExpiringBy(LocalDate.now().plusDays(days));
    }
}
