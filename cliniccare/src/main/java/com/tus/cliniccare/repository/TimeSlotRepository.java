package com.tus.cliniccare.repository;

import com.tus.cliniccare.entity.TimeSlot;
import com.tus.cliniccare.entity.enums.TimeSlotStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TimeSlotRepository extends JpaRepository<TimeSlot, Long> {

    List<TimeSlot> findByDoctorId(Long doctorId);

    List<TimeSlot> findByDoctorIdAndStatus(Long doctorId, TimeSlotStatus status);
}
