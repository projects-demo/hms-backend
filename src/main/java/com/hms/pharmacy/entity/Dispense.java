package com.hms.pharmacy.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "dispenses")
public class Dispense {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "dispense_code", nullable = false, unique = true, length = 20)
    private String dispenseCode;

    @Column(name = "prescription_id")
    private Long prescriptionId;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "dispensed_by")
    private Long dispensedBy;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "dispensed_at", nullable = false)
    private LocalDateTime dispensedAt = LocalDateTime.now();

    @OneToMany(mappedBy = "dispense", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DispenseItem> items = new ArrayList<>();

    public void addItem(DispenseItem item) {
        item.setDispense(this);
        items.add(item);
    }
}
