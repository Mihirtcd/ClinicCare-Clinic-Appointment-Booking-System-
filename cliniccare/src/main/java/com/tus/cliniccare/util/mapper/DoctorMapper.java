package com.tus.cliniccare.util.mapper;

import com.tus.cliniccare.dto.response.DoctorResponse;
import com.tus.cliniccare.entity.Doctor;

import java.util.List;

public final class DoctorMapper {

    private DoctorMapper() {
    }

    public static DoctorResponse toResponse(Doctor doctor, List<Long> serviceIds) {
        DoctorResponse response = new DoctorResponse();
        response.setId(doctor.getId());
        response.setUserId(doctor.getUser().getId());
        response.setFirstName(doctor.getUser().getFirstName());
        response.setLastName(doctor.getUser().getLastName());
        response.setSpecialization(doctor.getSpecialization());
        response.setServiceIds(serviceIds);
        return response;
    }
}
