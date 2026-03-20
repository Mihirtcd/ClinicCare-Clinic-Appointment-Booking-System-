package com.tus.cliniccare.controller;

import com.tus.cliniccare.dto.response.AppointmentResponse;
import com.tus.cliniccare.entity.Appointment;
import com.tus.cliniccare.entity.enums.AppointmentStatus;
import com.tus.cliniccare.service.AppointmentService;
import com.tus.cliniccare.util.mapper.AppointmentMapper;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/doctor")
@PreAuthorize("hasRole('DOCTOR')")
@Tag(name = "Doctor", description = "Doctor schedule and appointment viewing endpoints.")
@SecurityRequirement(name = "bearerAuth")
public class DoctorController {

    private final AppointmentService appointmentService;

    public DoctorController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @GetMapping("/appointments")
    public ResponseEntity<List<AppointmentResponse>> getDoctorAppointments(
            Authentication authentication,
            @RequestParam(required = false) AppointmentStatus status
    ) {
        List<Appointment> appointments = status == null
                ? appointmentService.getAppointmentsByDoctor(authentication.getName())
                : appointmentService.getAppointmentsByDoctorAndStatus(authentication.getName(), status);

        List<AppointmentResponse> responses = appointments.stream()
                .map(AppointmentMapper::toResponse)
                .toList();
        return ResponseEntity.ok(responses);
    }
}
