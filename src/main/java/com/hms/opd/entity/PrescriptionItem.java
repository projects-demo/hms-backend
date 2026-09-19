package com.hms.opd.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "prescription_items")
public class PrescriptionItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prescription_id", nullable = false)
    private Prescription prescription;

    @Column(name = "drug_name", nullable = false, length = 200)
    private String drugName;

    @Column(length = 100)
    private String dosage;
    @Column(length = 100)
    private String frequency;
    @Column(length = 100)
    private String duration;
    @Column(length = 500)
    private String instructions;
}
