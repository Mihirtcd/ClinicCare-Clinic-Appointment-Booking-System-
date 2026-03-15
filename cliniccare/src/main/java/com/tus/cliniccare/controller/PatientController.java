package com.tus.cliniccare.controller;

import com.tus.cliniccare.dto.request.BookAppointmentRequest;
import com.tus.cliniccare.dto.response.AppointmentResponse;
import com.tus.cliniccare.dto.response.ServiceResponse;
import com.tus.cliniccare.dto.response.TimeSlotResponse;
import com.tus.cliniccare.entity.Appointment;
import com.tus.cliniccare.entity.ServiceEntity;
import com.tus.cliniccare.entity.TimeSlot;
import com.tus.cliniccare.service.AppointmentService;
import com.tus.cliniccare.service.ServiceEntityService;
import com.tus.cliniccare.service.TimeSlotService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/patient")
@PreAuthorize("hasRole('PATIENT')")
public class PatientController {

    private final ServiceEntityService serviceEntityService;
    private final TimeSlotService timeSlotService;
    private final AppointmentService appointmentService;

    public PatientController(
            ServiceEntityService serviceEntityService,
            TimeSlotService timeSlotService,
            AppointmentService appointmentService
    ) {
        this.serviceEntityService = serviceEntityService;
        this.timeSlotService = timeSlotService;
        this.appointmentService = appointmentService;
    }

    @GetMapping("/services")
    public ResponseEntity<List<ServiceResponse>> getEnabledServices() {
        List<ServiceResponse> responses = serviceEntityService.getEnabledServices()
                .stream()
                .map(this::toServiceResponse)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/slots")
    public ResponseEntity<List<TimeSlotResponse>> getAvailableSlotsByDoctor(@RequestParam Long doctorId) {
        List<TimeSlotResponse> responses = timeSlotService.getAvailableSlotsByDoctor(doctorId)
                .stream()
                .map(this::toTimeSlotResponse)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/appointments")
    public ResponseEntity<List<AppointmentResponse>> getAppointmentsByPatient(Authentication authentication) {
        List<AppointmentResponse> responses = appointmentService.getAppointmentsByPatient(authentication.getName())
                .stream()
                .map(this::toAppointmentResponse)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @PostMapping("/appointments")
    public ResponseEntity<AppointmentResponse> bookAppointment(
            @Valid @RequestBody BookAppointmentRequest request,
            Authentication authentication
    ) {
        Appointment appointment = appointmentService.bookAppointment(
                authentication.getName(),
                request.getDoctorId(),
                request.getServiceId(),
                request.getTimeSlotId(),
                request.getPatientNote()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(toAppointmentResponse(appointment));
    }

    private ServiceResponse toServiceResponse(ServiceEntity service) {
        ServiceResponse response = new ServiceResponse();
        response.setId(service.getId());
        response.setName(service.getName());
        response.setDescription(service.getDescription());
        response.setDurationMinutes(service.getDurationMinutes());
        response.setIsEnabled(service.getIsEnabled());
        return response;
    }

    private TimeSlotResponse toTimeSlotResponse(TimeSlot slot) {
        TimeSlotResponse response = new TimeSlotResponse();
        response.setId(slot.getId());
        response.setDoctorId(slot.getDoctor().getId());
        response.setStartTime(slot.getStartTime());
        response.setEndTime(slot.getEndTime());
        response.setStatus(slot.getStatus());
        return response;
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
