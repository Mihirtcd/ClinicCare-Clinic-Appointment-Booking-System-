package com.tus.cliniccare.service;

import com.tus.cliniccare.entity.Doctor;
import com.tus.cliniccare.repository.DoctorRepository;
import com.tus.cliniccare.repository.DoctorServiceRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class DoctorService {

    private final DoctorRepository doctorRepository;
    private final DoctorServiceRepository doctorServiceRepository;

    public DoctorService(DoctorRepository doctorRepository, DoctorServiceRepository doctorServiceRepository) {
        this.doctorRepository = doctorRepository;
        this.doctorServiceRepository = doctorServiceRepository;
    }

    public Optional<Doctor> findByUserId(Long userId) {
        return doctorRepository.findByUserId(userId);
    }

    public Optional<Doctor> getDoctorById(Long doctorId) {
        return doctorRepository.findById(doctorId);
    }

    public List<com.tus.cliniccare.entity.DoctorService> getDoctorServices(Long doctorId) {
        return doctorServiceRepository.findByDoctorId(doctorId);
    }
}
