package com.hms.appointment.repository;

import com.hms.appointment.entity.AppointmentPayment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AppointmentPaymentRepository extends JpaRepository<AppointmentPayment, Long> {
    List<AppointmentPayment> findByAppointmentId(Long appointmentId);
}
