package com.tus.cliniccare.controller;

import com.tus.cliniccare.entity.Appointment;
import com.tus.cliniccare.entity.Doctor;
import com.tus.cliniccare.entity.ServiceEntity;
import com.tus.cliniccare.entity.TimeSlot;
import com.tus.cliniccare.entity.User;
import com.tus.cliniccare.entity.enums.AppointmentStatus;
import com.tus.cliniccare.entity.enums.Role;
import com.tus.cliniccare.entity.enums.TimeSlotStatus;
import com.tus.cliniccare.exception.GlobalExceptionHandler;
import com.tus.cliniccare.security.CustomUserDetailsService;
import com.tus.cliniccare.security.JwtService;
import com.tus.cliniccare.service.AppointmentService;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DoctorController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class DoctorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AppointmentService appointmentService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void getDoctorAppointments_shouldReturnAppointments_whenStatusNotProvided() throws Exception {
        Appointment appointment = appointmentEntity(10L, AppointmentStatus.PENDING);
        when(appointmentService.getAppointmentsByDoctor("doctor@cliniccare.com"))
                .thenReturn(List.of(appointment));

        TestingAuthenticationToken auth = new TestingAuthenticationToken(
                "doctor@cliniccare.com",
                "password",
                "ROLE_DOCTOR"
        );

        mockMvc.perform(get("/api/doctor/appointments").principal(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].patientName").value("Patient One"))
                .andExpect(jsonPath("$[0].doctorName").value("Doctor Smith"))
                .andExpect(jsonPath("$[0].status").value("PENDING"));
    }

    @Test
    void getDoctorAppointments_shouldReturnFilteredAppointments_whenStatusProvided() throws Exception {
        Appointment appointment = appointmentEntity(12L, AppointmentStatus.CONFIRMED);
        when(appointmentService.getAppointmentsByDoctorAndStatus(
                eq("doctor@cliniccare.com"),
                eq(AppointmentStatus.CONFIRMED)
        )).thenReturn(List.of(appointment));

        TestingAuthenticationToken auth = new TestingAuthenticationToken(
                "doctor@cliniccare.com",
                "password",
                "ROLE_DOCTOR"
        );

        mockMvc.perform(get("/api/doctor/appointments")
                        .principal(auth)
                        .param("status", "CONFIRMED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(12))
                .andExpect(jsonPath("$[0].serviceName").value("General Consultation"))
                .andExpect(jsonPath("$[0].status").value("CONFIRMED"));
    }

    private Appointment appointmentEntity(Long id, AppointmentStatus status) {
        User patient = new User();
        patient.setId(1L);
        patient.setFirstName("Patient");
        patient.setLastName("One");
        patient.setRole(Role.PATIENT);

        User doctorUser = new User();
        doctorUser.setId(2L);
        doctorUser.setFirstName("Doctor");
        doctorUser.setLastName("Smith");
        doctorUser.setRole(Role.DOCTOR);

        Doctor doctor = new Doctor();
        doctor.setId(5L);
        doctor.setUser(doctorUser);

        ServiceEntity service = new ServiceEntity();
        service.setId(8L);
        service.setName("General Consultation");
        service.setDurationMinutes(30);
        service.setIsEnabled(true);

        TimeSlot slot = new TimeSlot();
        slot.setId(9L);
        slot.setDoctor(doctor);
        slot.setStartTime(LocalDateTime.of(2026, 3, 20, 10, 0));
        slot.setEndTime(LocalDateTime.of(2026, 3, 20, 10, 30));
        slot.setStatus(TimeSlotStatus.RESERVED);

        Appointment appointment = new Appointment();
        appointment.setId(id);
        appointment.setPatient(patient);
        appointment.setDoctor(doctor);
        appointment.setService(service);
        appointment.setTimeSlot(slot);
        appointment.setStatus(status);
        appointment.setPatientNote("Need check");
        appointment.setBookedAt(LocalDateTime.of(2026, 3, 19, 9, 0));
        return appointment;
    }
}
