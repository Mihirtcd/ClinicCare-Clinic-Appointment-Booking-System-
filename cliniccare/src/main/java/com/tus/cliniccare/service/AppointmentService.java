package com.tus.cliniccare.service;

import com.tus.cliniccare.entity.Appointment;
import com.tus.cliniccare.entity.Doctor;
import com.tus.cliniccare.entity.ServiceEntity;
import com.tus.cliniccare.entity.TimeSlot;
import com.tus.cliniccare.entity.User;
import com.tus.cliniccare.entity.enums.AppointmentStatus;
import com.tus.cliniccare.entity.enums.Role;
import com.tus.cliniccare.entity.enums.TimeSlotStatus;
import com.tus.cliniccare.exception.BadRequestException;
import com.tus.cliniccare.exception.ConflictException;
import com.tus.cliniccare.exception.ResourceNotFoundException;
import com.tus.cliniccare.repository.AppointmentRepository;
import com.tus.cliniccare.repository.DoctorRepository;
import com.tus.cliniccare.repository.DoctorServiceRepository;
import com.tus.cliniccare.repository.ServiceRepository;
import com.tus.cliniccare.repository.TimeSlotRepository;
import com.tus.cliniccare.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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

    public List<Appointment> getAppointmentsByPatient(String patientEmail) {
        User patient = getUserByEmail(patientEmail);
        if (patient.getRole() != Role.PATIENT) {
            throw new BadRequestException("Only patients can view patient appointment history.");
        }
        Long patientId = patient.getId();
        return appointmentRepository.findByPatientId(patientId);
    }

    public List<Appointment> getAppointmentsByDoctor(Long doctorId) {
        return appointmentRepository.findByDoctorId(doctorId);
    }

    public List<Appointment> getAppointmentsByDoctor(String doctorEmail) {
        User doctorUser = getUserByEmail(doctorEmail);
        if (doctorUser.getRole() != Role.DOCTOR) {
            throw new BadRequestException("Only doctors can view doctor appointment history.");
        }

        Doctor doctor = doctorRepository.findByUserId(doctorUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Doctor profile not found for the authenticated user."));

        return appointmentRepository.findByDoctorId(doctor.getId());
    }

    public List<Appointment> getAppointmentsByDoctorAndStatus(String doctorEmail, AppointmentStatus status) {
        return getAppointmentsByDoctor(doctorEmail).stream()
                .filter(appointment -> appointment.getStatus() == status)
                .toList();
    }

    public List<Appointment> getAppointmentsByStatus(AppointmentStatus status) {
        return appointmentRepository.findByStatus(status);
    }

    public long getTotalAppointmentsCount() {
        return appointmentRepository.count();
    }

    public long getAppointmentsCountByStatus(AppointmentStatus status) {
        return appointmentRepository.countByStatus(status);
    }

    public boolean isTimeSlotBooked(Long timeSlotId) {
        return appointmentRepository.existsByTimeSlotId(timeSlotId);
    }

    @Transactional
    public Appointment bookAppointment(
            String patientEmail,
            Long doctorId,
            Long serviceId,
            Long timeSlotId,
            String patientNote
    ) {
        User patient = getUserByEmail(patientEmail);
        if (patient.getRole() != Role.PATIENT) {
            throw new BadRequestException("Only patients can book appointments.");
        }

        if (isTimeSlotBooked(timeSlotId)) {
            throw new ConflictException("Selected time slot is already booked.");
        }

        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found."));
        ServiceEntity service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Service not found."));
        TimeSlot timeSlot = timeSlotRepository.findById(timeSlotId)
                .orElseThrow(() -> new ResourceNotFoundException("Time slot not found."));

        if (!doctorId.equals(timeSlot.getDoctor().getId())) {
            throw new BadRequestException("Time slot does not belong to the selected doctor.");
        }

        if (!doctorServiceRepository.existsByDoctorIdAndServiceId(doctorId, serviceId)) {
            throw new BadRequestException("Selected doctor does not provide the selected service.");
        }

        if (timeSlot.getStatus() != TimeSlotStatus.AVAILABLE) {
            throw new ConflictException("Time slot is not available.");
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
    public Appointment confirmAppointment(Long appointmentId, String actorEmail, boolean isAdmin) {
        Appointment appointment = getAppointmentById(appointmentId);
        validateDoctorOwnership(appointment, actorEmail, isAdmin);
        validateTransition(appointment.getStatus(), AppointmentStatus.CONFIRMED);
        appointment.setStatus(AppointmentStatus.CONFIRMED);
        return appointmentRepository.save(appointment);
    }

    @Transactional
    public Appointment rejectAppointment(Long appointmentId, String actorEmail, boolean isAdmin) {
        Appointment appointment = getAppointmentById(appointmentId);
        validateDoctorOwnership(appointment, actorEmail, isAdmin);
        validateTransition(appointment.getStatus(), AppointmentStatus.REJECTED);
        appointment.setStatus(AppointmentStatus.REJECTED);

        if (appointment.getTimeSlot() != null) {
            appointment.getTimeSlot().setStatus(TimeSlotStatus.AVAILABLE);
            timeSlotRepository.save(appointment.getTimeSlot());
        }

        return appointmentRepository.save(appointment);
    }

    @Transactional
    public Appointment completeAppointment(Long appointmentId, String actorEmail, boolean isAdmin) {
        Appointment appointment = getAppointmentById(appointmentId);
        validateDoctorOwnership(appointment, actorEmail, isAdmin);
        validateTransition(appointment.getStatus(), AppointmentStatus.COMPLETED);

        LocalDateTime slotEndTime = appointment.getTimeSlot().getEndTime();
        if (slotEndTime != null && LocalDateTime.now().isBefore(slotEndTime)) {
            throw new BadRequestException("Appointment can be completed only after the scheduled slot ends.");
        }

        appointment.setStatus(AppointmentStatus.COMPLETED);
        return appointmentRepository.save(appointment);
    }

    @Transactional
    public Appointment cancelAppointmentByPatient(Long appointmentId, String patientEmail) {
        User patient = getUserByEmail(patientEmail);
        if (patient.getRole() != Role.PATIENT) {
            throw new BadRequestException("Only patients can cancel appointments.");
        }

        Appointment appointment = getAppointmentById(appointmentId);
        if (!appointment.getPatient().getId().equals(patient.getId())) {
            throw new BadRequestException("You can only cancel your own appointments.");
        }

        AppointmentStatus status = appointment.getStatus();
        if (status != AppointmentStatus.PENDING && status != AppointmentStatus.CONFIRMED) {
            throw new BadRequestException("Only pending or confirmed appointments can be cancelled.");
        }

        LocalDateTime slotStartTime = appointment.getTimeSlot() == null
                ? null
                : appointment.getTimeSlot().getStartTime();
        if (slotStartTime != null && !LocalDateTime.now().isBefore(slotStartTime)) {
            throw new BadRequestException("Past or ongoing appointments cannot be cancelled.");
        }

        appointment.setStatus(AppointmentStatus.CANCELLED);

        if (appointment.getTimeSlot() != null) {
            appointment.getTimeSlot().setStatus(TimeSlotStatus.AVAILABLE);
            timeSlotRepository.save(appointment.getTimeSlot());
        }

        return appointmentRepository.save(appointment);
    }

    private Appointment getAppointmentById(Long appointmentId) {
        return appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found."));
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));
    }

    private void validateDoctorOwnership(Appointment appointment, String actorEmail, boolean isAdmin) {
        if (isAdmin) {
            return;
        }

        User actor = getUserByEmail(actorEmail);
        if (actor.getRole() != Role.DOCTOR) {
            throw new BadRequestException("Only a doctor can perform this action.");
        }

        Doctor doctor = doctorRepository.findByUserId(actor.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Doctor profile not found for the authenticated user."));

        if (!doctor.getId().equals(appointment.getDoctor().getId())) {
            throw new BadRequestException("You can only manage appointments assigned to you.");
        }
    }

    private void validateTransition(AppointmentStatus currentStatus, AppointmentStatus targetStatus) {
        boolean allowed =
                (currentStatus == AppointmentStatus.PENDING && targetStatus == AppointmentStatus.CONFIRMED)
                        || (currentStatus == AppointmentStatus.PENDING && targetStatus == AppointmentStatus.REJECTED)
                        || (currentStatus == AppointmentStatus.CONFIRMED && targetStatus == AppointmentStatus.COMPLETED);

        if (!allowed) {
            throw new BadRequestException(
                    "Invalid appointment status transition: " + currentStatus + " -> " + targetStatus
            );
        }
    }
}
