package com.tus.cliniccare.service;

import com.tus.cliniccare.entity.Doctor;
import com.tus.cliniccare.entity.ServiceEntity;
import com.tus.cliniccare.entity.User;
import com.tus.cliniccare.entity.enums.Role;
import com.tus.cliniccare.exception.BadRequestException;
import com.tus.cliniccare.exception.ConflictException;
import com.tus.cliniccare.exception.ResourceNotFoundException;
import com.tus.cliniccare.repository.DoctorRepository;
import com.tus.cliniccare.repository.DoctorServiceRepository;
import com.tus.cliniccare.repository.ServiceRepository;
import com.tus.cliniccare.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DoctorServiceTest {

    private static final Long USER_ID = 10L;
    private static final Long DOCTOR_ID = 20L;
    private static final Long SERVICE_1_ID = 101L;
    private static final Long SERVICE_2_ID = 102L;

    @Mock
    private DoctorRepository doctorRepository;

    @Mock
    private DoctorServiceRepository doctorServiceRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ServiceRepository serviceRepository;

    @InjectMocks
    private DoctorService doctorService;

    @Test
    void createDoctor_shouldSucceed_whenDoctorUserAndServicesAreValid() {
        User doctorUser = doctorUser();
        Doctor savedDoctor = new Doctor();
        savedDoctor.setId(DOCTOR_ID);
        savedDoctor.setUser(doctorUser);
        savedDoctor.setSpecialization("Cardiology");

        ServiceEntity service1 = service(SERVICE_1_ID, "General Consultation");
        ServiceEntity service2 = service(SERVICE_2_ID, "Cardio Consultation");

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(doctorUser));
        when(doctorRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
        when(doctorRepository.save(any(Doctor.class))).thenReturn(savedDoctor);
        when(serviceRepository.findById(SERVICE_1_ID)).thenReturn(Optional.of(service1));
        when(serviceRepository.findById(SERVICE_2_ID)).thenReturn(Optional.of(service2));
        when(doctorServiceRepository.save(any(com.tus.cliniccare.entity.DoctorService.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Doctor result = doctorService.createDoctor(
                USER_ID,
                "Cardiology",
                List.of(SERVICE_1_ID, SERVICE_2_ID)
        );

        assertEquals(DOCTOR_ID, result.getId());
        assertEquals("Cardiology", result.getSpecialization());
        verify(doctorRepository).save(any(Doctor.class));
        verify(doctorServiceRepository, times(2)).save(any(com.tus.cliniccare.entity.DoctorService.class));
    }

    @Test
    void createDoctor_shouldSucceed_whenServiceIdsIsNull() {
        User doctorUser = doctorUser();
        Doctor savedDoctor = new Doctor();
        savedDoctor.setId(DOCTOR_ID);
        savedDoctor.setUser(doctorUser);
        savedDoctor.setSpecialization("General Medicine");

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(doctorUser));
        when(doctorRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
        when(doctorRepository.save(any(Doctor.class))).thenReturn(savedDoctor);

        Doctor result = doctorService.createDoctor(USER_ID, "General Medicine", null);

        assertEquals(DOCTOR_ID, result.getId());
        verify(doctorServiceRepository, never()).save(any(com.tus.cliniccare.entity.DoctorService.class));
    }

    @Test
    void createDoctor_shouldThrowResourceNotFound_whenUserMissing() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> doctorService.createDoctor(USER_ID, "Cardiology", List.of(SERVICE_1_ID))
        );
    }

    @Test
    void createDoctor_shouldThrowBadRequest_whenUserRoleIsNotDoctor() {
        User patientUser = new User();
        patientUser.setId(USER_ID);
        patientUser.setRole(Role.PATIENT);

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(patientUser));

        assertThrows(
                BadRequestException.class,
                () -> doctorService.createDoctor(USER_ID, "Cardiology", List.of(SERVICE_1_ID))
        );
    }

    @Test
    void createDoctor_shouldThrowConflict_whenDoctorProfileAlreadyExists() {
        User doctorUser = doctorUser();
        Doctor existingDoctor = new Doctor();
        existingDoctor.setId(DOCTOR_ID);
        existingDoctor.setUser(doctorUser);

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(doctorUser));
        when(doctorRepository.findByUserId(USER_ID)).thenReturn(Optional.of(existingDoctor));

        assertThrows(
                ConflictException.class,
                () -> doctorService.createDoctor(USER_ID, "Cardiology", List.of(SERVICE_1_ID))
        );
    }

    @Test
    void createDoctor_shouldThrowResourceNotFound_whenServiceIsMissing() {
        User doctorUser = doctorUser();
        Doctor savedDoctor = new Doctor();
        savedDoctor.setId(DOCTOR_ID);
        savedDoctor.setUser(doctorUser);

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(doctorUser));
        when(doctorRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
        when(doctorRepository.save(any(Doctor.class))).thenReturn(savedDoctor);
        when(serviceRepository.findById(SERVICE_1_ID)).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> doctorService.createDoctor(USER_ID, "Cardiology", List.of(SERVICE_1_ID))
        );
    }

    @Test
    void getDoctorServices_shouldReturnMappingsFromRepository() {
        com.tus.cliniccare.entity.DoctorService mapping = new com.tus.cliniccare.entity.DoctorService();
        when(doctorServiceRepository.findByDoctorId(DOCTOR_ID)).thenReturn(List.of(mapping));

        List<com.tus.cliniccare.entity.DoctorService> result = doctorService.getDoctorServices(DOCTOR_ID);

        assertEquals(1, result.size());
        verify(doctorServiceRepository).findByDoctorId(DOCTOR_ID);
    }

    private User doctorUser() {
        User user = new User();
        user.setId(USER_ID);
        user.setRole(Role.DOCTOR);
        user.setEmail("doctor@cliniccare.com");
        return user;
    }

    private ServiceEntity service(Long id, String name) {
        ServiceEntity service = new ServiceEntity();
        service.setId(id);
        service.setName(name);
        service.setDurationMinutes(30);
        service.setIsEnabled(true);
        return service;
    }
}
