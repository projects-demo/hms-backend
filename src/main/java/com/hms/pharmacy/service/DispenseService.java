package com.hms.pharmacy.service;

import com.hms.common.codeseq.CodeGeneratorService;
import com.hms.common.exception.BusinessRuleException;
import com.hms.common.exception.ResourceNotFoundException;
import com.hms.pharmacy.dto.DispenseRequest;
import com.hms.pharmacy.entity.*;
import com.hms.pharmacy.repository.DispenseRepository;
import com.hms.pharmacy.repository.DrugBatchRepository;
import com.hms.pharmacy.repository.DrugRepository;
import com.hms.pharmacy.repository.StockTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Transactional("transactionManager")
public class DispenseService {

    private final DispenseRepository dispenseRepository;
    private final DrugRepository drugRepository;
    private final DrugBatchRepository drugBatchRepository;
    private final StockTransactionRepository stockTransactionRepository;
    private final CodeGeneratorService codeGeneratorService;

    /**
     * FEFO (first-expiry-first-out): consumes stock from the soonest-expiring
     * batches first, splitting across batches if one doesn't have enough.
     * @Version on DrugBatch prevents two concurrent dispenses from
     * over-drawing the same batch.
     */
    public Dispense dispense(DispenseRequest r, Long actorUserId) {
        Dispense dispense = new Dispense();
        dispense.setDispenseCode(codeGeneratorService.next("DISPENSE", "DSP", 6));
        dispense.setPatientId(r.patientId());
        dispense.setPrescriptionId(r.prescriptionId());
        dispense.setDispensedBy(actorUserId);

        BigDecimal grandTotal = BigDecimal.ZERO;

        for (var itemReq : r.items()) {
            drugRepository.findById(itemReq.drugId())
                    .orElseThrow(() -> ResourceNotFoundException.of("Drug", itemReq.drugId()));

            int remaining = itemReq.quantity();
            var batches = drugBatchRepository.findDispensableBatches(itemReq.drugId());
            if (batches.isEmpty()) {
                throw new BusinessRuleException("No stock available for drug id " + itemReq.drugId());
            }

            for (DrugBatch batch : batches) {
                if (remaining <= 0) break;
                int take = Math.min(remaining, batch.getQuantityAvailable());
                if (take <= 0) continue;

                batch.setQuantityAvailable(batch.getQuantityAvailable() - take);
                drugBatchRepository.save(batch);

                BigDecimal lineAmount = batch.getSellingPrice().multiply(BigDecimal.valueOf(take));
                DispenseItem item = new DispenseItem();
                item.setDrugId(itemReq.drugId());
                item.setBatchId(batch.getId());
                item.setQuantity(take);
                item.setUnitPrice(batch.getSellingPrice());
                item.setAmount(lineAmount);
                dispense.addItem(item);
                grandTotal = grandTotal.add(lineAmount);

                StockTransaction txn = new StockTransaction();
                txn.setDrugId(itemReq.drugId());
                txn.setBatchId(batch.getId());
                txn.setTxnType(TxnType.OUT);
                txn.setQuantity(take);
                txn.setReferenceType("DISPENSE");
                txn.setPerformedBy(actorUserId);
                stockTransactionRepository.save(txn);

                remaining -= take;
            }
            if (remaining > 0) {
                throw new BusinessRuleException("Insufficient stock for drug id " + itemReq.drugId() +
                        " - short by " + remaining + " units across all batches");
            }
        }

        dispense.setTotalAmount(grandTotal);
        return dispenseRepository.save(dispense);
    }

    @Transactional(value = "transactionManager", readOnly = true)
    public Page<Dispense> byPatient(Long patientId, Pageable pageable) {
        return dispenseRepository.findByPatientId(patientId, pageable);
    }

    @Transactional(value = "transactionManager", readOnly = true)
    public Dispense getOrThrow(Long id) {
        return dispenseRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Dispense", id));
    }
}
