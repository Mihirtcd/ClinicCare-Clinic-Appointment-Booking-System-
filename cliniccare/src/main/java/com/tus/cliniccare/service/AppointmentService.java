package com.tus.cliniccare.service;

import com.tus.cliniccare.entity.Appointment;
import com.tus.cliniccare.entity.Doctor;
import com.tus.cliniccare.entity.ServiceEntity;
import com.tus.cliniccare.entity.TimeSlot;
import com.tus.cliniccare.entity.User;
import com.tus.cliniccare.entity.enums.AppointmentStatus;
import com.tus.cliniccare.entity.enums.TimeSlotStatus;
import com.tus.cliniccare.repository.AppointmentRepository;
import com.tus.cliniccare.repository.DoctorRepository;
import com.tus.cliniccare.repository.DoctorServiceRepository;
import com.tus.cliniccare.repository.ServiceRepository;
import com.tus.cliniccare.repository.TimeSlotRepository;
import com.tus.cliniccare.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final UserRepository userRepository;
    private final DoctorRepository doctorRepository;
    private final DoctorServiceRepository doctorServiceRepository;
    private final ServiceRepository serviceRepository;
    private final TimeSlotRepository timeSlotRepository;

    public AppointmentService(
            AppointmentRepository appointmentRepository,
            UserRepository userRepository,
            DoctorRepository doctorRepository,
            DoctorServiceRepository doctorServiceRepository,
            ServiceRepository serviceRepository,
            TimeSlotRepository timeSlotRepository
    ) {
        this.appointmentRepository = appointmentRepository;
        this.userRepository = userRepository;
        this.doctorRepository = doctorRepository;
        this.doctorServiceRepository = doctorServiceRepository;
        this.serviceRepository = serviceRepository;
        this.timeSlotRepository = timeSlotRepository;
    }

    public List<Appointment> getAppointmentsByPatient(Long patientId) {
        return appointmentRepository.findByPatientId(patientId);
    }

    public List<Appointment> getAppointmentsByDoctor(Long doctorId) {
        return appointmentRepository.findByDoctorId(doctorId);
    }

    public List<Appointment> getAppointmentsByStatus(AppointmentStatus status) {
        return appointmentRepository.findByStatus(status);
    }

    public boolean isTimeSlotBooked(Long timeSlotId) {
        return appointmentRepository.existsByTimeSlotId(timeSlotId);
    }

    @Transactional
    public Appointment bookAppointment(Long patientId, Long doctorId, Long serviceId, Long timeSlotId, String patientNote) {
        if (isTimeSlotBooked(timeSlotId)) {
            throw new IllegalStateException("Selected time slot is already booked.");
        }

        User patient = userRepository.findById(patientId)
                .orElseThrow(() -> new IllegalArgumentException("Patient not found."));
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new IllegalArgumentException("Doctor not found."));
        ServiceEntity service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new IllegalArgumentException("Service not found."));
        TimeSlot timeSlot = timeSlotRepository.findById(timeSlotId)
                .orElseThrow(() -> new IllegalArgumentException("Time slot not found."));

        if (!doctorId.equals(timeSlot.getDoctor().getId())) {
            throw new IllegalArgumentException("Time slot does not belong to the selected doctor.");
        }

        if (!doctorServiceRepository.existsByDoctorIdAndServiceId(doctorId, serviceId)) {
            throw new IllegalArgumentException("Selected doctor does not provide the selected service.");
        }

        if (timeSlot.getStatus() != TimeSlotStatus.AVAILABLE) {
            throw new IllegalStateException("Time slot is not available.");
        }

        Appointment appointment = new Appointment();
        appointment.setPatient(patient);
        appointment.setDoctor(doctor);
        appointment.setService(service);
        appointment.setTimeSlot(timeSlot);
        appointment.setPatientNote(patientNote);
        appointment.setStatus(AppointmentStatus.PENDING);

        timeSlot.setStatus(TimeSlotStatus.RESERVED);
        timeSlotRepository.save(timeSlot);

        return appointmentRepository.save(appointment);
    }

    @Transactional
    public Appointment confirmAppointment(Long appointmentId) {
        Appointment appointment = getAppointmentById(appointmentId);
        validateTransition(appointment.getStatus(), AppointmentStatus.CONFIRMED);
        appointment.setStatus(AppointmentStatus.CONFIRMED);
        return appointmentRepository.save(appointment);
    }

    @Transactional
    public Appointment rejectAppointment(Long appointmentId) {
        Appointment appointment = getAppointmentById(appointmentId);
        validateTransition(appointment.getStatus(), AppointmentStatus.REJECTED);
        appointment.setStatus(AppointmentStatus.REJECTED);

        if (appointment.getTimeSlot() != null) {
            appointment.getTimeSlot().setStatus(TimeSlotStatus.AVAILABLE);
            timeSlotRepository.save(appointment.getTimeSlot());
        }

        return appointmentRepository.save(appointment);
    }

    @Transactional
    public Appointment completeAppointment(Long appointmentId) {
        Appointment appointment = getAppointmentById(appointmentId);
        validateTransition(appointment.getStatus(), AppointmentStatus.COMPLETED);
        appointment.setStatus(AppointmentStatus.COMPLETED);
        return appointmentRepository.save(appointment);
    }

    private Appointment getAppointmentById(Long appointmentId) {
        return appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found."));
    }

    private void validateTransition(AppointmentStatus currentStatus, AppointmentStatus targetStatus) {
        boolean allowed =
                (currentStatus == AppointmentStatus.PENDING && targetStatus == AppointmentStatus.CONFIRMED)
                        || (currentStatus == AppointmentStatus.PENDING && targetStatus == AppointmentStatus.REJECTED)
                        || (currentStatus == AppointmentStatus.CONFIRMED && targetStatus == AppointmentStatus.COMPLETED);

        if (!allowed) {
            throw new IllegalStateException(
                    "Invalid appointment status transition: " + currentStatus + " -> " + targetStatus
            );
        }
    }
}
