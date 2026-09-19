package com.hms.billing.service;

import com.hms.billing.dto.CreateBillRequest;
import com.hms.billing.entity.*;
import com.hms.billing.repository.BillReceiptRepository;
import com.hms.billing.repository.BillRefundRepository;
import com.hms.billing.repository.BillRepository;
import com.hms.common.codeseq.CodeGeneratorService;
import com.hms.common.exception.BusinessRuleException;
import com.hms.common.exception.ResourceNotFoundException;
import com.hms.patient.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Transactional("transactionManager")
public class BillService {

    private final BillRepository billRepository;
    private final BillReceiptRepository receiptRepository;
    private final BillRefundRepository refundRepository;
    private final PatientRepository patientRepository;
    private final CodeGeneratorService codeGeneratorService;

    public Bill create(CreateBillRequest r) {
        patientRepository.findById(r.patientId()).orElseThrow(() -> ResourceNotFoundException.of("Patient", r.patientId()));

        Bill bill = new Bill();
        bill.setBillNumber(codeGeneratorService.next("BILL", "BL", 7));
        bill.setPatientId(r.patientId());
        bill.setAdmissionId(r.admissionId());
        bill.setAppointmentId(r.appointmentId());
        bill.setBillType(r.billType());
        bill.setStatus(BillStatus.DRAFT);

        for (var itemReq : r.items()) {
            BillItem item = new BillItem();
            item.setChargeMasterId(itemReq.chargeMasterId());
            item.setDescription(itemReq.description());
            item.setQuantity(itemReq.quantity());
            item.setUnitPrice(itemReq.unitPrice());
            item.setAmount(itemReq.unitPrice().multiply(BigDecimal.valueOf(itemReq.quantity())));
            bill.addItem(item);
        }
        bill.recalculateTotals();
        return billRepository.save(bill);
    }

    public Bill finalizeBill(Long id, BigDecimal discountAmount, BigDecimal taxAmount) {
        Bill bill = getOrThrow(id);
        if (bill.getStatus() != BillStatus.DRAFT) {
            throw new BusinessRuleException("Only a DRAFT bill can be finalized (current status: " + bill.getStatus() + ")");
        }
        if (discountAmount != null) bill.setDiscountAmount(discountAmount);
        if (taxAmount != null) bill.setTaxAmount(taxAmount);
        bill.recalculateTotals();
        bill.setStatus(BillStatus.FINALIZED);
        bill.setFinalizedAt(java.time.LocalDateTime.now());
        return billRepository.save(bill);
    }

    public BillReceipt recordReceipt(Long billId, BigDecimal amount, PaymentMode mode, Long actorUserId) {
        Bill bill = getOrThrow(billId);
        if (bill.getStatus() != BillStatus.FINALIZED) {
            throw new BusinessRuleException("Payments can only be recorded against a FINALIZED bill");
        }
        if (amount.compareTo(bill.getBalanceAmount()) > 0) {
            throw new BusinessRuleException("Payment amount exceeds the outstanding balance of " + bill.getBalanceAmount());
        }
        BillReceipt receipt = new BillReceipt();
        receipt.setBillId(billId);
        receipt.setReceiptNumber(codeGeneratorService.next("RECEIPT", "RCP", 7));
        receipt.setAmount(amount);
        receipt.setPaymentMode(mode);
        receipt.setReceivedBy(actorUserId);
        receipt = receiptRepository.save(receipt);

        bill.setPaidAmount(bill.getPaidAmount().add(amount));
        bill.recalculateTotals();
        billRepository.save(bill);
        return receipt;
    }

    public BillRefund refund(Long billId, BigDecimal amount, String reason, Long actorUserId) {
        Bill bill = getOrThrow(billId);
        if (amount.compareTo(bill.getPaidAmount()) > 0) {
            throw new BusinessRuleException("Refund amount exceeds the amount already paid");
        }
        BillRefund refund = new BillRefund();
        refund.setBillId(billId);
        refund.setAmount(amount);
        refund.setReason(reason);
        refund.setRefundedBy(actorUserId);
        refund = refundRepository.save(refund);

        bill.setPaidAmount(bill.getPaidAmount().subtract(amount));
        bill.recalculateTotals();
        billRepository.save(bill);
        return refund;
    }

    public Bill cancel(Long id) {
        Bill bill = getOrThrow(id);
        if (bill.getPaidAmount().compareTo(BigDecimal.ZERO) > 0) {
            throw new BusinessRuleException("Cannot cancel a bill that already has payments - issue a refund first");
        }
        bill.setStatus(BillStatus.CANCELLED);
        return billRepository.save(bill);
    }

    @Transactional(value = "transactionManager", readOnly = true)
    public Bill getOrThrow(Long id) {
        return billRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Bill", id));
    }

    @Transactional(value = "transactionManager", readOnly = true)
    public Page<Bill> search(Long patientId, BillStatus status, Pageable pageable) {
        return billRepository.search(patientId, status, pageable);
    }
}
