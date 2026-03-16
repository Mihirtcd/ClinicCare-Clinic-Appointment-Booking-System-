package com.tus.cliniccare.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tus.cliniccare.entity.Doctor;
import com.tus.cliniccare.entity.DoctorService;
import com.tus.cliniccare.entity.ServiceEntity;
import com.tus.cliniccare.entity.TimeSlot;
import com.tus.cliniccare.entity.User;
import com.tus.cliniccare.entity.enums.Role;
import com.tus.cliniccare.entity.enums.TimeSlotStatus;
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

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.datasource.url=jdbc:h2:mem:doctor_ownership_it;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "spring.sql.init.mode=never",
                "app.jwt.secret=Y2xpbmljY2FyZS1hY2FkZW1pYy1qd3Qtc2VjcmV0LWtleS1mb3ItcHJvamVjdC1kZW1vLXNpZ25pbmctMzJieXRlcw=="
        }
)
class DoctorOwnershipIntegrationTest {

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

    private final String patientEmail = "patient.ownership@cliniccare.com";
    private final String patientPassword = "Password@123";
    private final String doctorOneEmail = "doctor.one.ownership@cliniccare.com";
    private final String doctorTwoEmail = "doctor.two.ownership@cliniccare.com";
    private final String doctorPassword = "Password@123";

    private Long doctorOneId;
    private Long serviceId;
    private Long slotId;

    @BeforeEach
    void setUp() {
        appointmentRepository.deleteAll();
        timeSlotRepository.deleteAll();
        doctorServiceRepository.deleteAll();
        doctorRepository.deleteAll();
        serviceRepository.deleteAll();
        userRepository.deleteAll();

        User patient = user(patientEmail, patientPassword, Role.PATIENT);
        userRepository.save(patient);

        User doctorOneUser = user(doctorOneEmail, doctorPassword, Role.DOCTOR);
        User doctorTwoUser = user(doctorTwoEmail, doctorPassword, Role.DOCTOR);
        doctorOneUser = userRepository.save(doctorOneUser);
        doctorTwoUser = userRepository.save(doctorTwoUser);

        Doctor doctorOne = new Doctor();
        doctorOne.setUser(doctorOneUser);
        doctorOne.setSpecialization("Cardiology");
        doctorOne = doctorRepository.save(doctorOne);
        this.doctorOneId = doctorOne.getId();

        Doctor doctorTwo = new Doctor();
        doctorTwo.setUser(doctorTwoUser);
        doctorTwo.setSpecialization("Dental");
        doctorRepository.save(doctorTwo);

        ServiceEntity service = new ServiceEntity();
        service.setName("General Consultation");
        service.setDescription("General");
        service.setDurationMinutes(30);
        service.setIsEnabled(true);
        service = serviceRepository.save(service);
        this.serviceId = service.getId();

        DoctorService mapping = new DoctorService();
        mapping.setDoctor(doctorOne);
        mapping.setService(service);
        doctorServiceRepository.save(mapping);

        TimeSlot slot = new TimeSlot();
        slot.setDoctor(doctorOne);
        slot.setStartTime(LocalDateTime.now().plusDays(1).withHour(10).withMinute(0).withSecond(0).withNano(0));
        slot.setEndTime(LocalDateTime.now().plusDays(1).withHour(10).withMinute(30).withSecond(0).withNano(0));
        slot.setStatus(TimeSlotStatus.AVAILABLE);
        slot = timeSlotRepository.save(slot);
        this.slotId = slot.getId();
    }

    @Test
    void nonAssignedDoctorCannotConfirmAppointment() throws Exception {
        String patientToken = loginAndGetToken(patientEmail, patientPassword);
        String doctorOneToken = loginAndGetToken(doctorOneEmail, doctorPassword);
        String doctorTwoToken = loginAndGetToken(doctorTwoEmail, doctorPassword);

        String bookingPayload = """
                {
                  "doctorId": %d,
                  "serviceId": %d,
                  "timeSlotId": %d,
                  "patientNote": "Need check"
                }
                """.formatted(doctorOneId, serviceId, slotId);
        ResponseEntity<String> bookingResponse = restTemplate.exchange(
                "/api/patient/appointments",
                HttpMethod.POST,
                authorizedJsonEntity(patientToken, bookingPayload),
                String.class
        );
        assertEquals(HttpStatus.CREATED, bookingResponse.getStatusCode());
        Long appointmentId = objectMapper.readTree(bookingResponse.getBody()).get("id").asLong();

        ResponseEntity<String> doctorTwoConfirmResponse = restTemplate.exchange(
                "/api/appointments/" + appointmentId + "/confirm",
                HttpMethod.PATCH,
                authorizedJsonEntity(doctorTwoToken, null),
                String.class
        );
        assertEquals(HttpStatus.BAD_REQUEST, doctorTwoConfirmResponse.getStatusCode());
        JsonNode errorBody = objectMapper.readTree(doctorTwoConfirmResponse.getBody());
        assertTrue(errorBody.get("message").asText().contains("assigned to you"));

        ResponseEntity<String> doctorOneConfirmResponse = restTemplate.exchange(
                "/api/appointments/" + appointmentId + "/confirm",
                HttpMethod.PATCH,
                authorizedJsonEntity(doctorOneToken, null),
                String.class
        );
        assertEquals(HttpStatus.OK, doctorOneConfirmResponse.getStatusCode());
        JsonNode confirmedBody = objectMapper.readTree(doctorOneConfirmResponse.getBody());
        assertEquals("CONFIRMED", confirmedBody.get("status").asText());
    }

    private User user(String email, String rawPassword, Role role) {
        User user = new User();
        user.setFirstName("Test");
        user.setLastName("User");
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setPhoneNumber("9111111111");
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

    private HttpEntity<String> authorizedJsonEntity(String token, String body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        return new HttpEntity<>(body, headers);
    }
}
