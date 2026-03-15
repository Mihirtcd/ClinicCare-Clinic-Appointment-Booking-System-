package com.tus.cliniccare.controller;

import com.tus.cliniccare.dto.request.RejectAppointmentRequest;
import com.tus.cliniccare.dto.response.AppointmentResponse;
import com.tus.cliniccare.entity.Appointment;
import com.tus.cliniccare.exception.BadRequestException;
import com.tus.cliniccare.service.AppointmentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {

    private final AppointmentService appointmentService;

    public AppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @PatchMapping("/{id}/confirm")
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN')")
    public ResponseEntity<AppointmentResponse> confirmAppointment(
            @PathVariable Long id,
            Authentication authentication
    ) {
        Appointment appointment = appointmentService.confirmAppointment(
                id,
                authentication.getName(),
                isAdmin(authentication)
        );
        return ResponseEntity.ok(toAppointmentResponse(appointment));
    }

    @PatchMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN')")
    public ResponseEntity<AppointmentResponse> rejectAppointment(
            @PathVariable Long id,
            @Valid @RequestBody RejectAppointmentRequest request,
            Authentication authentication
    ) {
        if (request.getAppointmentId() != null && !id.equals(request.getAppointmentId())) {
            throw new BadRequestException("Appointment id in path and body must match.");
        }

        Appointment appointment = appointmentService.rejectAppointment(
                id,
                authentication.getName(),
                isAdmin(authentication)
        );
        return ResponseEntity.ok(toAppointmentResponse(appointment));
    }

    @PatchMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN')")
    public ResponseEntity<AppointmentResponse> completeAppointment(
            @PathVariable Long id,
            Authentication authentication
    ) {
        Appointment appointment = appointmentService.completeAppointment(
                id,
                authentication.getName(),
                isAdmin(authentication)
        );
        return ResponseEntity.ok(toAppointmentResponse(appointment));
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
    }

    private AppointmentResponse toAppointmentResponse(Appointment appointment) {
        AppointmentResponse response = new AppointmentResponse();
        response.setId(appointment.getId());
        response.setPatientId(appointment.getPatient().getId());
        response.setDoctorId(appointment.getDoctor().getId());
        response.setServiceId(appointment.getService().getId());
        response.setTimeSlotId(appointment.getTimeSlot().getId());
        response.setStatus(appointment.getStatus());
        response.setPatientNote(appointment.getPatientNote());
        response.setBookedAt(appointment.getBookedAt());
        return response;
    }
}
