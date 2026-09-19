package com.hms.pharmacy.entity;

import com.hms.common.AuditableEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "drugs")
public class Drug extends AuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "drug_code", nullable = false, unique = true, length = 30)
    private String drugCode;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "generic_name", length = 200)
    private String genericName;

    @Column(length = 150)
    private String manufacturer;

    @Column(length = 100)
    private String category;

    @Column(nullable = false, length = 20)
    private String unit = "UNIT";

    @Column(name = "reorder_level", nullable = false)
    private int reorderLevel = 10;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;
}
