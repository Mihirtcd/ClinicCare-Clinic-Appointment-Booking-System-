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
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private AuthenticationManager authenticationManager;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private JwtService jwtService;

    @Test
    void register_shouldReturnCreatedUser() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setFirstName("Test");
        user.setLastName("User");
        user.setEmail("test.user@cliniccare.com");
        user.setPhoneNumber("9999999999");
        user.setRole(Role.PATIENT);

        when(userService.registerPatient(
                "Test",
                "User",
                "test.user@cliniccare.com",
                "Password@123",
                "9999999999"
        )).thenReturn(user);

        String payload = """
                {
                  "firstName":"Test",
                  "lastName":"User",
                  "email":"test.user@cliniccare.com",
                  "password":"Password@123",
                  "phoneNumber":"9999999999"
                }
                """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email").value("test.user@cliniccare.com"))
                .andExpect(jsonPath("$.role").value("PATIENT"));
    }

    @Test
    void login_shouldReturnTokenHeaderAndUserBody() throws Exception {
        User user = new User();
        user.setId(2L);
        user.setFirstName("Patient");
        user.setLastName("Demo");
        user.setEmail("patient.demo@cliniccare.com");
        user.setPhoneNumber("8888888888");
        user.setRole(Role.PATIENT);

        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername("patient.demo@cliniccare.com")
                .password("encoded")
                .authorities("ROLE_PATIENT")
                .build();

        when(authenticationManager.authenticate(any())).thenReturn(null);
        when(customUserDetailsService.loadUserByUsername("patient.demo@cliniccare.com")).thenReturn(userDetails);
        when(jwtService.generateToken(userDetails)).thenReturn("jwt-token");
        when(userService.findByEmail("patient.demo@cliniccare.com")).thenReturn(Optional.of(user));

        String payload = """
                {
                  "email":"patient.demo@cliniccare.com",
                  "password":"Password@123"
                }
                """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.AUTHORIZATION, "Bearer jwt-token"))
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.email").value("patient.demo@cliniccare.com"));
    }

    @Test
    void login_shouldReturnBadRequest_forInvalidCredentials() throws Exception {
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("Bad credentials"));

        String payload = """
                {
                  "email":"wrong@cliniccare.com",
                  "password":"WrongPassword@123"
                }
                """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid email or password."));
    }
}
