package com.hms.patient.entity;

import com.hms.common.AuditableEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "patients")
public class Patient extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "patient_code", nullable = false, unique = true, length = 20)
    private String patientCode;

    @Column(name = "first_name", nullable = false, length = 50)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 50)
    private String lastName;

    private LocalDate dob;

    @Column(name = "age_years")
    private Short ageYears;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Gender gender;

    @Column(name = "primary_phone", nullable = false, length = 15)
    private String primaryPhone;

    @Column(name = "secondary_phone", length = 15)
    private String secondaryPhone;

    @Column(length = 150)
    private String email;

    @Column(name = "emergency_name", length = 100)
    private String emergencyName;

    @Column(name = "emergency_contact", length = 15)
    private String emergencyContact;

    @Enumerated(EnumType.STRING)
    @Column(name = "government_id_type", length = 20)
    private GovernmentIdType governmentIdType;

    @Column(name = "government_id_number", length = 100)
    private String governmentIdNumber;

    @Column(name = "insurance_provider", length = 100)
    private String insuranceProvider;

    @Column(name = "policy_number", length = 100)
    private String policyNumber;

    @Column(name = "group_id", length = 100)
    private String groupId;

    @Column(length = 255)
    private String street;
    @Column(length = 100)
    private String city;
    @Column(length = 100)
    private String state;
    @Column(length = 100)
    private String country;
    @Column(name = "zip_code", length = 20)
    private String zipCode;

    @Column(name = "photo_url", length = 500)
    private String photoUrl;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "updated_by")
    private Long updatedBy;

    public Integer getAge() {
        if (dob != null) {
            return LocalDate.now().getYear() - dob.getYear() -
                    (LocalDate.now().getDayOfYear() < dob.getDayOfYear() ? 1 : 0);
        }
        return ageYears != null ? ageYears.intValue() : null;
    }
}
