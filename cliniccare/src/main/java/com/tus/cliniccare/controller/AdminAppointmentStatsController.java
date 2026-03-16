package com.tus.cliniccare.controller;

import com.tus.cliniccare.dto.response.AppointmentStatsResponse;
import com.tus.cliniccare.entity.enums.AppointmentStatus;
import com.tus.cliniccare.service.AppointmentService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/appointments")
@PreAuthorize("hasRole('ADMIN')")
public class AdminAppointmentStatsController {

    private final AppointmentService appointmentService;

    public AdminAppointmentStatsController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @GetMapping("/stats")
    public ResponseEntity<AppointmentStatsResponse> getAppointmentStats() {
        AppointmentStatsResponse response = new AppointmentStatsResponse();
        response.setTotal(appointmentService.getTotalAppointmentsCount());
        response.setPending(appointmentService.getAppointmentsCountByStatus(AppointmentStatus.PENDING));
        response.setConfirmed(appointmentService.getAppointmentsCountByStatus(AppointmentStatus.CONFIRMED));
        response.setRejected(appointmentService.getAppointmentsCountByStatus(AppointmentStatus.REJECTED));
        response.setCompleted(appointmentService.getAppointmentsCountByStatus(AppointmentStatus.COMPLETED));
        response.setCancelled(appointmentService.getAppointmentsCountByStatus(AppointmentStatus.CANCELLED));
        return ResponseEntity.ok(response);
    }
}
