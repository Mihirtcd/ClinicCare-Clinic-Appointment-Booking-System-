package com.tus.cliniccare.repository;

import com.tus.cliniccare.entity.ServiceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ServiceRepository extends JpaRepository<ServiceEntity, Long> {

    @Query("""
            select distinct ds.service
            from DoctorService ds
            order by ds.service.name asc
            """)
    List<ServiceEntity> findEnabledServices();
}
