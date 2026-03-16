package com.tus.cliniccare.controller;

import com.tus.cliniccare.entity.User;
import com.tus.cliniccare.entity.enums.Role;
import com.tus.cliniccare.exception.GlobalExceptionHandler;
import com.tus.cliniccare.security.CustomUserDetailsService;
import com.tus.cliniccare.security.JwtService;
import com.tus.cliniccare.service.UserService;
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

@WebMvcTest(AdminUserController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class AdminUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void getUsers_shouldReturnAllUsers_whenRoleNotProvided() throws Exception {
        User patient = userEntity(1L, "patient@cliniccare.com", Role.PATIENT, "Patient", "One");
        User doctor = userEntity(2L, "doctor@cliniccare.com", Role.DOCTOR, "Doctor", "Two");
        when(userService.getAllUsers()).thenReturn(List.of(patient, doctor));

        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].email").value("patient@cliniccare.com"))
                .andExpect(jsonPath("$[1].role").value("DOCTOR"));
    }

    @Test
    void getUsers_shouldFilterByRole_whenRoleProvided() throws Exception {
        User patient = userEntity(3L, "patient2@cliniccare.com", Role.PATIENT, "Patient", "Two");
        when(userService.getUsersByRole(Role.PATIENT)).thenReturn(List.of(patient));

        mockMvc.perform(get("/api/admin/users").param("role", "PATIENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(3))
                .andExpect(jsonPath("$[0].role").value("PATIENT"));
    }

    @Test
    void createUser_shouldReturnCreatedUser() throws Exception {
        User doctorUser = userEntity(15L, "new.doctor@cliniccare.com", Role.DOCTOR, "John", "Doe");
        doctorUser.setPhoneNumber("9999999999");

        when(userService.createUserByAdmin(
                eq("John"),
                eq("Doe"),
                eq("new.doctor@cliniccare.com"),
                eq("Password@123"),
                eq("9999999999"),
                eq(Role.DOCTOR)
        )).thenReturn(doctorUser);

        String payload = """
                {
                  "firstName":"John",
                  "lastName":"Doe",
                  "email":"new.doctor@cliniccare.com",
                  "password":"Password@123",
                  "phoneNumber":"9999999999",
                  "role":"DOCTOR"
                }
                """;

        mockMvc.perform(post("/api/admin/users")
                        .contentType("application/json")
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(15))
                .andExpect(jsonPath("$.email").value("new.doctor@cliniccare.com"))
                .andExpect(jsonPath("$.role").value("DOCTOR"));
    }

    private User userEntity(Long id, String email, Role role, String firstName, String lastName) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setRole(role);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        return user;
    }
}
