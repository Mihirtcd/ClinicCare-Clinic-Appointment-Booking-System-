package com.tus.cliniccare.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tus.cliniccare.entity.User;
import com.tus.cliniccare.entity.enums.Role;
import com.tus.cliniccare.repository.AppointmentRepository;
import com.tus.cliniccare.repository.DoctorRepository;
import com.tus.cliniccare.repository.DoctorServiceRepository;
import com.tus.cliniccare.repository.ServiceRepository;
import com.tus.cliniccare.repository.TimeSlotRepository;
import com.tus.cliniccare.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.datasource.url=jdbc:h2:mem:security_access_it;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "spring.sql.init.mode=never",
                "app.jwt.secret=Y2xpbmljY2FyZS1hY2FkZW1pYy1qd3Qtc2VjcmV0LWtleS1mb3ItcHJvamVjdC1kZW1vLXNpZ25pbmctMzJieXRlcw=="
        }
)
class SecurityAccessIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private TimeSlotRepository timeSlotRepository;

    @Autowired
    private DoctorServiceRepository doctorServiceRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private ServiceRepository serviceRepository;

    @Autowired
    private UserRepository userRepository;

    private final String adminEmail = "admin.security@cliniccare.com";
    private final String adminPassword = "Password@123";
    private final String patientEmail = "patient.security@cliniccare.com";
    private final String patientPassword = "Password@123";

    @BeforeEach
    void setUp() {
        appointmentRepository.deleteAll();
        timeSlotRepository.deleteAll();
        doctorServiceRepository.deleteAll();
        doctorRepository.deleteAll();
        serviceRepository.deleteAll();
        userRepository.deleteAll();

        userRepository.save(user(adminEmail, adminPassword, Role.ADMIN));
        userRepository.save(user(patientEmail, patientPassword, Role.PATIENT));
    }

    @Test
    void protectedEndpoints_shouldRejectRequestsWithoutToken() {
        ResponseEntity<String> adminEndpointResponse = restTemplate.exchange(
                "/api/admin/users",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                String.class
        );
        assertTrue(
                adminEndpointResponse.getStatusCode() == HttpStatus.UNAUTHORIZED
                        || adminEndpointResponse.getStatusCode() == HttpStatus.FORBIDDEN
        );

        ResponseEntity<String> patientEndpointResponse = restTemplate.exchange(
                "/api/patient/services",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                String.class
        );
        assertTrue(
                patientEndpointResponse.getStatusCode() == HttpStatus.UNAUTHORIZED
                        || patientEndpointResponse.getStatusCode() == HttpStatus.FORBIDDEN
        );
    }

    @Test
    void patientToken_shouldNotAccessAdminOrDoctorProtectedEndpoints() throws Exception {
        String patientToken = loginAndGetToken(patientEmail, patientPassword);

        ResponseEntity<String> adminResponse = restTemplate.exchange(
                "/api/admin/users",
                HttpMethod.GET,
                authorizedEntity(patientToken),
                String.class
        );
        assertEquals(HttpStatus.FORBIDDEN, adminResponse.getStatusCode());

        ResponseEntity<String> doctorActionResponse = restTemplate.exchange(
                "/api/appointments/1/confirm",
                HttpMethod.PATCH,
                authorizedEntity(patientToken),
                String.class
        );
        assertEquals(HttpStatus.FORBIDDEN, doctorActionResponse.getStatusCode());
    }

    private User user(String email, String rawPassword, Role role) {
        User user = new User();
        user.setFirstName("Test");
        user.setLastName("User");
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setPhoneNumber("9000000099");
        user.setRole(role);
        return user;
    }

    private String loginAndGetToken(String email, String password) throws Exception {
        String loginPayload = """
                {
                  "email":"%s",
                  "password":"%s"
                }
                """.formatted(email, password);

        ResponseEntity<String> loginResponse = restTemplate.exchange(
                "/api/auth/login",
                HttpMethod.POST,
                jsonEntity(loginPayload),
                String.class
        );

        assertEquals(HttpStatus.OK, loginResponse.getStatusCode());
        String authHeader = loginResponse.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        assertNotNull(authHeader);
        assertTrue(authHeader.startsWith("Bearer "));
        return authHeader.substring("Bearer ".length());
    }

    private HttpEntity<String> jsonEntity(String body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }

    private HttpEntity<String> authorizedEntity(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return new HttpEntity<>(headers);
    }
}
