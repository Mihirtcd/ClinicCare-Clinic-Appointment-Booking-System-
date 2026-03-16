package com.tus.cliniccare.integration;

import com.tus.cliniccare.entity.Appointment;
import com.tus.cliniccare.entity.Doctor;
import com.tus.cliniccare.entity.DoctorService;
import com.tus.cliniccare.entity.ServiceEntity;
import com.tus.cliniccare.entity.TimeSlot;
import com.tus.cliniccare.entity.User;
import com.tus.cliniccare.entity.enums.AppointmentStatus;
import com.tus.cliniccare.entity.enums.Role;
import com.tus.cliniccare.entity.enums.TimeSlotStatus;
import com.tus.cliniccare.exception.ConflictException;
import com.tus.cliniccare.repository.AppointmentRepository;
import com.tus.cliniccare.repository.DoctorRepository;
import com.tus.cliniccare.repository.DoctorServiceRepository;
import com.tus.cliniccare.repository.ServiceRepository;
import com.tus.cliniccare.repository.TimeSlotRepository;
import com.tus.cliniccare.repository.UserRepository;
import com.tus.cliniccare.service.AppointmentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:appointment_service_it;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.sql.init.mode=never",
        "app.jwt.secret=Y2xpbmljY2FyZS1hY2FkZW1pYy1qd3Qtc2VjcmV0LWtleS1mb3ItcHJvamVjdC1kZW1vLXNpZ25pbmctMzJieXRlcw=="
})
@Transactional
class AppointmentServiceIntegrationTest {

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private ServiceRepository serviceRepository;

    @Autowired
    private DoctorServiceRepository doctorServiceRepository;

    @Autowired
    private TimeSlotRepository timeSlotRepository;

    @Test
    void bookAppointment_shouldPersistPendingAndReserveSlot() {
        User patient = saveUser("patient.it@cliniccare.com", Role.PATIENT);
        User doctorUser = saveUser("doctor.it@cliniccare.com", Role.DOCTOR);
        Doctor doctor = saveDoctor(doctorUser);
        ServiceEntity service = saveService("General Consultation");
        mapDoctorService(doctor, service);
        TimeSlot slot = saveSlot(doctor, TimeSlotStatus.AVAILABLE);

        Appointment booked = appointmentService.bookAppointment(
                patient.getEmail(),
                doctor.getId(),
                service.getId(),
                slot.getId(),
                "Please confirm"
        );

        Appointment persisted = appointmentRepository.findById(booked.getId()).orElseThrow();
        TimeSlot persistedSlot = timeSlotRepository.findById(slot.getId()).orElseThrow();

        assertEquals(AppointmentStatus.PENDING, persisted.getStatus());
        assertEquals(TimeSlotStatus.RESERVED, persistedSlot.getStatus());
    }

    @Test
    void bookAppointment_shouldThrowConflict_whenSlotAlreadyBooked() {
        User patient = saveUser("patient.one@cliniccare.com", Role.PATIENT);
        User secondPatient = saveUser("patient.two@cliniccare.com", Role.PATIENT);
        User doctorUser = saveUser("doctor.two@cliniccare.com", Role.DOCTOR);
        Doctor doctor = saveDoctor(doctorUser);
        ServiceEntity service = saveService("Dental Checkup");
        mapDoctorService(doctor, service);
        TimeSlot slot = saveSlot(doctor, TimeSlotStatus.AVAILABLE);

        appointmentService.bookAppointment(patient.getEmail(), doctor.getId(), service.getId(), slot.getId(), null);

        assertThrows(
                ConflictException.class,
                () -> appointmentService.bookAppointment(
                        secondPatient.getEmail(),
                        doctor.getId(),
                        service.getId(),
                        slot.getId(),
                        null
                )
        );
    }

    private User saveUser(String email, Role role) {
        User user = new User();
        user.setFirstName("Test");
        user.setLastName("User");
        user.setEmail(email);
        user.setPassword("encoded-password");
        user.setPhoneNumber("9999999999");
        user.setRole(role);
        return userRepository.save(user);
    }

    private Doctor saveDoctor(User doctorUser) {
        Doctor doctor = new Doctor();
        doctor.setUser(doctorUser);
        doctor.setSpecialization("General");
        return doctorRepository.save(doctor);
    }

    private ServiceEntity saveService(String name) {
        ServiceEntity service = new ServiceEntity();
        service.setName(name);
        service.setDescription("Service description");
        service.setDurationMinutes(30);
        service.setIsEnabled(true);
        return serviceRepository.save(service);
    }

    private void mapDoctorService(Doctor doctor, ServiceEntity service) {
        DoctorService mapping = new DoctorService();
        mapping.setDoctor(doctor);
        mapping.setService(service);
        doctorServiceRepository.save(mapping);
    }

    private TimeSlot saveSlot(Doctor doctor, TimeSlotStatus status) {
        TimeSlot slot = new TimeSlot();
        slot.setDoctor(doctor);
        slot.setStartTime(LocalDateTime.now().plusDays(1).withHour(10).withMinute(0).withSecond(0).withNano(0));
        slot.setEndTime(LocalDateTime.now().plusDays(1).withHour(10).withMinute(30).withSecond(0).withNano(0));
        slot.setStatus(status);
        return timeSlotRepository.save(slot);
    }
}
