package com.tus.cliniccare.controller;

import com.tus.cliniccare.entity.Doctor;
import com.tus.cliniccare.entity.ServiceEntity;
import com.tus.cliniccare.entity.TimeSlot;
import com.tus.cliniccare.entity.User;
import com.tus.cliniccare.entity.enums.Role;
import com.tus.cliniccare.entity.enums.TimeSlotStatus;
import com.tus.cliniccare.exception.GlobalExceptionHandler;
import com.tus.cliniccare.security.CustomUserDetailsService;
import com.tus.cliniccare.security.JwtService;
import com.tus.cliniccare.service.AppointmentService;
import com.tus.cliniccare.service.DoctorService;
import com.tus.cliniccare.service.ServiceEntityService;
import com.tus.cliniccare.service.TimeSlotService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PatientController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class PatientControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ServiceEntityService serviceEntityService;

    @MockitoBean
    private DoctorService doctorService;

    @MockitoBean
    private TimeSlotService timeSlotService;

    @MockitoBean
    private AppointmentService appointmentService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void getEnabledServices_shouldReturnServiceList() throws Exception {
        ServiceEntity service = new ServiceEntity();
        service.setId(1L);
        service.setName("General Consultation");
        service.setDescription("General checkup");
        service.setDurationMinutes(30);
        service.setIsEnabled(true);

        when(serviceEntityService.getEnabledServices()).thenReturn(List.of(service));

        mockMvc.perform(get("/api/patient/services"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("General Consultation"))
                .andExpect(jsonPath("$[0].isEnabled").value(true));
    }

    @Test
    void getAvailableSlotsByDoctor_shouldReturnSlots() throws Exception {
        Doctor doctor = doctorEntity(10L, 100L, "John", "Doe");
        TimeSlot slot = new TimeSlot();
        slot.setId(20L);
        slot.setDoctor(doctor);
        slot.setStartTime(LocalDateTime.of(2026, 3, 20, 10, 0));
        slot.setEndTime(LocalDateTime.of(2026, 3, 20, 10, 30));
        slot.setStatus(TimeSlotStatus.AVAILABLE);

        when(timeSlotService.getAvailableSlotsByDoctor(10L)).thenReturn(List.of(slot));

        mockMvc.perform(get("/api/patient/slots").param("doctorId", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(20))
                .andExpect(jsonPath("$[0].doctorId").value(10))
                .andExpect(jsonPath("$[0].status").value("AVAILABLE"));
    }

    @Test
    void bookAppointment_shouldReturnCreated_whenValidRequest() throws Exception {
        Doctor doctor = doctorEntity(10L, 100L, "John", "Doe");
        User patient = new User();
        patient.setId(300L);
        patient.setFirstName("Patient");
        patient.setLastName("One");
        patient.setRole(Role.PATIENT);

        ServiceEntity service = new ServiceEntity();
        service.setId(30L);
        service.setName("General Consultation");
        service.setDurationMinutes(30);
        service.setIsEnabled(true);

        TimeSlot slot = new TimeSlot();
        slot.setId(40L);
        slot.setDoctor(doctor);
        slot.setStartTime(LocalDateTime.of(2026, 3, 20, 11, 0));
        slot.setEndTime(LocalDateTime.of(2026, 3, 20, 11, 30));
        slot.setStatus(TimeSlotStatus.RESERVED);

        com.tus.cliniccare.entity.Appointment appointment = new com.tus.cliniccare.entity.Appointment();
        appointment.setId(50L);
        appointment.setPatient(patient);
        appointment.setDoctor(doctor);
        appointment.setService(service);
        appointment.setTimeSlot(slot);
        appointment.setStatus(com.tus.cliniccare.entity.enums.AppointmentStatus.PENDING);
        appointment.setPatientNote("Need quick check");

        when(appointmentService.bookAppointment(
                eq("patient.test@cliniccare.com"),
                eq(10L),
                eq(30L),
                eq(40L),
                eq("Need quick check")
        )).thenReturn(appointment);

        String payload = """
                {
                  "doctorId":10,
                  "serviceId":30,
                  "timeSlotId":40,
                  "patientNote":"Need quick check"
                }
                """;

        TestingAuthenticationToken auth = new TestingAuthenticationToken(
                "patient.test@cliniccare.com",
                "password",
                "ROLE_PATIENT"
        );

        mockMvc.perform(post("/api/patient/appointments")
                        .principal(auth)
                        .contentType("application/json")
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(50))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.doctorId").value(10))
                .andExpect(jsonPath("$.serviceId").value(30));
    }

    private Doctor doctorEntity(Long doctorId, Long userId, String firstName, String lastName) {
        User user = new User();
        user.setId(userId);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setRole(Role.DOCTOR);
        user.setEmail(firstName.toLowerCase() + ".doctor@cliniccare.com");

        Doctor doctor = new Doctor();
        doctor.setId(doctorId);
        doctor.setUser(user);
        doctor.setSpecialization("General");
        return doctor;
    }
}
