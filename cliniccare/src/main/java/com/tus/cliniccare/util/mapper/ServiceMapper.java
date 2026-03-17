package com.tus.cliniccare.util.mapper;

import com.tus.cliniccare.dto.response.ServiceResponse;
import com.tus.cliniccare.entity.ServiceEntity;

public final class ServiceMapper {

    private ServiceMapper() {
    }

    public static ServiceResponse toResponse(ServiceEntity service) {
        ServiceResponse response = new ServiceResponse();
        response.setId(service.getId());
        response.setName(service.getName());
        response.setDescription(service.getDescription());
        response.setDurationMinutes(service.getDurationMinutes());
        response.setIsEnabled(service.getIsEnabled());
        return response;
    }
}
