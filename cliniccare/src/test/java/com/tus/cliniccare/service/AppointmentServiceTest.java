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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceTest {

    private static final Long PATIENT_ID = 1L;
    private static final Long DOCTOR_ID = 2L;
    private static final Long OTHER_DOCTOR_ID = 22L;
    private static final Long SERVICE_ID = 3L;
    private static final Long SLOT_ID = 4L;
    private static final Long APPOINTMENT_ID = 10L;

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private DoctorRepository doctorRepository;

    @Mock
    private DoctorServiceRepository doctorServiceRepository;

    @Mock
    private ServiceRepository serviceRepository;

    @Mock
    private TimeSlotRepository timeSlotRepository;

    @InjectMocks
    private AppointmentService appointmentService;

    @Test
    void bookAppointment_shouldSucceed_whenInputsAreValid() {
        User patient = patientUser();
        Doctor doctor = doctorEntity();
        ServiceEntity service = serviceEntity();
        TimeSlot slot = slotEntity(doctor, TimeSlotStatus.AVAILABLE);

        when(userRepository.findByEmail(patient.getEmail())).thenReturn(Optional.of(patient));
        when(appointmentRepository.existsByTimeSlotId(SLOT_ID)).thenReturn(false);
        when(doctorRepository.findById(DOCTOR_ID)).thenReturn(Optional.of(doctor));
        when(serviceRepository.findById(SERVICE_ID)).thenReturn(Optional.of(service));
        when(timeSlotRepository.findById(SLOT_ID)).thenReturn(Optional.of(slot));
        when(doctorServiceRepository.existsByDoctorIdAndServiceId(DOCTOR_ID, SERVICE_ID)).thenReturn(true);
        when(timeSlotRepository.save(any(TimeSlot.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Appointment result = appointmentService.bookAppointment(
                patient.getEmail(),
                DOCTOR_ID,
                SERVICE_ID,
                SLOT_ID,
                "Need consultation"
        );

        assertEquals(AppointmentStatus.PENDING, result.getStatus());
        assertEquals(TimeSlotStatus.RESERVED, slot.getStatus());
        assertEquals(patient.getId(), result.getPatient().getId());
        assertEquals(doctor.getId(), result.getDoctor().getId());
        assertEquals(service.getId(), result.getService().getId());
        verify(timeSlotRepository).save(slot);
        verify(appointmentRepository).save(any(Appointment.class));
    }

    @Test
    void bookAppointment_shouldThrowConflict_whenSlotAlreadyBooked() {
        User patient = patientUser();
        when(userRepository.findByEmail(patient.getEmail())).thenReturn(Optional.of(patient));
        when(appointmentRepository.existsByTimeSlotId(SLOT_ID)).thenReturn(true);

        assertThrows(
                ConflictException.class,
                () -> appointmentService.bookAppointment(patient.getEmail(), DOCTOR_ID, SERVICE_ID, SLOT_ID, null)
        );

        verifyNoInteractions(doctorRepository, serviceRepository, timeSlotRepository, doctorServiceRepository);
    }

    @Test
    void bookAppointment_shouldThrowConflict_whenSlotNotAvailable() {
        User patient = patientUser();
        Doctor doctor = doctorEntity();
        ServiceEntity service = serviceEntity();
        TimeSlot slot = slotEntity(doctor, TimeSlotStatus.RESERVED);

        when(userRepository.findByEmail(patient.getEmail())).thenReturn(Optional.of(patient));
        when(appointmentRepository.existsByTimeSlotId(SLOT_ID)).thenReturn(false);
        when(doctorRepository.findById(DOCTOR_ID)).thenReturn(Optional.of(doctor));
        when(serviceRepository.findById(SERVICE_ID)).thenReturn(Optional.of(service));
        when(timeSlotRepository.findById(SLOT_ID)).thenReturn(Optional.of(slot));
        when(doctorServiceRepository.existsByDoctorIdAndServiceId(DOCTOR_ID, SERVICE_ID)).thenReturn(true);

        assertThrows(
                ConflictException.class,
                () -> appointmentService.bookAppointment(patient.getEmail(), DOCTOR_ID, SERVICE_ID, SLOT_ID, null)
        );

        verify(appointmentRepository, never()).save(any(Appointment.class));
    }

    @Test
    void bookAppointment_shouldThrowBadRequest_whenDoctorDoesNotProvideService() {
        User patient = patientUser();
        Doctor doctor = doctorEntity();
        ServiceEntity service = serviceEntity();
        TimeSlot slot = slotEntity(doctor, TimeSlotStatus.AVAILABLE);

        when(userRepository.findByEmail(patient.getEmail())).thenReturn(Optional.of(patient));
        when(appointmentRepository.existsByTimeSlotId(SLOT_ID)).thenReturn(false);
        when(doctorRepository.findById(DOCTOR_ID)).thenReturn(Optional.of(doctor));
        when(serviceRepository.findById(SERVICE_ID)).thenReturn(Optional.of(service));
        when(timeSlotRepository.findById(SLOT_ID)).thenReturn(Optional.of(slot));
        when(doctorServiceRepository.existsByDoctorIdAndServiceId(DOCTOR_ID, SERVICE_ID)).thenReturn(false);

        assertThrows(
                BadRequestException.class,
                () -> appointmentService.bookAppointment(patient.getEmail(), DOCTOR_ID, SERVICE_ID, SLOT_ID, null)
        );

        verify(appointmentRepository, never()).save(any(Appointment.class));
    }

    @Test
    void bookAppointment_shouldThrowBadRequest_whenUserRoleIsNotPatient() {
        User doctorUser = doctorUser(100L, "doctor.user@cliniccare.com");

        when(userRepository.findByEmail(doctorUser.getEmail())).thenReturn(Optional.of(doctorUser));

        assertThrows(
                BadRequestException.class,
                () -> appointmentService.bookAppointment(doctorUser.getEmail(), DOCTOR_ID, SERVICE_ID, SLOT_ID, null)
        );

        verifyNoInteractions(doctorRepository, serviceRepository, timeSlotRepository, doctorServiceRepository, appointmentRepository);
    }

    @Test
    void bookAppointment_shouldThrowResourceNotFound_whenUserDoesNotExist() {
        when(userRepository.findByEmail("missing@cliniccare.com")).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> appointmentService.bookAppointment("missing@cliniccare.com", DOCTOR_ID, SERVICE_ID, SLOT_ID, null)
        );
    }

    @Test
    void bookAppointment_shouldThrowResourceNotFound_whenDoctorDoesNotExist() {
        User patient = patientUser();
        when(userRepository.findByEmail(patient.getEmail())).thenReturn(Optional.of(patient));
        when(appointmentRepository.existsByTimeSlotId(SLOT_ID)).thenReturn(false);
        when(doctorRepository.findById(DOCTOR_ID)).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> appointmentService.bookAppointment(patient.getEmail(), DOCTOR_ID, SERVICE_ID, SLOT_ID, null)
        );
    }

    @Test
    void bookAppointment_shouldThrowResourceNotFound_whenServiceDoesNotExist() {
        User patient = patientUser();
        Doctor doctor = doctorEntity();

        when(userRepository.findByEmail(patient.getEmail())).thenReturn(Optional.of(patient));
        when(appointmentRepository.existsByTimeSlotId(SLOT_ID)).thenReturn(false);
        when(doctorRepository.findById(DOCTOR_ID)).thenReturn(Optional.of(doctor));
        when(serviceRepository.findById(SERVICE_ID)).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> appointmentService.bookAppointment(patient.getEmail(), DOCTOR_ID, SERVICE_ID, SLOT_ID, null)
        );
    }

    @Test
    void bookAppointment_shouldThrowResourceNotFound_whenSlotDoesNotExist() {
        User patient = patientUser();
        Doctor doctor = doctorEntity();
        ServiceEntity service = serviceEntity();

        when(userRepository.findByEmail(patient.getEmail())).thenReturn(Optional.of(patient));
        when(appointmentRepository.existsByTimeSlotId(SLOT_ID)).thenReturn(false);
        when(doctorRepository.findById(DOCTOR_ID)).thenReturn(Optional.of(doctor));
        when(serviceRepository.findById(SERVICE_ID)).thenReturn(Optional.of(service));
        when(timeSlotRepository.findById(SLOT_ID)).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> appointmentService.bookAppointment(patient.getEmail(), DOCTOR_ID, SERVICE_ID, SLOT_ID, null)
        );
    }

    @Test
    void confirmAppointment_shouldSucceed_forPendingToConfirmedTransition() {
        Appointment appointment = appointmentEntity(AppointmentStatus.PENDING);

        when(appointmentRepository.findById(APPOINTMENT_ID)).thenReturn(Optional.of(appointment));
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Appointment result = appointmentService.confirmAppointment(APPOINTMENT_ID, "admin@cliniccare.com", true);

        assertEquals(AppointmentStatus.CONFIRMED, result.getStatus());
    }

    @Test
    void confirmAppointment_shouldSucceed_whenDoctorOwnsAppointment() {
        Appointment appointment = appointmentEntity(AppointmentStatus.PENDING);
        User doctorUser = appointment.getDoctor().getUser();

        when(appointmentRepository.findById(APPOINTMENT_ID)).thenReturn(Optional.of(appointment));
        when(userRepository.findByEmail(doctorUser.getEmail())).thenReturn(Optional.of(doctorUser));
        when(doctorRepository.findByUserId(doctorUser.getId())).thenReturn(Optional.of(appointment.getDoctor()));
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Appointment result = appointmentService.confirmAppointment(APPOINTMENT_ID, doctorUser.getEmail(), false);

        assertEquals(AppointmentStatus.CONFIRMED, result.getStatus());
    }

    @Test
    void confirmAppointment_shouldThrowResourceNotFound_whenDoctorProfileMissingForActor() {
        Appointment appointment = appointmentEntity(AppointmentStatus.PENDING);
        User doctorUser = appointment.getDoctor().getUser();

        when(appointmentRepository.findById(APPOINTMENT_ID)).thenReturn(Optional.of(appointment));
        when(userRepository.findByEmail(doctorUser.getEmail())).thenReturn(Optional.of(doctorUser));
        when(doctorRepository.findByUserId(doctorUser.getId())).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> appointmentService.confirmAppointment(APPOINTMENT_ID, doctorUser.getEmail(), false)
        );
    }

    @Test
    void confirmAppointment_shouldThrowBadRequest_whenDoctorDoesNotOwnAppointment() {
        Appointment appointment = appointmentEntity(AppointmentStatus.PENDING);
        User otherDoctorUser = doctorUser(999L, "other.doctor@cliniccare.com");
        Doctor otherDoctor = new Doctor();
        otherDoctor.setId(OTHER_DOCTOR_ID);
        otherDoctor.setUser(otherDoctorUser);

        when(appointmentRepository.findById(APPOINTMENT_ID)).thenReturn(Optional.of(appointment));
        when(userRepository.findByEmail(otherDoctorUser.getEmail())).thenReturn(Optional.of(otherDoctorUser));
        when(doctorRepository.findByUserId(otherDoctorUser.getId())).thenReturn(Optional.of(otherDoctor));

        assertThrows(
                BadRequestException.class,
                () -> appointmentService.confirmAppointment(APPOINTMENT_ID, otherDoctorUser.getEmail(), false)
        );
    }

    @Test
    void rejectAppointment_shouldThrowBadRequest_whenDoctorDoesNotOwnAppointment() {
        Appointment appointment = appointmentEntity(AppointmentStatus.PENDING);
        User otherDoctorUser = doctorUser(999L, "other.doctor@cliniccare.com");
        Doctor otherDoctor = new Doctor();
        otherDoctor.setId(OTHER_DOCTOR_ID);
        otherDoctor.setUser(otherDoctorUser);

        when(appointmentRepository.findById(APPOINTMENT_ID)).thenReturn(Optional.of(appointment));
        when(userRepository.findByEmail(otherDoctorUser.getEmail())).thenReturn(Optional.of(otherDoctorUser));
        when(doctorRepository.findByUserId(otherDoctorUser.getId())).thenReturn(Optional.of(otherDoctor));

        assertThrows(
                BadRequestException.class,
                () -> appointmentService.rejectAppointment(APPOINTMENT_ID, otherDoctorUser.getEmail(), false)
        );
    }

    @Test
    void rejectAppointment_shouldSucceed_forPendingToRejectedTransition() {
        Appointment appointment = appointmentEntity(AppointmentStatus.PENDING);
        appointment.getTimeSlot().setStatus(TimeSlotStatus.RESERVED);

        when(appointmentRepository.findById(APPOINTMENT_ID)).thenReturn(Optional.of(appointment));
        when(timeSlotRepository.save(any(TimeSlot.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Appointment result = appointmentService.rejectAppointment(APPOINTMENT_ID, "admin@cliniccare.com", true);

        assertEquals(AppointmentStatus.REJECTED, result.getStatus());
        assertEquals(TimeSlotStatus.AVAILABLE, appointment.getTimeSlot().getStatus());
        verify(timeSlotRepository).save(appointment.getTimeSlot());
    }

    @Test
    void completeAppointment_shouldSucceed_forConfirmedToCompletedTransition() {
        Appointment appointment = appointmentEntity(AppointmentStatus.CONFIRMED);
        appointment.getTimeSlot().setEndTime(LocalDateTime.now().minusMinutes(10));

        when(appointmentRepository.findById(APPOINTMENT_ID)).thenReturn(Optional.of(appointment));
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Appointment result = appointmentService.completeAppointment(APPOINTMENT_ID, "admin@cliniccare.com", true);

        assertEquals(AppointmentStatus.COMPLETED, result.getStatus());
    }

    @Test
    void completeAppointment_shouldThrowBadRequest_whenDoctorDoesNotOwnAppointment() {
        Appointment appointment = appointmentEntity(AppointmentStatus.CONFIRMED);
        appointment.getTimeSlot().setEndTime(LocalDateTime.now().minusMinutes(5));
        User otherDoctorUser = doctorUser(999L, "other.doctor@cliniccare.com");
        Doctor otherDoctor = new Doctor();
        otherDoctor.setId(OTHER_DOCTOR_ID);
        otherDoctor.setUser(otherDoctorUser);

        when(appointmentRepository.findById(APPOINTMENT_ID)).thenReturn(Optional.of(appointment));
        when(userRepository.findByEmail(otherDoctorUser.getEmail())).thenReturn(Optional.of(otherDoctorUser));
        when(doctorRepository.findByUserId(otherDoctorUser.getId())).thenReturn(Optional.of(otherDoctor));

        assertThrows(
                BadRequestException.class,
                () -> appointmentService.completeAppointment(APPOINTMENT_ID, otherDoctorUser.getEmail(), false)
        );
    }

    @Test
    void completeAppointment_shouldThrowBadRequest_whenActorIsNotDoctorAndNotAdmin() {
        Appointment appointment = appointmentEntity(AppointmentStatus.CONFIRMED);
        appointment.getTimeSlot().setEndTime(LocalDateTime.now().minusMinutes(5));
        User patientActor = patientUser();

        when(appointmentRepository.findById(APPOINTMENT_ID)).thenReturn(Optional.of(appointment));
        when(userRepository.findByEmail(patientActor.getEmail())).thenReturn(Optional.of(patientActor));

        assertThrows(
                BadRequestException.class,
                () -> appointmentService.completeAppointment(APPOINTMENT_ID, patientActor.getEmail(), false)
        );
    }

    @Test
    void completeAppointment_shouldThrowBadRequest_whenSlotHasNotEndedYet() {
        Appointment appointment = appointmentEntity(AppointmentStatus.CONFIRMED);
        appointment.getTimeSlot().setEndTime(LocalDateTime.now().plusMinutes(10));
        when(appointmentRepository.findById(APPOINTMENT_ID)).thenReturn(Optional.of(appointment));

        assertThrows(
                BadRequestException.class,
                () -> appointmentService.completeAppointment(APPOINTMENT_ID, "admin@cliniccare.com", true)
        );
    }

    @Test
    void confirmAppointment_shouldThrowBadRequest_forInvalidTransition() {
        Appointment appointment = appointmentEntity(AppointmentStatus.CONFIRMED);
        when(appointmentRepository.findById(APPOINTMENT_ID)).thenReturn(Optional.of(appointment));

        assertThrows(
                BadRequestException.class,
                () -> appointmentService.confirmAppointment(APPOINTMENT_ID, "admin@cliniccare.com", true)
        );
    }

    @Test
    void rejectAppointment_shouldThrowBadRequest_forInvalidTransition() {
        Appointment appointment = appointmentEntity(AppointmentStatus.COMPLETED);
        when(appointmentRepository.findById(APPOINTMENT_ID)).thenReturn(Optional.of(appointment));

        assertThrows(
                BadRequestException.class,
                () -> appointmentService.rejectAppointment(APPOINTMENT_ID, "admin@cliniccare.com", true)
        );
    }

    @Test
    void completeAppointment_shouldThrowBadRequest_forInvalidTransition() {
        Appointment appointment = appointmentEntity(AppointmentStatus.PENDING);
        appointment.getTimeSlot().setEndTime(LocalDateTime.now().minusMinutes(10));
        when(appointmentRepository.findById(APPOINTMENT_ID)).thenReturn(Optional.of(appointment));

        assertThrows(
                BadRequestException.class,
                () -> appointmentService.completeAppointment(APPOINTMENT_ID, "admin@cliniccare.com", true)
        );
    }

    @Test
    void confirmAppointment_shouldThrowResourceNotFound_whenAppointmentDoesNotExist() {
        when(appointmentRepository.findById(APPOINTMENT_ID)).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> appointmentService.confirmAppointment(APPOINTMENT_ID, "admin@cliniccare.com", true)
        );
    }

    @Test
    void cancelAppointmentByPatient_shouldSucceed_forPendingAppointmentBeforeSlotStart() {
        User patient = patientUser();
        Appointment appointment = appointmentEntity(AppointmentStatus.PENDING);
        appointment.getTimeSlot().setStatus(TimeSlotStatus.RESERVED);
        appointment.getTimeSlot().setStartTime(LocalDateTime.now().plusHours(1));

        when(userRepository.findByEmail(patient.getEmail())).thenReturn(Optional.of(patient));
        when(appointmentRepository.findById(APPOINTMENT_ID)).thenReturn(Optional.of(appointment));
        when(timeSlotRepository.save(any(TimeSlot.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Appointment result = appointmentService.cancelAppointmentByPatient(APPOINTMENT_ID, patient.getEmail());

        assertEquals(AppointmentStatus.CANCELLED, result.getStatus());
        assertEquals(TimeSlotStatus.AVAILABLE, result.getTimeSlot().getStatus());
    }

    @Test
    void cancelAppointmentByPatient_shouldSucceed_forConfirmedAppointmentBeforeSlotStart() {
        User patient = patientUser();
        Appointment appointment = appointmentEntity(AppointmentStatus.CONFIRMED);
        appointment.getTimeSlot().setStatus(TimeSlotStatus.RESERVED);
        appointment.getTimeSlot().setStartTime(LocalDateTime.now().plusHours(2));

        when(userRepository.findByEmail(patient.getEmail())).thenReturn(Optional.of(patient));
        when(appointmentRepository.findById(APPOINTMENT_ID)).thenReturn(Optional.of(appointment));
        when(timeSlotRepository.save(any(TimeSlot.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Appointment result = appointmentService.cancelAppointmentByPatient(APPOINTMENT_ID, patient.getEmail());

        assertEquals(AppointmentStatus.CANCELLED, result.getStatus());
    }

    @Test
    void cancelAppointmentByPatient_shouldThrowBadRequest_whenPatientCancelsAnotherPatientsAppointment() {
        User authenticatedPatient = patientUser();
        User anotherPatient = new User();
        anotherPatient.setId(999L);
        anotherPatient.setRole(Role.PATIENT);
        anotherPatient.setEmail("another.patient@cliniccare.com");

        Appointment appointment = appointmentEntity(AppointmentStatus.PENDING);
        appointment.setPatient(anotherPatient);
        appointment.getTimeSlot().setStartTime(LocalDateTime.now().plusHours(1));

        when(userRepository.findByEmail(authenticatedPatient.getEmail())).thenReturn(Optional.of(authenticatedPatient));
        when(appointmentRepository.findById(APPOINTMENT_ID)).thenReturn(Optional.of(appointment));

        assertThrows(
                BadRequestException.class,
                () -> appointmentService.cancelAppointmentByPatient(APPOINTMENT_ID, authenticatedPatient.getEmail())
        );
    }

    @Test
    void cancelAppointmentByPatient_shouldThrowBadRequest_forInvalidStatus() {
        User patient = patientUser();
        Appointment appointment = appointmentEntity(AppointmentStatus.REJECTED);
        appointment.getTimeSlot().setStartTime(LocalDateTime.now().plusHours(1));

        when(userRepository.findByEmail(patient.getEmail())).thenReturn(Optional.of(patient));
        when(appointmentRepository.findById(APPOINTMENT_ID)).thenReturn(Optional.of(appointment));

        assertThrows(
                BadRequestException.class,
                () -> appointmentService.cancelAppointmentByPatient(APPOINTMENT_ID, patient.getEmail())
        );
    }

    @Test
    void cancelAppointmentByPatient_shouldThrowBadRequest_afterSlotStartTime() {
        User patient = patientUser();
        Appointment appointment = appointmentEntity(AppointmentStatus.PENDING);
        appointment.getTimeSlot().setStartTime(LocalDateTime.now().minusMinutes(5));

        when(userRepository.findByEmail(patient.getEmail())).thenReturn(Optional.of(patient));
        when(appointmentRepository.findById(APPOINTMENT_ID)).thenReturn(Optional.of(appointment));

        assertThrows(
                BadRequestException.class,
                () -> appointmentService.cancelAppointmentByPatient(APPOINTMENT_ID, patient.getEmail())
        );
    }

    @Test
    void cancelAppointmentByPatient_shouldThrowResourceNotFound_whenPatientEmailNotFound() {
        when(userRepository.findByEmail("missing@cliniccare.com")).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> appointmentService.cancelAppointmentByPatient(APPOINTMENT_ID, "missing@cliniccare.com")
        );
    }

    @Test
    void cancelAppointmentByPatient_shouldThrowResourceNotFound_whenAppointmentNotFound() {
        User patient = patientUser();
        when(userRepository.findByEmail(patient.getEmail())).thenReturn(Optional.of(patient));
        when(appointmentRepository.findById(APPOINTMENT_ID)).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> appointmentService.cancelAppointmentByPatient(APPOINTMENT_ID, patient.getEmail())
        );
    }

    @Test
    void getAppointmentsByPatient_shouldThrowBadRequest_whenUserIsNotPatient() {
        User doctorUser = doctorUser(88L, "doctor.for.patient.history@cliniccare.com");
        when(userRepository.findByEmail(doctorUser.getEmail())).thenReturn(Optional.of(doctorUser));

        assertThrows(
                BadRequestException.class,
                () -> appointmentService.getAppointmentsByPatient(doctorUser.getEmail())
        );
    }

    @Test
    void getAppointmentsByDoctor_shouldThrowBadRequest_whenUserIsNotDoctor() {
        User patient = patientUser();
        when(userRepository.findByEmail(patient.getEmail())).thenReturn(Optional.of(patient));

        assertThrows(
                BadRequestException.class,
                () -> appointmentService.getAppointmentsByDoctor(patient.getEmail())
        );
    }

    private User patientUser() {
        User patient = new User();
        patient.setId(PATIENT_ID);
        patient.setEmail("patient@cliniccare.com");
        patient.setRole(Role.PATIENT);
        patient.setFirstName("Patient");
        patient.setLastName("User");
        return patient;
    }

    private User doctorUser(Long id, String email) {
        User doctorUser = new User();
        doctorUser.setId(id);
        doctorUser.setRole(Role.DOCTOR);
        doctorUser.setEmail(email);
        return doctorUser;
    }

    private Doctor doctorEntity() {
        User doctorUser = doctorUser(50L, "doctor@cliniccare.com");
        Doctor doctor = new Doctor();
        doctor.setId(DOCTOR_ID);
        doctor.setUser(doctorUser);
        doctor.setSpecialization("General");
        return doctor;
    }

    private ServiceEntity serviceEntity() {
        ServiceEntity service = new ServiceEntity();
        service.setId(SERVICE_ID);
        service.setName("General Consultation");
        service.setIsEnabled(true);
        service.setDurationMinutes(30);
        return service;
    }

    private TimeSlot slotEntity(Doctor doctor, TimeSlotStatus status) {
        TimeSlot slot = new TimeSlot();
        slot.setId(SLOT_ID);
        slot.setDoctor(doctor);
        slot.setStartTime(LocalDateTime.now().plusHours(1));
        slot.setEndTime(LocalDateTime.now().plusHours(2));
        slot.setStatus(status);
        return slot;
    }

    private Appointment appointmentEntity(AppointmentStatus status) {
        User patient = patientUser();
        Doctor doctor = doctorEntity();
        ServiceEntity service = serviceEntity();
        TimeSlot slot = slotEntity(doctor, TimeSlotStatus.RESERVED);

        Appointment appointment = new Appointment();
        appointment.setId(APPOINTMENT_ID);
        appointment.setPatient(patient);
        appointment.setDoctor(doctor);
        appointment.setService(service);
        appointment.setTimeSlot(slot);
        appointment.setStatus(status);
        return appointment;
    }
}
