package com.tus.cliniccare.service;

import com.tus.cliniccare.entity.TimeSlot;
import com.tus.cliniccare.entity.enums.TimeSlotStatus;
import com.tus.cliniccare.repository.TimeSlotRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TimeSlotService {

    private final TimeSlotRepository timeSlotRepository;

    public TimeSlotService(TimeSlotRepository timeSlotRepository) {
        this.timeSlotRepository = timeSlotRepository;
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
}
