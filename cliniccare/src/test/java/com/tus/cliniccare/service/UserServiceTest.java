package com.tus.cliniccare.service;

import com.tus.cliniccare.entity.User;
import com.tus.cliniccare.entity.enums.Role;
import com.tus.cliniccare.exception.BadRequestException;
import com.tus.cliniccare.exception.ConflictException;
import com.tus.cliniccare.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    void findByEmail_shouldReturnUser_whenUserExists() {
        String email = "patient@cliniccare.com";
        User user = new User();
        user.setId(1L);
        user.setEmail(email);
        user.setRole(Role.PATIENT);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        Optional<User> result = userService.findByEmail(email);

        assertTrue(result.isPresent());
        assertEquals(email, result.get().getEmail());
        assertEquals(Role.PATIENT, result.get().getRole());
    }

    @Test
    void emailExists_shouldReturnTrue_whenUserExists() {
        String email = "exists@cliniccare.com";
        User user = new User();
        user.setId(2L);
        user.setEmail(email);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        boolean result = userService.emailExists(email);

        assertTrue(result);
    }

    @Test
    void emailExists_shouldReturnFalse_whenUserDoesNotExist() {
        String email = "missing@cliniccare.com";
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        boolean result = userService.emailExists(email);

        assertFalse(result);
    }

    @Test
    void registerPatient_shouldThrowConflict_whenEmailAlreadyExists() {
        String email = "existing@cliniccare.com";
        User existing = new User();
        existing.setEmail(email);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(existing));

        assertThrows(
                ConflictException.class,
                () -> userService.registerPatient("A", "B", email, "Password@123", "9999999999")
        );
    }

    @Test
    void createUserByAdmin_shouldThrowBadRequest_whenRoleIsNotDoctor() {
        assertThrows(
                BadRequestException.class,
                () -> userService.createUserByAdmin(
                        "Admin",
                        "User",
                        "admin2@cliniccare.com",
                        "Password@123",
                        "8888888888",
                        Role.ADMIN
                )
        );
    }

    @Test
    void registerPatient_shouldEncodePassword_beforeSaving() {
        String email = "new.patient@cliniccare.com";
        String rawPassword = "Password@123";
        String encodedPassword = "encoded-password";

        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());
        when(passwordEncoder.encode(rawPassword)).thenReturn(encodedPassword);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User saved = userService.registerPatient(
                "New",
                "Patient",
                email,
                rawPassword,
                "7777777777"
        );

        assertEquals(encodedPassword, saved.getPassword());
        assertEquals(Role.PATIENT, saved.getRole());
        verify(passwordEncoder).encode(rawPassword);
    }

    @Test
    void createUserByAdmin_shouldEncodePassword_beforeSaving() {
        String email = "new.doctor@cliniccare.com";
        String rawPassword = "Password@123";
        String encodedPassword = "encoded-doctor-password";

        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());
        when(passwordEncoder.encode(rawPassword)).thenReturn(encodedPassword);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User saved = userService.createUserByAdmin(
                "Doc",
                "User",
                email,
                rawPassword,
                "6666666666",
                Role.DOCTOR
        );

        assertEquals(encodedPassword, saved.getPassword());
        assertEquals(Role.DOCTOR, saved.getRole());
        verify(passwordEncoder).encode(rawPassword);
    }
}
