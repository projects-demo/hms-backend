package com.hms.appointment.dto;

import com.hms.appointment.entity.AppointmentSlot;

import java.time.LocalDate;
import java.time.LocalTime;

public record SlotResponse(Long id, Long doctorId, LocalDate slotDate, LocalTime startTime, LocalTime endTime, String status) {
    public static SlotResponse from(AppointmentSlot s) {
        return new SlotResponse(s.getId(), s.getDoctorId(), s.getSlotDate(), s.getStartTime(), s.getEndTime(), s.getStatus().name());
    }
}
