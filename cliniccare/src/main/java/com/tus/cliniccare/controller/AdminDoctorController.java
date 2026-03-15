package com.tus.cliniccare.controller;

import com.tus.cliniccare.dto.request.CreateDoctorRequest;
import com.tus.cliniccare.dto.response.DoctorResponse;
import com.tus.cliniccare.entity.Doctor;
import com.tus.cliniccare.service.DoctorService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/doctors")
@PreAuthorize("hasRole('ADMIN')")
public class AdminDoctorController {

    private final DoctorService doctorService;

    public AdminDoctorController(DoctorService doctorService) {
        this.doctorService = doctorService;
    }

    @PostMapping
    public ResponseEntity<DoctorResponse> createDoctor(@Valid @RequestBody CreateDoctorRequest request) {
        Doctor doctor = doctorService.createDoctor(
                request.getUserId(),
                request.getSpecialization(),
                request.getServiceIds()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(toDoctorResponse(doctor));
    }

    @GetMapping
    public ResponseEntity<List<DoctorResponse>> getAllDoctors() {
        List<DoctorResponse> responses = doctorService.getAllDoctors()
                .stream()
                .map(this::toDoctorResponse)
                .toList();
        return ResponseEntity.ok(responses);
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
}
