package com.tus.cliniccare.controller;

import com.tus.cliniccare.dto.request.CreateTimeSlotRequest;
import com.tus.cliniccare.dto.response.TimeSlotResponse;
import com.tus.cliniccare.entity.TimeSlot;
import com.tus.cliniccare.service.TimeSlotService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping({"/admin/timeslots", "/api/admin/timeslots"})
@PreAuthorize("hasRole('ADMIN')")
public class AdminTimeSlotController {

    private final TimeSlotService timeSlotService;

    public AdminTimeSlotController(TimeSlotService timeSlotService) {
        this.timeSlotService = timeSlotService;
    }

    @PostMapping
    public ResponseEntity<TimeSlotResponse> createTimeSlot(@Valid @RequestBody CreateTimeSlotRequest request) {
        TimeSlot slot = timeSlotService.createTimeSlot(
                request.getDoctorId(),
                request.getStartTime(),
                request.getEndTime()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(toTimeSlotResponse(slot));
    }

    @GetMapping("/doctor/{doctorId}")
    public ResponseEntity<List<TimeSlotResponse>> getSlotsByDoctor(@PathVariable Long doctorId) {
        List<TimeSlotResponse> responses = timeSlotService.getSlotsByDoctor(doctorId)
                .stream()
                .map(this::toTimeSlotResponse)
                .toList();
        return ResponseEntity.ok(responses);
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
}
