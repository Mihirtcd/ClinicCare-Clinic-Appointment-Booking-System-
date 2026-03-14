package com.tus.cliniccare.repository;

import com.tus.cliniccare.entity.ServiceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ServiceRepository extends JpaRepository<ServiceEntity, Long> {

    List<ServiceEntity> findByIsEnabledTrue();
}
