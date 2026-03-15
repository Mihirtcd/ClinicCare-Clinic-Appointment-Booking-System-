package com.tus.cliniccare.repository;

import com.tus.cliniccare.entity.DoctorService;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DoctorServiceRepository extends JpaRepository<DoctorService, Long> {

    List<DoctorService> findByDoctorId(Long doctorId);

    boolean existsByDoctorIdAndServiceId(Long doctorId, Long serviceId);
}
