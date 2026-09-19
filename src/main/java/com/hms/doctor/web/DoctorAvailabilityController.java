package com.hms.doctor.web;

import com.hms.common.dto.ApiResponse;
import com.hms.common.exception.ResourceNotFoundException;
import com.hms.doctor.dto.AvailabilitySlotRequest;
import com.hms.doctor.entity.DoctorAvailability;
import com.hms.doctor.repository.DoctorAvailabilityRepository;
import com.hms.doctor.repository.DoctorRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/doctors/{doctorId}/availability")
@RequiredArgsConstructor
@Tag(name = "Doctors - Weekly Availability", description = "Template used by /appointment-slots/generate to materialize bookable slots")
public class DoctorAvailabilityController {

    private final DoctorAvailabilityRepository availabilityRepository;
    private final DoctorRepository doctorRepository;

    public record AvailabilityResponse(Long id, short dayOfWeek, String startTime, String endTime, int slotDurationMins) {
        static AvailabilityResponse from(DoctorAvailability a) {
            return new AvailabilityResponse(a.getId(), a.getDayOfWeek(), a.getStartTime().toString(), a.getEndTime().toString(), a.getSlotDurationMins());
        }
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('HOSPITAL_ADMIN','DOCTOR')")
    @Operation(summary = "Add a weekly availability window")
    @Transactional("transactionManager")
    public ApiResponse<AvailabilityResponse> add(@PathVariable Long doctorId, @Valid @RequestBody AvailabilitySlotRequest request) {
        doctorRepository.findById(doctorId).orElseThrow(() -> ResourceNotFoundException.of("Doctor", doctorId));
        DoctorAvailability a = new DoctorAvailability();
        a.setDoctorId(doctorId);
        a.setDayOfWeek(request.dayOfWeek());
        a.setStartTime(request.startTime());
        a.setEndTime(request.endTime());
        a.setSlotDurationMins(request.slotDurationMins() == 0 ? 15 : request.slotDurationMins());
        return ApiResponse.ok(AvailabilityResponse.from(availabilityRepository.save(a)));
    }

    @GetMapping
    @Operation(summary = "List a doctor's weekly availability windows")
    @Transactional(value = "transactionManager", readOnly = true)
    public ApiResponse<List<AvailabilityResponse>> list(@PathVariable Long doctorId) {
        return ApiResponse.ok(availabilityRepository.findByDoctorIdAndActiveTrue(doctorId).stream().map(AvailabilityResponse::from).toList());
    }
}
