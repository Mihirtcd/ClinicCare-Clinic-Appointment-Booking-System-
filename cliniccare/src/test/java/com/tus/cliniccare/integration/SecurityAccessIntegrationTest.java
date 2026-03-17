package com.tus.cliniccare.integration;

import com.tus.cliniccare.entity.enums.Role;
import com.tus.cliniccare.support.AbstractApiIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
class SecurityAccessIntegrationTest extends AbstractApiIntegrationTest {

    private final String adminEmail = "admin.security@cliniccare.com";
    private final String adminPassword = "Password@123";
    private final String patientEmail = "patient.security@cliniccare.com";
    private final String patientPassword = "Password@123";

    @BeforeEach
    void setUp() {
        resetDatabase();
        createUser(adminEmail, adminPassword, Role.ADMIN);
        createUser(patientEmail, patientPassword, Role.PATIENT);
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
    void patientToken_shouldNotAccessAdminOrDoctorProtectedEndpoints() {
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

    private HttpEntity<String> authorizedEntity(String token) {
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setBearerAuth(token);
        return new HttpEntity<>(headers);
    }
}
