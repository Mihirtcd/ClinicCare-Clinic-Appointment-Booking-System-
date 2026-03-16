package com.tus.cliniccare.integration;

import com.tus.cliniccare.entity.User;
import com.tus.cliniccare.entity.enums.Role;
import com.tus.cliniccare.repository.UserRepository;
import com.tus.cliniccare.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:user_service_it;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.sql.init.mode=never",
        "app.jwt.secret=Y2xpbmljY2FyZS1hY2FkZW1pYy1qd3Qtc2VjcmV0LWtleS1mb3ItcHJvamVjdC1kZW1vLXNpZ25pbmctMzJieXRlcw=="
})
@Transactional
class UserServiceIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void registerPatient_shouldPersistEncodedPasswordAndPatientRole() {
        String rawPassword = "Password@123";

        User saved = userService.registerPatient(
                "Patient",
                "One",
                "patient.integration@cliniccare.com",
                rawPassword,
                "9999999999"
        );

        User persisted = userRepository.findById(saved.getId()).orElseThrow();
        assertEquals(Role.PATIENT, persisted.getRole());
        assertNotEquals(rawPassword, persisted.getPassword());
        assertTrue(passwordEncoder.matches(rawPassword, persisted.getPassword()));
    }

    @Test
    void createUserByAdmin_shouldPersistEncodedPasswordForDoctorRole() {
        String rawPassword = "Password@123";

        User saved = userService.createUserByAdmin(
                "Doctor",
                "One",
                "doctor.integration@cliniccare.com",
                rawPassword,
                "8888888888",
                Role.DOCTOR
        );

        User persisted = userRepository.findById(saved.getId()).orElseThrow();
        assertEquals(Role.DOCTOR, persisted.getRole());
        assertNotEquals(rawPassword, persisted.getPassword());
        assertTrue(passwordEncoder.matches(rawPassword, persisted.getPassword()));
    }
}
