package com.hms.appointment.repository;

import com.hms.appointment.entity.Appointment;
import com.hms.appointment.entity.AppointmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    Optional<Appointment> findByAppointmentCode(String code);

       @Query("select a.id as id, a.doctorId as doctorId, a.appointmentCode as appointmentCode from Appointment a where a.id in :appointmentIds")
       List<AppointmentDoctorRow> findDoctorIdsByAppointmentIds(@Param("appointmentIds") Collection<Long> appointmentIds);

       interface AppointmentDoctorRow {
              Long getId();
              Long getDoctorId();
              String getAppointmentCode();
       }

    @Query("""
           select a from Appointment a
           where (:doctorId is null or a.doctorId = :doctorId)
             and (:patientId is null or a.patientId = :patientId)
             and (:date is null or a.appointmentDate = :date)
             and (:status is null or a.status = :status)
           """)
    Page<Appointment> search(@Param("doctorId") Long doctorId,
                              @Param("patientId") Long patientId,
                              @Param("date") LocalDate date,
                              @Param("status") AppointmentStatus status,
                              Pageable pageable);

    long countByDoctorIdAndAppointmentDate(Long doctorId, LocalDate date);

    long countByAppointmentDate(LocalDate date);

    long countByAppointmentDateAndStatus(LocalDate date, AppointmentStatus status);

    @Query(value = """
           select a.id as appointmentId,
                        case when a.status = 'CANCELLED' then null else
                      1 + (select count(*)
                           from appointments earlier
                                     left join appointment_slots earlier_slot on earlier.slot_id = earlier_slot.id
                           where earlier.doctor_id = a.doctor_id
                             and earlier.appointment_date = a.appointment_date
                             and earlier.status <> 'CANCELLED'
                                       and ((current_slot.start_time is not null
                                                 and earlier_slot.start_time is not null
                                                 and (earlier_slot.start_time < current_slot.start_time
                                                        or (earlier_slot.start_time = current_slot.start_time and earlier.id < a.id)))
                                              or (current_slot.start_time is null
                                                    and (earlier_slot.id is not null
                                                           or (earlier_slot.id is null and earlier.id < a.id)))))
                  end as queueNumber
           from appointments a
           left join appointment_slots current_slot on a.slot_id = current_slot.id
           where a.id in (:appointmentIds)
           """, nativeQuery = true)
    List<QueueNumberRow> findQueueNumbers(@Param("appointmentIds") Collection<Long> appointmentIds);

    // --- Analytics / reporting queries below ---
    // Native SQL because these join across doctors/app_users/departments, which
    // Appointment has no mapped JPA relationship to (it only stores plain FK ids) -
    // a JPQL join isn't possible here, so we go straight to SQL for these reports.

    @Query(value = """
           select d.id as doctorId, u.full_name as doctorName, dept.name as departmentName,
                  count(*) as visitCount,
                  coalesce(sum(case when a.is_paid = true then a.fee_amount else 0 end), 0) as revenue
           from appointments a
           join doctors d on a.doctor_id = d.id
           join app_users u on d.user_id = u.id
           join departments dept on d.department_id = dept.id
           where a.appointment_date between :from and :to
           group by d.id, u.full_name, dept.name
           order by visitCount desc
           """, nativeQuery = true)
    List<DoctorPerformanceRow> doctorPerformance(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query(value = """
           select dept.id as departmentId, dept.name as departmentName,
                  count(*) as visitCount,
                  coalesce(sum(case when a.is_paid = true then a.fee_amount else 0 end), 0) as revenue
           from appointments a
           join doctors d on a.doctor_id = d.id
           join departments dept on d.department_id = dept.id
           where a.appointment_date between :from and :to
           group by dept.id, dept.name
           order by visitCount desc
           """, nativeQuery = true)
    List<DepartmentPerformanceRow> departmentPerformance(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query(value = """
           select a.appointment_date as visitDate,
                  count(*) as total,
                  sum(case when a.appointment_type = 'NEW' then 1 else 0 end) as newCount,
                  sum(case when a.appointment_type = 'FOLLOWUP' then 1 else 0 end) as followupCount
           from appointments a
           where a.appointment_date between :from and :to
           group by a.appointment_date
           order by a.appointment_date
           """, nativeQuery = true)
    List<DailyVisitRow> opdTrend(@Param("from") LocalDate from, @Param("to") LocalDate to);

    interface DoctorPerformanceRow {
        Long getDoctorId();
        String getDoctorName();
        String getDepartmentName();
        Long getVisitCount();
        java.math.BigDecimal getRevenue();
    }

    interface DepartmentPerformanceRow {
        Long getDepartmentId();
        String getDepartmentName();
        Long getVisitCount();
        java.math.BigDecimal getRevenue();
    }

    interface DailyVisitRow {
        java.sql.Date getVisitDate();
        Long getTotal();
        Long getNewCount();
        Long getFollowupCount();
    }

       interface QueueNumberRow {
              Long getAppointmentId();
              Long getQueueNumber();
       }
}