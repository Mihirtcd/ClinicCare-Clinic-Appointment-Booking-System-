package com.tus.cliniccare.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.tus.cliniccare.entity.enums.Role;
import com.tus.cliniccare.support.AbstractApiIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.datasource.url=jdbc:h2:mem:admin_api_it;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "spring.sql.init.mode=never",
                "app.jwt.secret=Y2xpbmljY2FyZS1hY2FkZW1pYy1qd3Qtc2VjcmV0LWtleS1mb3ItcHJvamVjdC1kZW1vLXNpZ25pbmctMzJieXRlcw=="
        }
)
class AdminApiIntegrationTest extends AbstractApiIntegrationTest {

    private final String adminEmail = "admin.integration@cliniccare.com";
    private final String adminPassword = "Password@123";

    @BeforeEach
    void setUp() {
        resetDatabase();
        createUser(adminEmail, adminPassword, Role.ADMIN);
    }

    @Test
    void adminCanManageUsersDoctorsServicesAndSlots() throws Exception {
        String adminToken = loginAndGetToken(adminEmail, adminPassword);

        String servicePayload = """
                {
                  "name":"Cardio Consultation",
                  "description":"Cardio test service",
                  "durationMinutes":30,
                  "isEnabled":true
                }
                """;
        ResponseEntity<String> serviceResponse = restTemplate.exchange(
                "/api/admin/services",
                HttpMethod.POST,
                authorizedJsonEntity(adminToken, servicePayload),
                String.class
        );
        assertEquals(HttpStatus.CREATED, serviceResponse.getStatusCode());
        Long serviceId = objectMapper.readTree(serviceResponse.getBody()).get("id").asLong();

        String createDoctorUserPayload = """
                {
                  "firstName":"Sarah",
                  "lastName":"Lee",
                  "email":"doctor.created.by.admin@cliniccare.com",
                  "password":"Password@123",
                  "phoneNumber":"9000000002",
                  "role":"DOCTOR"
                }
                """;
        ResponseEntity<String> doctorUserResponse = restTemplate.exchange(
                "/api/admin/users",
                HttpMethod.POST,
                authorizedJsonEntity(adminToken, createDoctorUserPayload),
                String.class
        );
        assertEquals(HttpStatus.CREATED, doctorUserResponse.getStatusCode());
        Long doctorUserId = objectMapper.readTree(doctorUserResponse.getBody()).get("id").asLong();

        String createDoctorPayload = """
                {
                  "userId":%d,
                  "specialization":"Cardiology",
                  "serviceIds":[%d]
                }
                """.formatted(doctorUserId, serviceId);
        ResponseEntity<String> doctorProfileResponse = restTemplate.exchange(
                "/api/admin/doctors",
                HttpMethod.POST,
                authorizedJsonEntity(adminToken, createDoctorPayload),
                String.class
        );
        assertEquals(HttpStatus.CREATED, doctorProfileResponse.getStatusCode());
        Long doctorId = objectMapper.readTree(doctorProfileResponse.getBody()).get("id").asLong();

        LocalDateTime startTime = LocalDateTime.now().plusDays(2).withHour(10).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime endTime = startTime.plusMinutes(30);
        String slotPayload = """
                {
                  "doctorId":%d,
                  "startTime":"%s",
                  "endTime":"%s"
                }
                """.formatted(doctorId, startTime, endTime);
        ResponseEntity<String> slotResponse = restTemplate.exchange(
                "/api/admin/timeslots",
                HttpMethod.POST,
                authorizedJsonEntity(adminToken, slotPayload),
                String.class
        );
        assertEquals(HttpStatus.CREATED, slotResponse.getStatusCode());
        JsonNode slotBody = objectMapper.readTree(slotResponse.getBody());
        assertEquals("AVAILABLE", slotBody.get("status").asText());

        ResponseEntity<String> usersByRole = restTemplate.exchange(
                "/api/admin/users?role=DOCTOR",
                HttpMethod.GET,
                authorizedJsonEntity(adminToken, null),
                String.class
        );
        assertEquals(HttpStatus.OK, usersByRole.getStatusCode());
        JsonNode usersArray = objectMapper.readTree(usersByRole.getBody());
        assertTrue(usersArray.isArray());
        assertTrue(!usersArray.isEmpty());

        ResponseEntity<String> statsResponse = restTemplate.exchange(
                "/api/admin/appointments/stats",
                HttpMethod.GET,
                authorizedJsonEntity(adminToken, null),
                String.class
        );
        assertEquals(HttpStatus.OK, statsResponse.getStatusCode());
        JsonNode statsBody = objectMapper.readTree(statsResponse.getBody());
        assertNotNull(statsBody.get("total"));
        assertNotNull(statsBody.get("pending"));
        assertNotNull(statsBody.get("confirmed"));
        assertNotNull(statsBody.get("rejected"));
        assertNotNull(statsBody.get("completed"));
        assertNotNull(statsBody.get("cancelled"));
    }

}
