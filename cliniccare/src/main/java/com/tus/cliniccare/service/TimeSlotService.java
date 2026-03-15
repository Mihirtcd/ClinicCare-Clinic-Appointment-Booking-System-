package com.tus.cliniccare.service;

import com.tus.cliniccare.entity.Doctor;
import com.tus.cliniccare.entity.TimeSlot;
import com.tus.cliniccare.entity.enums.TimeSlotStatus;
import com.tus.cliniccare.exception.ConflictException;
import com.tus.cliniccare.exception.ResourceNotFoundException;
import com.tus.cliniccare.repository.DoctorRepository;
import com.tus.cliniccare.repository.TimeSlotRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TimeSlotService {

    private final TimeSlotRepository timeSlotRepository;
    private final DoctorRepository doctorRepository;

    public TimeSlotService(TimeSlotRepository timeSlotRepository, DoctorRepository doctorRepository) {
        this.timeSlotRepository = timeSlotRepository;
        this.doctorRepository = doctorRepository;
    }

    public List<TimeSlot> getSlotsByDoctor(Long doctorId) {
        return timeSlotRepository.findByDoctorId(doctorId);
    }

    public List<TimeSlot> getAvailableSlotsByDoctor(Long doctorId) {
        return timeSlotRepository.findByDoctorIdAndStatus(doctorId, TimeSlotStatus.AVAILABLE);
    }

    public boolean hasOverlappingSlot(Long doctorId, LocalDateTime startTime, LocalDateTime endTime) {
        if (doctorId == null || startTime == null || endTime == null || !endTime.isAfter(startTime)) {
            return false;
        }

        // Initial overlap validation; can be replaced with a repository-level optimized query later.
        return timeSlotRepository.findByDoctorId(doctorId).stream()
                .anyMatch(slot -> slot.getStartTime().isBefore(endTime) && slot.getEndTime().isAfter(startTime));
    }

    @Transactional
    public TimeSlot createTimeSlot(Long doctorId, LocalDateTime startTime, LocalDateTime endTime) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found."));

        if (hasOverlappingSlot(doctorId, startTime, endTime)) {
            throw new ConflictException("Overlapping time slot exists for this doctor.");
        }

        TimeSlot timeSlot = new TimeSlot();
        timeSlot.setDoctor(doctor);
        timeSlot.setStartTime(startTime);
        timeSlot.setEndTime(endTime);
        timeSlot.setStatus(TimeSlotStatus.AVAILABLE);
        return timeSlotRepository.save(timeSlot);
    }
}
