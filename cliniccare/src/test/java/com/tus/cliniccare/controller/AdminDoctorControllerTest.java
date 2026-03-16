package com.tus.cliniccare.controller;

import com.tus.cliniccare.entity.Doctor;
import com.tus.cliniccare.entity.ServiceEntity;
import com.tus.cliniccare.entity.User;
import com.tus.cliniccare.entity.enums.Role;
import com.tus.cliniccare.exception.GlobalExceptionHandler;
import com.tus.cliniccare.security.CustomUserDetailsService;
import com.tus.cliniccare.security.JwtService;
import com.tus.cliniccare.service.DoctorService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminDoctorController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class AdminDoctorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DoctorService doctorService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void createDoctor_shouldReturnCreatedDoctor() throws Exception {
        Doctor doctor = doctorEntity(11L, 101L, "John", "Smith", "Cardiology");
        when(doctorService.createDoctor(eq(101L), eq("Cardiology"), eq(List.of(1L, 2L))))
                .thenReturn(doctor);
        when(doctorService.getDoctorServices(11L)).thenReturn(List.of(
                doctorServiceMapping(doctor, 1L),
                doctorServiceMapping(doctor, 2L)
        ));

        String payload = """
                {
                  "userId":101,
                  "specialization":"Cardiology",
                  "serviceIds":[1,2]
                }
                """;

        mockMvc.perform(post("/api/admin/doctors")
                        .contentType("application/json")
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(11))
                .andExpect(jsonPath("$.userId").value(101))
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.specialization").value("Cardiology"))
                .andExpect(jsonPath("$.serviceIds.length()").value(2));
    }

    @Test
    void getAllDoctors_shouldReturnDoctorList() throws Exception {
        Doctor doctor1 = doctorEntity(11L, 101L, "John", "Smith", "Cardiology");
        Doctor doctor2 = doctorEntity(12L, 102L, "Sarah", "Lee", "Dermatology");

        when(doctorService.getAllDoctors()).thenReturn(List.of(doctor1, doctor2));
        when(doctorService.getDoctorServices(11L)).thenReturn(List.of(doctorServiceMapping(doctor1, 1L)));
        when(doctorService.getDoctorServices(12L)).thenReturn(List.of(doctorServiceMapping(doctor2, 3L)));

        mockMvc.perform(get("/api/admin/doctors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(11))
                .andExpect(jsonPath("$[0].serviceIds[0]").value(1))
                .andExpect(jsonPath("$[1].id").value(12))
                .andExpect(jsonPath("$[1].serviceIds[0]").value(3));
    }

    private Doctor doctorEntity(Long doctorId, Long userId, String firstName, String lastName, String specialization) {
        User user = new User();
        user.setId(userId);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setRole(Role.DOCTOR);
        user.setEmail(firstName.toLowerCase() + "." + lastName.toLowerCase() + "@cliniccare.com");

        Doctor doctor = new Doctor();
        doctor.setId(doctorId);
        doctor.setUser(user);
        doctor.setSpecialization(specialization);
        return doctor;
    }

    private com.tus.cliniccare.entity.DoctorService doctorServiceMapping(Doctor doctor, Long serviceId) {
        ServiceEntity service = new ServiceEntity();
        service.setId(serviceId);
        service.setName("Service " + serviceId);
        service.setDurationMinutes(30);
        service.setIsEnabled(true);

        com.tus.cliniccare.entity.DoctorService mapping = new com.tus.cliniccare.entity.DoctorService();
        mapping.setDoctor(doctor);
        mapping.setService(service);
        return mapping;
    }
}
