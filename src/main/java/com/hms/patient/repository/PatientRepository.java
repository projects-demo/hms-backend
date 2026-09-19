package com.hms.patient.repository;

import com.hms.common.dto.OptionDto;
import com.hms.patient.entity.Patient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PatientRepository extends JpaRepository<Patient, Long> {

    Optional<Patient> findByPatientCode(String patientCode);

    boolean existsByPatientCode(String patientCode);

    @Query("""
           select p from Patient p
           where lower(p.firstName) like lower(concat('%', :q, '%'))
              or lower(p.lastName) like lower(concat('%', :q, '%'))
              or p.primaryPhone like concat('%', :q, '%')
              or p.patientCode like concat('%', :q, '%')
           """)
    Page<Patient> search(@Param("q") String q, Pageable pageable);

    // Lightweight typeahead used by reception/billing/OPD screens - id+label only,
    // capped by the caller's Pageable (see PatientService.autocomplete).
    @Query("""
           select new com.hms.common.dto.OptionDto(p.id, concat(p.firstName, ' ', p.lastName), p.primaryPhone)
           from Patient p
           where lower(p.firstName) like lower(concat(:q, '%'))
              or lower(p.lastName) like lower(concat(:q, '%'))
              or p.primaryPhone like concat(:q, '%')
              or p.patientCode like concat(:q, '%')
           """)
    List<OptionDto> autocomplete(@Param("q") String q, Pageable pageable);

    @Query("select p.gender as gender, count(p) as patientCount from Patient p group by p.gender")
    List<GenderRow> genderBreakdown();

    // Age computed from dob where available, falling back to the stored age_years snapshot -
    // bucketed in SQL since JPQL has no clean date-arithmetic-then-CASE equivalent across DBs.
    @Query(value = """
           select
             case
               when age < 18 then '0-17'
               when age between 18 and 30 then '18-30'
               when age between 31 and 45 then '31-45'
               when age between 46 and 60 then '46-60'
               else '61+'
             end as ageGroup,
             count(*) as patientCount
           from (
             select coalesce(TIMESTAMPDIFF(YEAR, dob, CURDATE()), age_years, 0) as age
             from patients
           ) ages
           group by ageGroup
           """, nativeQuery = true)
    List<AgeGroupRow> ageGroupBreakdown();

    interface GenderRow {
        com.hms.patient.entity.Gender getGender();
        Long getPatientCount();
    }

    interface AgeGroupRow {
        String getAgeGroup();
        Long getPatientCount();
    }
}