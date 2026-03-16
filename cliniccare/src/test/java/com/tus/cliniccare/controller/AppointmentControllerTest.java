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

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AppointmentController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class AppointmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AppointmentService appointmentService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void confirmAppointment_shouldReturnUpdatedAppointment() throws Exception {
        Appointment appointment = appointmentEntity(50L, AppointmentStatus.CONFIRMED);
        when(appointmentService.confirmAppointment(50L, "doctor@cliniccare.com", false)).thenReturn(appointment);

        TestingAuthenticationToken auth = new TestingAuthenticationToken(
                "doctor@cliniccare.com",
                "password",
                "ROLE_DOCTOR"
        );

        mockMvc.perform(patch("/api/appointments/50/confirm").principal(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(50))
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    void rejectAppointment_shouldReturnBadRequest_whenPathAndBodyIdsMismatch() throws Exception {
        String payload = """
                {
                  "appointmentId": 99,
                  "rejectionReason": "Unavailable"
                }
                """;

        TestingAuthenticationToken auth = new TestingAuthenticationToken(
                "doctor@cliniccare.com",
                "password",
                "ROLE_DOCTOR"
        );

        mockMvc.perform(patch("/api/appointments/50/reject")
                        .principal(auth)
                        .contentType("application/json")
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Appointment id in path and body must match."));
    }

    @Test
    void completeAppointment_shouldPassAdminFlagTrue_forAdminActor() throws Exception {
        Appointment appointment = appointmentEntity(70L, AppointmentStatus.COMPLETED);
        when(appointmentService.completeAppointment(eq(70L), eq("admin@cliniccare.com"), eq(true)))
                .thenReturn(appointment);

        TestingAuthenticationToken auth = new TestingAuthenticationToken(
                "admin@cliniccare.com",
                "password",
                "ROLE_ADMIN"
        );

        mockMvc.perform(patch("/api/appointments/70/complete").principal(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(70))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
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
        doctorUser.setLastName("One");
        doctorUser.setRole(Role.DOCTOR);

        Doctor doctor = new Doctor();
        doctor.setId(10L);
        doctor.setUser(doctorUser);

        ServiceEntity service = new ServiceEntity();
        service.setId(20L);
        service.setName("General Consultation");
        service.setDurationMinutes(30);
        service.setIsEnabled(true);

        TimeSlot slot = new TimeSlot();
        slot.setId(30L);
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
        appointment.setPatientNote("Note");
        appointment.setBookedAt(LocalDateTime.of(2026, 3, 19, 9, 0));
        return appointment;
    }
}
