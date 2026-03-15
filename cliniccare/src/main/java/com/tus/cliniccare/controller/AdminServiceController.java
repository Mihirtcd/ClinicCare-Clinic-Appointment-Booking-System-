package com.tus.cliniccare.controller;

import com.tus.cliniccare.dto.request.CreateServiceRequest;
import com.tus.cliniccare.dto.response.ServiceResponse;
import com.tus.cliniccare.entity.ServiceEntity;
import com.tus.cliniccare.service.ServiceEntityService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping({"/admin/services", "/api/admin/services"})
@PreAuthorize("hasRole('ADMIN')")
public class AdminServiceController {

    private final ServiceEntityService serviceEntityService;

    public AdminServiceController(ServiceEntityService serviceEntityService) {
        this.serviceEntityService = serviceEntityService;
    }

    @PostMapping
    public ResponseEntity<ServiceResponse> createService(@Valid @RequestBody CreateServiceRequest request) {
        ServiceEntity service = serviceEntityService.createService(
                request.getName(),
                request.getDescription(),
                request.getDurationMinutes(),
                request.getIsEnabled()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(toServiceResponse(service));
    }

    @GetMapping
    public ResponseEntity<List<ServiceResponse>> getAllServices() {
        List<ServiceResponse> responses = serviceEntityService.getAllServices()
                .stream()
                .map(this::toServiceResponse)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ServiceResponse> updateService(
            @PathVariable Long id,
            @Valid @RequestBody CreateServiceRequest request
    ) {
        ServiceEntity updated = serviceEntityService.updateService(
                id,
                request.getName(),
                request.getDescription(),
                request.getDurationMinutes(),
                request.getIsEnabled()
        );
        return ResponseEntity.ok(toServiceResponse(updated));
    }

    @PatchMapping("/{id}/disable")
    public ResponseEntity<ServiceResponse> disableService(@PathVariable Long id) {
        ServiceEntity disabled = serviceEntityService.disableService(id);
        return ResponseEntity.ok(toServiceResponse(disabled));
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
}
