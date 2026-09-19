package com.hms.appointment.repository;

import com.hms.appointment.entity.AppointmentSlot;
import com.hms.appointment.entity.SlotStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AppointmentSlotRepository extends JpaRepository<AppointmentSlot, Long> {

    @Query("""
           select s from AppointmentSlot s
           where s.doctorId = :doctorId and s.slotDate = :date and s.status = :status
           order by s.startTime
           """)
    List<AppointmentSlot> findAvailable(@Param("doctorId") Long doctorId,
                                         @Param("date") LocalDate date,
                                         @Param("status") SlotStatus status);

    Optional<AppointmentSlot> findByDoctorIdAndSlotDateAndStartTime(Long doctorId, LocalDate date, java.time.LocalTime startTime);
}
