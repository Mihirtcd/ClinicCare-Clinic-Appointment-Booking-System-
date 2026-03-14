package com.tus.cliniccare.repository;

import com.tus.cliniccare.entity.TimeSlot;
import com.tus.cliniccare.entity.enums.TimeSlotStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface TimeSlotRepository extends JpaRepository<TimeSlot, Long> {

    List<TimeSlot> findByDoctorIdOrderByStartTimeAsc(Long doctorId);

    boolean existsByDoctorIdAndStartTimeLessThanAndEndTimeGreaterThan(
            Long doctorId,
            LocalDateTime endTime,
            LocalDateTime startTime
    );

    List<TimeSlot> findByDoctorIdAndStatusOrderByStartTimeAsc(Long doctorId, TimeSlotStatus status);

    List<TimeSlot> findByStatusOrderByStartTimeAsc(TimeSlotStatus status);
}
