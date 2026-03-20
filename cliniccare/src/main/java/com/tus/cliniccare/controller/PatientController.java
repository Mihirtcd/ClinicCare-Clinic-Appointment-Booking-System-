package com.tus.cliniccare.controller;

import com.tus.cliniccare.dto.request.BookAppointmentRequest;
import com.tus.cliniccare.dto.response.AppointmentResponse;
import com.tus.cliniccare.dto.response.DoctorResponse;
import com.tus.cliniccare.dto.response.ServiceResponse;
import com.tus.cliniccare.dto.response.TimeSlotResponse;
import com.tus.cliniccare.entity.Appointment;
import com.tus.cliniccare.entity.Doctor;
import com.tus.cliniccare.service.AppointmentService;
import com.tus.cliniccare.service.DoctorService;
import com.tus.cliniccare.service.ServiceEntityService;
import com.tus.cliniccare.service.TimeSlotService;
import com.tus.cliniccare.util.mapper.AppointmentMapper;
import com.tus.cliniccare.util.mapper.DoctorMapper;
import com.tus.cliniccare.util.mapper.ServiceMapper;
import com.tus.cliniccare.util.mapper.TimeSlotMapper;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/patient")
@PreAuthorize("hasRole('PATIENT')")
@Tag(name = "Patient", description = "Patient operations for browsing, booking, and managing appointments.")
@SecurityRequirement(name = "bearerAuth")
public class PatientController {

    private final ServiceEntityService serviceEntityService;
    private final DoctorService doctorService;
    private final TimeSlotService timeSlotService;
    private final AppointmentService appointmentService;

    public PatientController(
            ServiceEntityService serviceEntityService,
            DoctorService doctorService,
            TimeSlotService timeSlotService,
            AppointmentService appointmentService
    ) {
        this.serviceEntityService = serviceEntityService;
        this.doctorService = doctorService;
        this.timeSlotService = timeSlotService;
        this.appointmentService = appointmentService;
    }

    @GetMapping("/services")
    public ResponseEntity<List<ServiceResponse>> getEnabledServices() {
        List<ServiceResponse> responses = serviceEntityService.getEnabledServices()
                .stream()
                .map(ServiceMapper::toResponse)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/doctors")
    public ResponseEntity<List<DoctorResponse>> getDoctorsForBooking() {
        List<DoctorResponse> responses = doctorService.getAllDoctors()
                .stream()
                .map(this::toDoctorResponse)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/slots")
    public ResponseEntity<List<TimeSlotResponse>> getAvailableSlotsByDoctor(@RequestParam Long doctorId) {
        List<TimeSlotResponse> responses = timeSlotService.getAvailableSlotsByDoctor(doctorId)
                .stream()
                .map(TimeSlotMapper::toResponse)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/appointments")
    public ResponseEntity<List<AppointmentResponse>> getAppointmentsByPatient(Authentication authentication) {
        List<AppointmentResponse> responses = appointmentService.getAppointmentsByPatient(authentication.getName())
                .stream()
                .map(AppointmentMapper::toResponse)
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
        return ResponseEntity.status(HttpStatus.CREATED).body(AppointmentMapper.toResponse(appointment));
    }

    @PatchMapping("/appointments/{id}/cancel")
    public ResponseEntity<AppointmentResponse> cancelAppointment(
            @PathVariable Long id,
            Authentication authentication
    ) {
        Appointment appointment = appointmentService.cancelAppointmentByPatient(id, authentication.getName());
        return ResponseEntity.ok(AppointmentMapper.toResponse(appointment));
    }

    private DoctorResponse toDoctorResponse(Doctor doctor) {
        List<Long> serviceIds = doctorService.getDoctorServices(doctor.getId()).stream()
                .map(doctorServiceEntity -> doctorServiceEntity.getService().getId())
                .toList();
        return DoctorMapper.toResponse(doctor, serviceIds);
    }
}
