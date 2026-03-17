package com.tus.cliniccare.util.mapper;

import com.tus.cliniccare.dto.response.TimeSlotResponse;
import com.tus.cliniccare.entity.TimeSlot;

public final class TimeSlotMapper {

    private TimeSlotMapper() {
    }

    public static TimeSlotResponse toResponse(TimeSlot slot) {
        TimeSlotResponse response = new TimeSlotResponse();
        response.setId(slot.getId());
        response.setDoctorId(slot.getDoctor().getId());
        response.setStartTime(slot.getStartTime());
        response.setEndTime(slot.getEndTime());
        response.setStatus(slot.getStatus());
        return response;
    }
}
