package com.tus.cliniccare.service;

import com.tus.cliniccare.entity.ServiceEntity;
import com.tus.cliniccare.repository.ServiceRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ServiceEntityService {

    private final ServiceRepository serviceRepository;

    public ServiceEntityService(ServiceRepository serviceRepository) {
        this.serviceRepository = serviceRepository;
    }

    public List<ServiceEntity> getEnabledServices() {
        return serviceRepository.findByIsEnabledTrue();
    }

    public Optional<ServiceEntity> getServiceById(Long serviceId) {
        return serviceRepository.findById(serviceId);
    }
}
