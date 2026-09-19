package com.hms.ipd.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "wards")
public class Ward {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "ward_type", nullable = false, length = 20)
    private WardType wardType;

    @Column(length = 20)
    private String floor;

    @Column(name = "total_beds", nullable = false)
    private int totalBeds;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;
}
