package com.tus.cliniccare.repository;

import com.tus.cliniccare.entity.Appointment;
import com.tus.cliniccare.entity.Doctor;
import com.tus.cliniccare.entity.ServiceEntity;
import com.tus.cliniccare.entity.TimeSlot;
import com.tus.cliniccare.entity.User;
import com.tus.cliniccare.entity.enums.AppointmentStatus;
import com.tus.cliniccare.entity.enums.Role;
import com.tus.cliniccare.entity.enums.TimeSlotStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:appointment_repository_test;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.sql.init.mode=never",
        "app.jwt.secret=Y2xpbmljY2FyZS1hY2FkZW1pYy1qd3Qtc2VjcmV0LWtleS1mb3ItcHJvamVjdC1kZW1vLXNpZ25pbmctMzJieXRlcw=="
})
@Transactional
class AppointmentRepositoryTest {

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private ServiceRepository serviceRepository;

    @Autowired
    private TimeSlotRepository timeSlotRepository;

    @Test
    void existsByTimeSlotId_shouldReturnTrue_whenSlotBooked() {
        TestData data = createTestData("patient.booked@cliniccare.com");
        appointmentRepository.saveAndFlush(newAppointment(data.patient, data.doctor, data.service, data.slot));

        boolean result = appointmentRepository.existsByTimeSlotId(data.slot.getId());

        assertTrue(result);
    }

    @Test
    void existsByTimeSlotId_shouldReturnFalse_whenSlotNotBooked() {
        TestData data = createTestData("patient.free@cliniccare.com");

        boolean result = appointmentRepository.existsByTimeSlotId(data.slot.getId());

        assertFalse(result);
    }

    @Test
    void save_shouldFail_whenTwoAppointmentsUseSameSlot() {
        TestData data = createTestData("patient.a@cliniccare.com");
        appointmentRepository.saveAndFlush(newAppointment(data.patient, data.doctor, data.service, data.slot));

        User secondPatient = userRepository.saveAndFlush(newUser("patient.b@cliniccare.com", Role.PATIENT));

        assertThrows(
                DataIntegrityViolationException.class,
                () -> appointmentRepository.saveAndFlush(
                        newAppointment(secondPatient, data.doctor, data.service, data.slot)
                )
        );
    }

    private TestData createTestData(String patientEmail) {
        User patient = userRepository.saveAndFlush(newUser(patientEmail, Role.PATIENT));
        User doctorUser = userRepository.saveAndFlush(newUser("doctor.repo@cliniccare.com", Role.DOCTOR));

        Doctor doctor = new Doctor();
        doctor.setUser(doctorUser);
        doctor.setSpecialization("General");
        doctor = doctorRepository.saveAndFlush(doctor);

        ServiceEntity service = new ServiceEntity();
        service.setName("General Consultation");
        service.setDescription("General check");
        service.setDurationMinutes(30);
        service.setIsEnabled(true);
        service = serviceRepository.saveAndFlush(service);

        TimeSlot slot = new TimeSlot();
        slot.setDoctor(doctor);
        slot.setStartTime(LocalDateTime.now().plusDays(1).withHour(10).withMinute(0).withSecond(0).withNano(0));
        slot.setEndTime(LocalDateTime.now().plusDays(1).withHour(10).withMinute(30).withSecond(0).withNano(0));
        slot.setStatus(TimeSlotStatus.AVAILABLE);
        slot = timeSlotRepository.saveAndFlush(slot);

        return new TestData(patient, doctor, service, slot);
    }

    private Appointment newAppointment(User patient, Doctor doctor, ServiceEntity service, TimeSlot slot) {
        Appointment appointment = new Appointment();
        appointment.setPatient(patient);
        appointment.setDoctor(doctor);
        appointment.setService(service);
        appointment.setTimeSlot(slot);
        appointment.setStatus(AppointmentStatus.PENDING);
        return appointment;
    }

    private User newUser(String email, Role role) {
        User user = new User();
        user.setFirstName("Test");
        user.setLastName("User");
        user.setEmail(email);
        user.setPassword("encoded-password");
        user.setPhoneNumber("9999999999");
        user.setRole(role);
        return user;
    }

    private record TestData(User patient, Doctor doctor, ServiceEntity service, TimeSlot slot) {
    }
}
