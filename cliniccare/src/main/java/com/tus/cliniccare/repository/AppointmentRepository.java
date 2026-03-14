package com.tus.cliniccare.repository;

import com.tus.cliniccare.entity.Appointment;
import com.tus.cliniccare.entity.enums.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    boolean existsByTimeSlotId(Long timeSlotId);

    List<Appointment> findByPatientIdOrderByBookedAtDesc(Long patientId);

    List<Appointment> findByDoctorIdOrderByBookedAtDesc(Long doctorId);

    List<Appointment> findByStatusOrderByBookedAtDesc(AppointmentStatus status);
}
