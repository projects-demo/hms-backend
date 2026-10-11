package com.hms.billing.service;

import com.hms.billing.dto.CreateBillRequest;
import com.hms.billing.entity.*;
import com.hms.billing.repository.BillReceiptRepository;
import com.hms.billing.repository.BillRefundRepository;
import com.hms.billing.repository.BillRepository;
import com.hms.billing.repository.ChargeMasterRepository;
import com.hms.billing.repository.DiscountMasterRepository;
import com.hms.appointment.entity.Appointment;
import com.hms.appointment.repository.AppointmentRepository;
import com.hms.common.codeseq.CodeGeneratorService;
import com.hms.common.exception.BusinessRuleException;
import com.hms.common.exception.ResourceNotFoundException;
import com.hms.patient.repository.PatientRepository;
import com.hms.security.jwt.AuthenticatedUser;
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
    private final ChargeMasterRepository chargeMasterRepository;
    private final DiscountMasterRepository discountMasterRepository;
    private final BillReceiptRepository receiptRepository;
    private final BillRefundRepository refundRepository;
    private final PatientRepository patientRepository;
    private final AppointmentRepository appointmentRepository;
    private final CodeGeneratorService codeGeneratorService;
    private final BillingAuditService billingAuditService;

    public Bill create(CreateBillRequest r, AuthenticatedUser actor) {
        patientRepository.findById(r.patientId()).orElseThrow(() -> ResourceNotFoundException.of("Patient", r.patientId()));
        if (r.appointmentId() != null) {
            Appointment appointment = appointmentRepository.findById(r.appointmentId())
                    .orElseThrow(() -> ResourceNotFoundException.of("Appointment", r.appointmentId()));
            if (!appointment.getPatientId().equals(r.patientId())) {
                throw new BusinessRuleException("The bill patient must match the appointment patient");
            }
            if (billRepository.existsByAppointmentIdAndStatusNot(r.appointmentId(), BillStatus.CANCELLED)) {
                throw new BusinessRuleException("An active bill already exists for this appointment");
            }
        }

        Bill bill = new Bill();
        bill.setBillNumber(codeGeneratorService.next("BILL", "BL", 7));
        bill.setPatientId(r.patientId());
        bill.setAdmissionId(r.admissionId());
        bill.setAppointmentId(r.appointmentId());
        bill.setBillType(r.billType());
        bill.setStatus(BillStatus.DRAFT);
        bill.setCreatedBy(actor.userId());

        for (var itemReq : r.items()) {
            BillItem item = new BillItem();
            var chargeMaster = itemReq.chargeMasterId() == null ? null : chargeMasterRepository.findById(itemReq.chargeMasterId())
                    .orElseThrow(() -> ResourceNotFoundException.of("ChargeMaster", itemReq.chargeMasterId()));
            if (chargeMaster != null && !chargeMaster.isActive()) {
                throw new BusinessRuleException("The selected standard billing item is inactive");
            }
            item.setChargeMasterId(chargeMaster == null ? null : chargeMaster.getId());
            item.setDescription(chargeMaster == null ? itemReq.description() : chargeMaster.getName());
            item.setQuantity(itemReq.quantity());
            BigDecimal unitPrice = chargeMaster == null ? itemReq.unitPrice() : chargeMaster.getDefaultPrice();
            item.setUnitPrice(unitPrice);
            item.setAmount(unitPrice.multiply(BigDecimal.valueOf(itemReq.quantity())));
            bill.addItem(item);
        }
        bill.recalculateTotals();
        bill = billRepository.save(bill);
        billingAuditService.record("BILL_CREATED", "BILL", bill.getId(), bill.getId(), actor, java.util.Map.of(
                "billNumber", bill.getBillNumber(), "patientId", bill.getPatientId(), "billType", bill.getBillType(),
                "subtotalAmount", bill.getSubtotalAmount(), "items", bill.getItems().stream().map(item -> java.util.Map.of(
                        "description", item.getDescription(), "quantity", item.getQuantity(),
                        "unitPrice", item.getUnitPrice(), "amount", item.getAmount())).toList()));
        return bill;
    }

    public Bill finalizeBill(Long id, Long discountMasterId, BigDecimal taxAmount, AuthenticatedUser actor) {
        Bill bill = getOrThrow(id);
        if (bill.getStatus() != BillStatus.DRAFT) {
            throw new BusinessRuleException("Only a DRAFT bill can be finalized (current status: " + bill.getStatus() + ")");
        }
        BigDecimal previousTotal = bill.getTotalAmount();
        BigDecimal previousDiscount = bill.getDiscountAmount();
        if (discountMasterId != null) {
            if (!"HOSPITAL_ADMIN".equals(actor.role())) {
                throw new BusinessRuleException("Only a hospital admin can apply a discount");
            }
            DiscountMaster discount = discountMasterRepository.findById(discountMasterId)
                    .orElseThrow(() -> ResourceNotFoundException.of("Discount", discountMasterId));
            if (!discount.isActive() || discount.getDiscountKind() != DiscountKind.PERCENT
                    || discount.getValue().compareTo(BigDecimal.ZERO) <= 0
                    || discount.getValue().compareTo(new BigDecimal("100")) > 0) {
                throw new BusinessRuleException("The selected percentage discount is not active or valid");
            }
            bill.setDiscountMasterId(discount.getId());
            bill.setDiscountPercentage(discount.getValue());
            bill.setDiscountAmount(bill.getSubtotalAmount().multiply(discount.getValue())
                    .divide(new BigDecimal("100"), 2, java.math.RoundingMode.HALF_UP));
        } else {
            bill.setDiscountMasterId(null);
            bill.setDiscountPercentage(null);
            bill.setDiscountAmount(BigDecimal.ZERO);
        }
        if (taxAmount != null) bill.setTaxAmount(taxAmount);
        bill.recalculateTotals();
        bill.setStatus(BillStatus.FINALIZED);
        bill.setFinalizedAt(java.time.LocalDateTime.now());
        bill = billRepository.save(bill);
        syncAppointmentPaidStatus(bill);
        billingAuditService.record("BILL_FINALIZED", "BILL", bill.getId(), bill.getId(), actor, java.util.Map.of(
                "previousTotal", previousTotal, "previousDiscount", previousDiscount,
                "discountMasterId", bill.getDiscountMasterId() == null ? "none" : bill.getDiscountMasterId(),
                "discountPercentage", bill.getDiscountPercentage() == null ? BigDecimal.ZERO : bill.getDiscountPercentage(),
                "discountAmount", bill.getDiscountAmount(), "taxAmount", bill.getTaxAmount(),
                "totalAmount", bill.getTotalAmount(), "status", bill.getStatus()));
        return bill;
    }

    public BillReceipt recordReceipt(Long billId, BigDecimal amount, PaymentMode mode, AuthenticatedUser actor) {
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
        receipt.setReceivedBy(actor.userId());
        receipt = receiptRepository.save(receipt);

        bill.setPaidAmount(bill.getPaidAmount().add(amount));
        bill.recalculateTotals();
        billRepository.save(bill);
        syncAppointmentPaidStatus(bill);
        billingAuditService.record("PAYMENT_RECEIVED", "BILL_RECEIPT", receipt.getId(), bill.getId(), actor,
            java.util.Map.of("receiptNumber", receipt.getReceiptNumber(), "amount", amount,
                "paymentMode", mode, "paidAmountAfter", bill.getPaidAmount(), "balanceAfter", bill.getBalanceAmount()));
        return receipt;
    }

        public BillRefund refund(Long billId, BigDecimal amount, String reason, AuthenticatedUser actor) {
        Bill bill = getOrThrow(billId);
        if (amount.compareTo(bill.getPaidAmount()) > 0) {
            throw new BusinessRuleException("Refund amount exceeds the amount already paid");
        }
        BillRefund refund = new BillRefund();
        refund.setBillId(billId);
        refund.setAmount(amount);
        refund.setReason(reason);
        refund.setRefundedBy(actor.userId());
        refund = refundRepository.save(refund);

        bill.setPaidAmount(bill.getPaidAmount().subtract(amount));
        bill.recalculateTotals();
        billRepository.save(bill);
        syncAppointmentPaidStatus(bill);
        billingAuditService.record("PAYMENT_REFUNDED", "BILL_REFUND", refund.getId(), bill.getId(), actor,
            java.util.Map.of("amount", amount, "reason", reason == null ? "" : reason,
                "paidAmountAfter", bill.getPaidAmount(), "balanceAfter", bill.getBalanceAmount()));
        return refund;
    }

    private void syncAppointmentPaidStatus(Bill bill) {
        if (bill.getAppointmentId() == null) return;
        appointmentRepository.findById(bill.getAppointmentId()).ifPresent(appointment -> {
            appointment.setPaid(bill.getBalanceAmount().compareTo(BigDecimal.ZERO) == 0);
            appointmentRepository.save(appointment);
        });
    }

    public Bill cancel(Long id, AuthenticatedUser actor) {
        Bill bill = getOrThrow(id);
        if (bill.getPaidAmount().compareTo(BigDecimal.ZERO) > 0) {
            throw new BusinessRuleException("Cannot cancel a bill that already has payments - issue a refund first");
        }
        bill.setStatus(BillStatus.CANCELLED);
        bill = billRepository.save(bill);
        billingAuditService.record("BILL_CANCELLED", "BILL", bill.getId(), bill.getId(), actor,
            java.util.Map.of("billNumber", bill.getBillNumber(), "status", bill.getStatus(),
                "paidAmount", bill.getPaidAmount(), "balanceAmount", bill.getBalanceAmount()));
        return bill;
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
