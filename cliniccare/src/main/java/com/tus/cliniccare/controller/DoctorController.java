package com.tus.cliniccare.controller;

import com.tus.cliniccare.dto.response.AppointmentResponse;
import com.tus.cliniccare.entity.Appointment;
import com.tus.cliniccare.entity.enums.AppointmentStatus;
import com.tus.cliniccare.service.AppointmentService;
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
                .map(this::toAppointmentResponse)
                .toList();
        return ResponseEntity.ok(responses);
    }

    private AppointmentResponse toAppointmentResponse(Appointment appointment) {
        AppointmentResponse response = new AppointmentResponse();
        response.setId(appointment.getId());
        response.setPatientId(appointment.getPatient().getId());
        response.setPatientName(
                ((appointment.getPatient().getFirstName() == null ? "" : appointment.getPatient().getFirstName()) + " "
                        + (appointment.getPatient().getLastName() == null ? "" : appointment.getPatient().getLastName()))
                        .trim()
        );
        response.setDoctorId(appointment.getDoctor().getId());
        response.setDoctorName(
                ((appointment.getDoctor().getUser().getFirstName() == null ? "" : appointment.getDoctor().getUser().getFirstName()) + " "
                        + (appointment.getDoctor().getUser().getLastName() == null ? "" : appointment.getDoctor().getUser().getLastName()))
                        .trim()
        );
        response.setServiceId(appointment.getService().getId());
        response.setServiceName(appointment.getService().getName());
        response.setTimeSlotId(appointment.getTimeSlot().getId());
        response.setSlotStartTime(appointment.getTimeSlot().getStartTime());
        response.setSlotEndTime(appointment.getTimeSlot().getEndTime());
        response.setStatus(appointment.getStatus());
        response.setPatientNote(appointment.getPatientNote());
        response.setBookedAt(appointment.getBookedAt());
        return response;
    }
}
