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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class DoctorService {

    private final DoctorRepository doctorRepository;
    private final DoctorServiceRepository doctorServiceRepository;
    private final UserRepository userRepository;
    private final ServiceRepository serviceRepository;

    public DoctorService(
            DoctorRepository doctorRepository,
            DoctorServiceRepository doctorServiceRepository,
            UserRepository userRepository,
            ServiceRepository serviceRepository
    ) {
        this.doctorRepository = doctorRepository;
        this.doctorServiceRepository = doctorServiceRepository;
        this.userRepository = userRepository;
        this.serviceRepository = serviceRepository;
    }

    public Optional<Doctor> findByUserId(Long userId) {
        return doctorRepository.findByUserId(userId);
    }

    public Optional<Doctor> getDoctorById(Long doctorId) {
        return doctorRepository.findById(doctorId);
    }

    public List<Doctor> getAllDoctors() {
        return doctorRepository.findAll();
    }

    public List<com.tus.cliniccare.entity.DoctorService> getDoctorServices(Long doctorId) {
        return doctorServiceRepository.findByDoctorId(doctorId);
    }

    @Transactional
    public Doctor createDoctor(Long userId, String specialization, List<Long> serviceIds) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));

        if (user.getRole() != Role.DOCTOR) {
            throw new BadRequestException("Selected user must have DOCTOR role.");
        }

        if (doctorRepository.findByUserId(userId).isPresent()) {
            throw new ConflictException("Doctor profile already exists for this user.");
        }

        Doctor doctor = new Doctor();
        doctor.setUser(user);
        doctor.setSpecialization(specialization);
        Doctor savedDoctor = doctorRepository.save(doctor);

        List<Long> ids = serviceIds == null ? new ArrayList<>() : serviceIds;
        for (Long serviceId : ids) {
            ServiceEntity service = serviceRepository.findById(serviceId)
                    .orElseThrow(() -> new ResourceNotFoundException("Service not found for id: " + serviceId));

            com.tus.cliniccare.entity.DoctorService doctorService = new com.tus.cliniccare.entity.DoctorService();
            doctorService.setDoctor(savedDoctor);
            doctorService.setService(service);
            doctorServiceRepository.save(doctorService);
        }

        return savedDoctor;
    }
}
