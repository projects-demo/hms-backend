package com.hms.appointment.web;

import com.hms.appointment.dto.SlotGenerateRequest;
import com.hms.appointment.dto.SlotResponse;
import com.hms.appointment.service.AppointmentService;
import com.hms.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/appointment-slots")
@RequiredArgsConstructor
@Tag(name = "Appointment Slots")
public class SlotController {

    private final AppointmentService appointmentService;

    @PostMapping("/generate")
    @PreAuthorize("hasAnyRole('HOSPITAL_ADMIN','RECEPTIONIST')")
    @Operation(summary = "Generate bookable slots for a doctor/date",
            description = "Materializes slots from the doctor's weekly DoctorAvailability template. Safe to call repeatedly (idempotent).")
    public ApiResponse<List<SlotResponse>> generate(@Valid @RequestBody SlotGenerateRequest request) {
        var slots = appointmentService.generateSlotsForDate(request.doctorId(), request.date());
        return ApiResponse.ok(slots.stream().map(SlotResponse::from).toList());
    }

    @GetMapping
    @Operation(summary = "List available slots for a doctor on a date")
    public ApiResponse<List<SlotResponse>> available(
            @RequestParam Long doctorId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        var slots = appointmentService.availableSlots(doctorId, date);
        return ApiResponse.ok(slots.stream().map(SlotResponse::from).toList());
    }
}
