package com.hms.appointment.web;

import com.hms.appointment.dto.*;
import com.hms.appointment.entity.AppointmentStatus;
import com.hms.appointment.service.AppointmentService;
import com.hms.common.PageRequestFactory;
import com.hms.common.dto.ApiResponse;
import com.hms.common.dto.PageResponse;
import com.hms.security.jwt.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/appointments")
@RequiredArgsConstructor
@Tag(name = "Appointments")
public class AppointmentController {

    private final AppointmentService appointmentService;
    private final PageRequestFactory pageRequestFactory;

    @PostMapping
    @Operation(summary = "Book an appointment")
    public ApiResponse<AppointmentResponse> book(@Valid @RequestBody BookAppointmentRequest request,
                                                  @AuthenticationPrincipal AuthenticatedUser actor) {
        return ApiResponse.ok("Appointment booked", AppointmentResponse.from(appointmentService.book(request, actor.userId())));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get appointment by id")
    public ApiResponse<AppointmentResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(AppointmentResponse.from(appointmentService.getOrThrow(id)));
    }

    @GetMapping
    @Operation(summary = "Search/list appointments (paginated)")
    public ApiResponse<PageResponse<AppointmentResponse>> search(
            @RequestParam(required = false) Long doctorId,
            @RequestParam(required = false) Long patientId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) AppointmentStatus status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        var pageable = pageRequestFactory.of(page, size, "appointmentDate", "desc");
        var result = appointmentService.search(doctorId, patientId, date, status, pageable);
        return ApiResponse.ok(PageResponse.of(result.map(AppointmentResponse::from)));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Transition appointment status", description = "SCHEDULED -> CHECKED_IN -> IN_CONSULTATION -> COMPLETED, or CANCELLED/NO_SHOW")
    public ApiResponse<AppointmentResponse> changeStatus(@PathVariable Long id, @RequestParam AppointmentStatus status) {
        return ApiResponse.ok(AppointmentResponse.from(appointmentService.changeStatus(id, status)));
    }

    @PostMapping("/{id}/payments")
    @Operation(summary = "Record a payment against an appointment fee")
    public ApiResponse<Void> recordPayment(@PathVariable Long id, @Valid @RequestBody RecordPaymentRequest request,
                                            @AuthenticationPrincipal AuthenticatedUser actor) {
        appointmentService.recordPayment(id, request.amount(), request.paymentMode(), actor.userId());
        return ApiResponse.ok("Payment recorded", null);
    }
}
