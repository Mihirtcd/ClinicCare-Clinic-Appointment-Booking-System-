package com.tus.cliniccare.controller;

import com.tus.cliniccare.dto.request.BookAppointmentRequest;
import com.tus.cliniccare.dto.response.AppointmentResponse;
import com.tus.cliniccare.dto.response.DoctorResponse;
import com.tus.cliniccare.dto.response.ServiceResponse;
import com.tus.cliniccare.dto.response.TimeSlotResponse;
import com.tus.cliniccare.entity.Appointment;
import com.tus.cliniccare.entity.Doctor;
import com.tus.cliniccare.entity.ServiceEntity;
import com.tus.cliniccare.entity.TimeSlot;
import com.tus.cliniccare.service.AppointmentService;
import com.tus.cliniccare.service.DoctorService;
import com.tus.cliniccare.service.ServiceEntityService;
import com.tus.cliniccare.service.TimeSlotService;
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
                .map(this::toServiceResponse)
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

    @PatchMapping("/appointments/{id}/cancel")
    public ResponseEntity<AppointmentResponse> cancelAppointment(
            @PathVariable Long id,
            Authentication authentication
    ) {
        Appointment appointment = appointmentService.cancelAppointmentByPatient(id, authentication.getName());
        return ResponseEntity.ok(toAppointmentResponse(appointment));
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

    private DoctorResponse toDoctorResponse(Doctor doctor) {
        DoctorResponse response = new DoctorResponse();
        response.setId(doctor.getId());
        response.setUserId(doctor.getUser().getId());
        response.setFirstName(doctor.getUser().getFirstName());
        response.setLastName(doctor.getUser().getLastName());
        response.setSpecialization(doctor.getSpecialization());
        response.setServiceIds(
                doctorService.getDoctorServices(doctor.getId()).stream()
                        .map(doctorServiceEntity -> doctorServiceEntity.getService().getId())
                        .toList()
        );
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
