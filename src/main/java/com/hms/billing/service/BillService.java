package com.hms.billing.service;

import com.hms.billing.dto.CreateBillRequest;
import com.hms.billing.dto.BillResponse;
import com.hms.billing.dto.PaymentReceiptResponse;
import com.hms.billing.entity.*;
import com.hms.billing.repository.BillReceiptRepository;
import com.hms.billing.repository.BillRefundRepository;
import com.hms.billing.repository.BillRepository;
import com.hms.billing.repository.ChargeMasterRepository;
import com.hms.billing.repository.DiscountMasterRepository;
import com.hms.identity.entity.AppUser;
import com.hms.identity.entity.Role;
import com.hms.identity.repository.AppUserRepository;
import com.hms.appointment.entity.Appointment;
import com.hms.appointment.repository.AppointmentRepository;
import com.hms.common.codeseq.CodeGeneratorService;
import com.hms.common.exception.BusinessRuleException;
import com.hms.common.exception.ResourceNotFoundException;
import com.hms.patient.repository.PatientRepository;
import com.hms.patient.entity.Patient;
import com.hms.doctor.repository.DoctorRepository;
import com.hms.security.jwt.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
    private final DoctorRepository doctorRepository;
    private final CodeGeneratorService codeGeneratorService;
    private final BillingAuditService billingAuditService;
    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;

    public Bill create(CreateBillRequest r, AuthenticatedUser actor) {
        Patient patient = patientRepository.findById(r.patientId()).orElseThrow(() -> ResourceNotFoundException.of("Patient", r.patientId()));
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
        bill.setComments(r.comments());
        bill.setCreatedBy(actor.userId());
        // Advisor selection (item 3)
        if (r.advisorType() != null) {
            bill.setAdvisorType(r.advisorType());
            bill.setAdvisorDoctorId(r.advisorDoctorId());
            bill.setExternalAdvisorName(r.externalAdvisorName());
        }

        addItems(bill, r.items());
        bill.recalculateTotals();
        bill = billRepository.save(bill);
        billingAuditService.record("BILL_CREATED", "BILL", bill.getId(), bill.getId(),
                patient.getFirstName() + " " + patient.getLastName(), patient.getPrimaryPhone(),
                actor, java.util.Map.of(
                "billNumber", bill.getBillNumber(), "patientId", bill.getPatientId(), "billType", bill.getBillType(),
                "subtotalAmount", bill.getSubtotalAmount(), "comments", bill.getComments() == null ? "" : bill.getComments(),
                "items", bill.getItems().stream().map(item -> java.util.Map.of(
                        "description", item.getDescription(), "quantity", item.getQuantity(),
                        "unitPrice", item.getUnitPrice(), "amount", item.getAmount())).toList()));
        return bill;
    }

    public Bill updateDraft(Long id, CreateBillRequest request, AuthenticatedUser actor) {
        Bill bill = getOrThrow(id);
        if (bill.getStatus() != BillStatus.DRAFT) {
            throw new BusinessRuleException("Only a DRAFT bill can be edited");
        }
        Patient patient = patientRepository.findById(request.patientId())
                .orElseThrow(() -> ResourceNotFoundException.of("Patient", request.patientId()));
        if (request.appointmentId() != null) {
            Appointment appointment = appointmentRepository.findById(request.appointmentId())
                    .orElseThrow(() -> ResourceNotFoundException.of("Appointment", request.appointmentId()));
            if (!appointment.getPatientId().equals(request.patientId())) {
                throw new BusinessRuleException("The bill patient must match the appointment patient");
            }
            if (billRepository.existsByAppointmentIdAndStatusNotAndIdNot(
                    request.appointmentId(), BillStatus.CANCELLED, id)) {
                throw new BusinessRuleException("An active bill already exists for this appointment");
            }
        }

        bill.setPatientId(request.patientId());
        bill.setAdmissionId(request.admissionId());
        bill.setAppointmentId(request.appointmentId());
        bill.setBillType(request.billType());
        bill.setComments(request.comments());
        bill.getItems().clear();
        addItems(bill, request.items());
        bill.recalculateTotals();
        bill = billRepository.save(bill);
        billingAuditService.record("BILL_UPDATED", "BILL", bill.getId(), bill.getId(),
                patient.getFirstName() + " " + patient.getLastName(), patient.getPrimaryPhone(),
                actor, java.util.Map.of(
                "billNumber", bill.getBillNumber(), "patientId", bill.getPatientId(),
                "subtotalAmount", bill.getSubtotalAmount(), "comments", bill.getComments() == null ? "" : bill.getComments(),
                "items", bill.getItems().stream().map(item -> java.util.Map.of(
                        "description", item.getDescription(), "quantity", item.getQuantity(),
                        "unitPrice", item.getUnitPrice(), "amount", item.getAmount())).toList()));
        return bill;
    }

    private void addItems(Bill bill, java.util.List<CreateBillRequest.BillItemRequest> itemRequests) {
        for (var itemReq : itemRequests) {
            BillItem item = new BillItem();
            var chargeMaster = itemReq.chargeMasterId() == null ? null : chargeMasterRepository.findById(itemReq.chargeMasterId())
                    .orElseThrow(() -> ResourceNotFoundException.of("ChargeMaster", itemReq.chargeMasterId()));
            if (chargeMaster != null && !chargeMaster.isActive()) {
                throw new BusinessRuleException("The selected standard billing item is inactive");
            }
            item.setChargeMasterId(chargeMaster == null ? null : chargeMaster.getId());
            item.setDescription(chargeMaster == null ? itemReq.description() : chargeMaster.getName());
            item.setQuantity(itemReq.quantity());
            // Use the caller-supplied unitPrice; fall back to the charge-master default only when
            // the caller explicitly sends 0 (i.e. they did not supply a price themselves).
            BigDecimal unitPrice = (itemReq.unitPrice() != null && itemReq.unitPrice().compareTo(BigDecimal.ZERO) > 0)
                    ? itemReq.unitPrice()
                    : (chargeMaster != null ? chargeMaster.getDefaultPrice() : BigDecimal.ZERO);
            item.setUnitPrice(unitPrice);
            item.setAmount(unitPrice.multiply(BigDecimal.valueOf(itemReq.quantity())));
            bill.addItem(item);
        }
    }

    public Bill finalizeBill(Long id, Long discountMasterId, BigDecimal taxAmount, PaymentMode paymentMode, AuthenticatedUser actor) {
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
        if (bill.getTotalAmount().compareTo(BigDecimal.ZERO) > 0) {
            if (paymentMode == null) throw new BusinessRuleException("Select a payment method to submit this bill");
            recordReceipt(bill.getId(), bill.getBalanceAmount(), paymentMode, actor);
            bill = billRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Bill", id));
        } else {
            syncAppointmentPaidStatus(bill);
        }
        billingAuditService.record("BILL_FINALIZED", "BILL", bill.getId(), bill.getId(), actor, java.util.Map.of(
                "previousTotal", previousTotal, "previousDiscount", previousDiscount,
                "discountMasterId", bill.getDiscountMasterId() == null ? "none" : bill.getDiscountMasterId(),
                "discountPercentage", bill.getDiscountPercentage() == null ? BigDecimal.ZERO : bill.getDiscountPercentage(),
                "discountAmount", bill.getDiscountAmount(), "taxAmount", bill.getTaxAmount(),
                "totalAmount", bill.getTotalAmount(), "paymentMode", paymentMode == null ? "NONE" : paymentMode.name(),
                "comments", bill.getComments() == null ? "" : bill.getComments(),
                "status", bill.getStatus()));
        return bill;
    }

    public PaymentReceiptResponse recordReceipt(Long billId, BigDecimal amount, PaymentMode mode, AuthenticatedUser actor) {
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
        Patient patient = patientRepository.findById(bill.getPatientId())
            .orElseThrow(() -> ResourceNotFoundException.of("Patient", bill.getPatientId()));
        billingAuditService.record("PAYMENT_RECEIVED", "BILL_RECEIPT", receipt.getId(), bill.getId(),
            patient.getFirstName() + " " + patient.getLastName(), patient.getPrimaryPhone(),
            actor, java.util.Map.of("receiptNumber", receipt.getReceiptNumber(), "amount", amount,
                "paymentMode", mode, "paidAmountAfter", bill.getPaidAmount(), "balanceAfter", bill.getBalanceAmount()));
        String appointmentCode = bill.getAppointmentId() == null ? null
            : appointmentRepository.findById(bill.getAppointmentId()).map(Appointment::getAppointmentCode).orElse(null);
        return PaymentReceiptResponse.from(receipt, bill, patient, appointmentCode);
    }

        @Transactional(value = "transactionManager", readOnly = true)
        public Page<PaymentReceiptResponse> patientReceiptHistory(Long patientId, Pageable pageable) {
        patientRepository.findById(patientId).orElseThrow(() -> ResourceNotFoundException.of("Patient", patientId));
        return receiptRepository.findPatientReceiptHistory(patientId, pageable)
            .map(row -> new PaymentReceiptResponse(row.getReceiptId(), row.getReceiptNumber(), row.getBillId(),
                row.getBillNumber(), row.getPatientId(), row.getPatientName(), row.getPatientCode(),
                        row.getPatientPhone(), row.getAppointmentId(), row.getAppointmentCode(), row.getAmount(), row.getPaymentMode(),
                row.getReceivedAt()));
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

    public Bill generateFreeVisitBill(Appointment appointment, AuthenticatedUser actor) {
        if (appointment == null || appointment.getId() == null) {
            return null;
        }
        if (billRepository.existsByAppointmentIdAndStatusNot(appointment.getId(), BillStatus.CANCELLED)) {
            return billRepository.findFirstByAppointmentIdAndStatusNot(appointment.getId(), BillStatus.CANCELLED).orElse(null);
        }

        Bill bill = new Bill();
        bill.setBillNumber(codeGeneratorService.next("BILL", "BL", 7));
        bill.setPatientId(appointment.getPatientId());
        bill.setAppointmentId(appointment.getId());
        bill.setBillType(BillType.OPD);
        bill.setStatus(BillStatus.FINALIZED);
        bill.setSubtotalAmount(BigDecimal.ZERO);
        bill.setDiscountAmount(BigDecimal.ZERO);
        bill.setTaxAmount(BigDecimal.ZERO);
        bill.setTotalAmount(BigDecimal.ZERO);
        bill.setPaidAmount(BigDecimal.ZERO);
        bill.setBalanceAmount(BigDecimal.ZERO);
        bill.setComments("free visit");
        bill.setCreatedBy(actor != null ? actor.userId() : appointment.getCreatedBy());
        bill.setFinalizedAt(java.time.LocalDateTime.now());

        BillItem item = new BillItem();
        item.setDescription("Consultation (Free Visit)");
        item.setQuantity(1);
        item.setUnitPrice(BigDecimal.ZERO);
        item.setAmount(BigDecimal.ZERO);
        bill.addItem(item);

        bill = billRepository.save(bill);

        AuthenticatedUser auditActor = actor;
        if (auditActor == null && appointment.getCreatedBy() != null) {
            auditActor = appUserRepository.findById(appointment.getCreatedBy())
                    .map(u -> new AuthenticatedUser(u.getId(), u.getUsername(), u.getFullName(), u.getRole().name(), null, null, false))
                    .orElse(null);
        }

        Patient patient = patientRepository.findById(appointment.getPatientId()).orElse(null);
        String patientName = patient != null ? patient.getFirstName() + " " + patient.getLastName() : null;
        String mobileNumber = patient != null ? patient.getPrimaryPhone() : null;

        if (auditActor != null) {
            billingAuditService.record("BILL_CREATED", "BILL", bill.getId(), bill.getId(),
                    patientName, mobileNumber, auditActor, java.util.Map.of(
                    "billNumber", bill.getBillNumber(),
                    "patientId", bill.getPatientId(),
                    "appointmentId", bill.getAppointmentId(),
                    "billType", bill.getBillType(),
                    "subtotalAmount", bill.getSubtotalAmount(),
                    "comments", bill.getComments(),
                    "items", java.util.List.of(java.util.Map.of(
                            "description", item.getDescription(),
                            "quantity", item.getQuantity(),
                            "unitPrice", item.getUnitPrice(),
                            "amount", item.getAmount()
                    ))
            ));
            billingAuditService.record("BILL_FINALIZED", "BILL", bill.getId(), bill.getId(),
                    patientName, mobileNumber, auditActor, java.util.Map.of(
                    "previousTotal", BigDecimal.ZERO,
                    "previousDiscount", BigDecimal.ZERO,
                    "discountMasterId", "none",
                    "discountPercentage", BigDecimal.ZERO,
                    "discountAmount", BigDecimal.ZERO,
                    "taxAmount", BigDecimal.ZERO,
                    "totalAmount", BigDecimal.ZERO,
                    "paymentMode", "FREE_VISIT",
                    "status", bill.getStatus(),
                    "comments", bill.getComments()
            ));
        }

        syncAppointmentPaidStatus(bill);
        return bill;
    }

    public Bill voidBill(Long id, String password, AuthenticatedUser actor) {
        if (!"HOSPITAL_ADMIN".equals(actor.role())) {
            throw new BusinessRuleException("Only a hospital admin can void a bill");
        }
        AppUser admin = appUserRepository.findById(actor.userId())
                .filter(AppUser::isActive)
                .filter(user -> user.getRole() == Role.HOSPITAL_ADMIN)
                .orElseThrow(() -> new BusinessRuleException("Admin authentication failed"));
        if (!passwordEncoder.matches(password, admin.getPasswordHash())) {
            throw new BusinessRuleException("Admin password is incorrect");
        }

        Bill bill = getOrThrow(id);
        if (bill.getStatus() == BillStatus.CANCELLED) {
            throw new BusinessRuleException("This bill is already voided");
        }
        bill.setStatus(BillStatus.CANCELLED);
        bill = billRepository.save(bill);
        if (bill.getAppointmentId() != null) {
            appointmentRepository.findById(bill.getAppointmentId()).ifPresent(appointment -> {
                appointment.setPaid(false);
                appointmentRepository.save(appointment);
            });
        }
        billingAuditService.record("BILL_VOIDED", "BILL", bill.getId(), bill.getId(), actor,
            java.util.Map.of("billNumber", bill.getBillNumber(), "status", bill.getStatus(),
                "paidAmount", bill.getPaidAmount(), "balanceAmount", bill.getBalanceAmount(),
                "receiptsRetained", true, "voidedBy", actor.userId()));
        return bill;
    }

    @Transactional(value = "transactionManager", readOnly = true)
    public Bill getOrThrow(Long id) {
        return billRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Bill", id));
    }

        @Transactional(value = "transactionManager", readOnly = true)
        public BillResponse response(Bill bill) {
        Patient patient = patientRepository.findById(bill.getPatientId())
            .orElseThrow(() -> ResourceNotFoundException.of("Patient", bill.getPatientId()));
        Appointment appointment = bill.getAppointmentId() == null ? null
            : appointmentRepository.findById(bill.getAppointmentId()).orElse(null);
        String doctorName = appointment == null ? null : doctorRepository.findDisplayNamesByIds(Set.of(appointment.getDoctorId())).stream()
            .findFirst().map(DoctorRepository.DoctorDisplayRow::getFullName).orElse(null);
        return BillResponse.from(bill, patient.getFirstName() + " " + patient.getLastName(),
            patient.getPatientCode(), patient.getPrimaryPhone(),
            appointment == null ? null : appointment.getAppointmentCode(), doctorName);
        }

    @Transactional(value = "transactionManager", readOnly = true)
        public Page<BillResponse> search(Long patientId, String query, BillStatus status, Pageable pageable) {
        String normalizedQuery = query == null || query.isBlank() ? null : query.trim();
        Page<Bill> bills = billRepository.search(patientId, status, normalizedQuery, pageable);
        if (bills.isEmpty()) return bills.map(BillResponse::from);

        Set<Long> patientIds = bills.stream().map(Bill::getPatientId).collect(Collectors.toSet());
        Map<Long, PatientRepository.PatientBillingRow> patientRows = patientRepository.findBillingDetailsByIds(patientIds).stream()
            .collect(Collectors.toMap(PatientRepository.PatientBillingRow::getId, row -> row));
        List<Long> appointmentIds = bills.stream().map(Bill::getAppointmentId).filter(java.util.Objects::nonNull).distinct().toList();
        Map<Long, Long> doctorIdsByAppointment = appointmentIds.isEmpty() ? Map.of()
            : appointmentRepository.findDoctorIdsByAppointmentIds(appointmentIds).stream()
                .collect(Collectors.toMap(AppointmentRepository.AppointmentDoctorRow::getId,
                    AppointmentRepository.AppointmentDoctorRow::getDoctorId));
            Map<Long, String> appointmentCodes = appointmentIds.isEmpty() ? Map.of()
                : appointmentRepository.findDoctorIdsByAppointmentIds(appointmentIds).stream()
                    .collect(Collectors.toMap(AppointmentRepository.AppointmentDoctorRow::getId,
                        AppointmentRepository.AppointmentDoctorRow::getAppointmentCode));
        Set<Long> doctorIds = Set.copyOf(doctorIdsByAppointment.values());
        Map<Long, String> doctorNames = doctorIds.isEmpty() ? Map.of()
            : doctorRepository.findDisplayNamesByIds(doctorIds).stream()
                .collect(Collectors.toMap(DoctorRepository.DoctorDisplayRow::getId,
                    DoctorRepository.DoctorDisplayRow::getFullName));

        return bills.map(bill -> {
            PatientRepository.PatientBillingRow patient = patientRows.get(bill.getPatientId());
            String doctorName = bill.getAppointmentId() == null ? null
                : doctorNames.get(doctorIdsByAppointment.get(bill.getAppointmentId()));
            return BillResponse.from(bill, patient == null ? null : patient.getFullName(),
                patient == null ? null : patient.getPatientCode(),
                    patient == null ? null : patient.getPrimaryPhone(),
                    appointmentCodes.get(bill.getAppointmentId()), doctorName);
        });
    }
}
