package com.tus.cliniccare.service;

import com.tus.cliniccare.entity.ServiceEntity;
import com.tus.cliniccare.exception.ResourceNotFoundException;
import com.tus.cliniccare.repository.ServiceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServiceEntityServiceTest {

    private static final Long SERVICE_ID = 11L;

    @Mock
    private ServiceRepository serviceRepository;

    @InjectMocks
    private ServiceEntityService serviceEntityService;

    @Test
    void getEnabledServices_shouldReturnEnabledServices() {
        ServiceEntity enabledService = service("General Consultation", true, 30);
        when(serviceRepository.findByIsEnabledTrue()).thenReturn(List.of(enabledService));

        List<ServiceEntity> result = serviceEntityService.getEnabledServices();

        assertEquals(1, result.size());
        assertTrue(result.get(0).getIsEnabled());
    }

    @Test
    void createService_shouldDefaultEnabledToTrue_whenEnabledIsNull() {
        when(serviceRepository.save(any(ServiceEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ServiceEntity result = serviceEntityService.createService(
                "Dermatology Consultation",
                "Skin consultation",
                25,
                null
        );

        assertEquals("Dermatology Consultation", result.getName());
        assertTrue(result.getIsEnabled());
    }

    @Test
    void createService_shouldUseProvidedEnabledFlag_whenProvided() {
        when(serviceRepository.save(any(ServiceEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ServiceEntity result = serviceEntityService.createService(
                "Dental Checkup",
                "Dental checkup service",
                20,
                false
        );

        assertFalse(result.getIsEnabled());
    }

    @Test
    void updateService_shouldUpdateAllFields_whenEnabledProvided() {
        ServiceEntity existing = service("Old Name", true, 30);
        existing.setId(SERVICE_ID);

        when(serviceRepository.findById(SERVICE_ID)).thenReturn(Optional.of(existing));
        when(serviceRepository.save(any(ServiceEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ServiceEntity updated = serviceEntityService.updateService(
                SERVICE_ID,
                "Updated Name",
                "Updated Description",
                45,
                false
        );

        assertEquals("Updated Name", updated.getName());
        assertEquals("Updated Description", updated.getDescription());
        assertEquals(45, updated.getDurationMinutes());
        assertFalse(updated.getIsEnabled());
    }

    @Test
    void updateService_shouldKeepEnabledUnchanged_whenEnabledIsNull() {
        ServiceEntity existing = service("General Consultation", true, 30);
        existing.setId(SERVICE_ID);

        when(serviceRepository.findById(SERVICE_ID)).thenReturn(Optional.of(existing));
        when(serviceRepository.save(any(ServiceEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ServiceEntity updated = serviceEntityService.updateService(
                SERVICE_ID,
                "General Consultation Updated",
                "Updated text",
                35,
                null
        );

        assertTrue(updated.getIsEnabled());
        assertEquals(35, updated.getDurationMinutes());
    }

    @Test
    void updateService_shouldThrowResourceNotFound_whenServiceMissing() {
        when(serviceRepository.findById(SERVICE_ID)).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> serviceEntityService.updateService(
                        SERVICE_ID,
                        "Name",
                        "Description",
                        30,
                        true
                )
        );
    }

    @Test
    void disableService_shouldSetEnabledFalse() {
        ServiceEntity existing = service("General Consultation", true, 30);
        existing.setId(SERVICE_ID);

        when(serviceRepository.findById(SERVICE_ID)).thenReturn(Optional.of(existing));
        when(serviceRepository.save(any(ServiceEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ServiceEntity disabled = serviceEntityService.disableService(SERVICE_ID);

        assertFalse(disabled.getIsEnabled());
        verify(serviceRepository).save(existing);
    }

    @Test
    void disableService_shouldThrowResourceNotFound_whenServiceMissing() {
        when(serviceRepository.findById(SERVICE_ID)).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> serviceEntityService.disableService(SERVICE_ID)
        );
    }

    private ServiceEntity service(String name, boolean isEnabled, int durationMinutes) {
        ServiceEntity service = new ServiceEntity();
        service.setName(name);
        service.setDescription("Description");
        service.setDurationMinutes(durationMinutes);
        service.setIsEnabled(isEnabled);
        return service;
    }
}
