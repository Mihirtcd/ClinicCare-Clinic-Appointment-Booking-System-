package com.tus.cliniccare.service;

import com.tus.cliniccare.entity.ServiceEntity;
import com.tus.cliniccare.exception.ResourceNotFoundException;
import com.tus.cliniccare.repository.ServiceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    public List<ServiceEntity> getAllServices() {
        return serviceRepository.findAll();
    }

    public Optional<ServiceEntity> getServiceById(Long serviceId) {
        return serviceRepository.findById(serviceId);
    }

    @Transactional
    public ServiceEntity createService(
            String name,
            String description,
            Integer durationMinutes,
            Boolean isEnabled
    ) {
        ServiceEntity service = new ServiceEntity();
        service.setName(name);
        service.setDescription(description);
        service.setDurationMinutes(durationMinutes);
        service.setIsEnabled(isEnabled == null ? Boolean.TRUE : isEnabled);
        return serviceRepository.save(service);
    }

    @Transactional
    public ServiceEntity updateService(
            Long serviceId,
            String name,
            String description,
            Integer durationMinutes,
            Boolean isEnabled
    ) {
        ServiceEntity service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Service not found."));

        service.setName(name);
        service.setDescription(description);
        service.setDurationMinutes(durationMinutes);
        if (isEnabled != null) {
            service.setIsEnabled(isEnabled);
        }

        return serviceRepository.save(service);
    }

    @Transactional
    public ServiceEntity disableService(Long serviceId) {
        ServiceEntity service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Service not found."));
        service.setIsEnabled(Boolean.FALSE);
        return serviceRepository.save(service);
    }
}
