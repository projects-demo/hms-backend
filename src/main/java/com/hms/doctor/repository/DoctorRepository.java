package com.hms.doctor.repository;

import com.hms.common.dto.OptionDto;
import com.hms.doctor.entity.Doctor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DoctorRepository extends JpaRepository<Doctor, Long> {

    Optional<Doctor> findByDoctorCode(String doctorCode);

    Optional<Doctor> findByUserId(Long userId);

    @Query("""
           select d from Doctor d join fetch d.department
           where (:departmentId is null or d.department.id = :departmentId)
             and (:specialization is null or lower(d.specialization) = lower(:specialization))
             and (:q is null or lower(d.specialization) like lower(concat('%', :q, '%'))
                  or lower(d.doctorCode) like lower(concat('%', :q, '%'))
                  or d.userId in (select u.id from AppUser u where lower(u.fullName) like lower(concat('%', :q, '%'))))
           """)
    Page<Doctor> search(@Param("departmentId") Long departmentId,
                         @Param("specialization") String specialization,
                         @Param("q") String q,
                         Pageable pageable);

    @Query("""
           select new com.hms.common.dto.OptionDto(d.id, d.specialization, d.doctorCode)
           from Doctor d
           where d.active = true and lower(d.specialization) like lower(concat(:q, '%'))
           """)
    List<OptionDto> autocompleteBySpecialization(@Param("q") String q, Pageable pageable);

    // Doctor's display name lives on AppUser (identity module), not Doctor itself -
    // native join keeps this a single round trip instead of N+1 lookups.
    @Query(value = """
           select d.id as id, u.full_name as label, d.specialization as sub_label
           from doctors d join app_users u on d.user_id = u.id
           where d.is_active = true and lower(u.full_name) like lower(concat(:q, '%'))
           order by u.full_name limit :limit
           """, nativeQuery = true)
    List<DoctorNameRow> autocompleteByName(@Param("q") String q, @Param("limit") int limit);

    interface DoctorNameRow {
        Long getId();
        String getLabel();
        String getSubLabel();
    }
}
