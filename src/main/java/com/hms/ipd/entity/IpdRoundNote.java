package com.hms.ipd.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "ipd_round_notes")
public class IpdRoundNote {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "admission_id", nullable = false)
    private Long admissionId;

    @Column(name = "recorded_by")
    private Long recordedBy;

    @Column(name = "recorded_at", nullable = false)
    private LocalDateTime recordedAt = LocalDateTime.now();

    @Column(length = 20)
    private String bp;
    @Column(length = 10)
    private String pulse;
    private String temperature;
    @Column(length = 10)
    private String spo2;

    @Column(columnDefinition = "TEXT")
    private String notes;
}
