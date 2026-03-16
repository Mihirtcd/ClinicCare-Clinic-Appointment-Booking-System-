package com.tus.cliniccare.api;

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
                "spring.datasource.url=jdbc:h2:mem:appointment_api_test;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "spring.sql.init.mode=never",
                "app.jwt.secret=Y2xpbmljY2FyZS1hY2FkZW1pYy1qd3Qtc2VjcmV0LWtleS1mb3ItcHJvamVjdC1kZW1vLXNpZ25pbmctMzJieXRlcw=="
        }
)
class AppointmentApiTest {

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

    private Long doctorId;
    private Long serviceId;
    private Long slotId;
    private String doctorEmail;
    private final String doctorPassword = "Password@123";

    @BeforeEach
    void setUp() {
        appointmentRepository.deleteAll();
        timeSlotRepository.deleteAll();
        doctorServiceRepository.deleteAll();
        doctorRepository.deleteAll();
        serviceRepository.deleteAll();
        userRepository.deleteAll();

        User doctorUser = new User();
        doctorUser.setFirstName("Doctor");
        doctorUser.setLastName("Api");
        doctorUser.setEmail("doctor.api@cliniccare.com");
        doctorUser.setPassword(passwordEncoder.encode(doctorPassword));
        doctorUser.setPhoneNumber("7777777777");
        doctorUser.setRole(Role.DOCTOR);
        doctorUser = userRepository.save(doctorUser);
        this.doctorEmail = doctorUser.getEmail();

        Doctor doctor = new Doctor();
        doctor.setUser(doctorUser);
        doctor.setSpecialization("General");
        doctor = doctorRepository.save(doctor);
        this.doctorId = doctor.getId();

        ServiceEntity service = new ServiceEntity();
        service.setName("General Consultation");
        service.setDescription("Consultation");
        service.setDurationMinutes(30);
        service.setIsEnabled(true);
        service = serviceRepository.save(service);
        this.serviceId = service.getId();

        DoctorService mapping = new DoctorService();
        mapping.setDoctor(doctor);
        mapping.setService(service);
        doctorServiceRepository.save(mapping);

        TimeSlot slot = new TimeSlot();
        slot.setDoctor(doctor);
        slot.setStartTime(LocalDateTime.now().plusDays(1).withHour(11).withMinute(0).withSecond(0).withNano(0));
        slot.setEndTime(LocalDateTime.now().plusDays(1).withHour(11).withMinute(30).withSecond(0).withNano(0));
        slot.setStatus(TimeSlotStatus.AVAILABLE);
        slot = timeSlotRepository.save(slot);
        this.slotId = slot.getId();
    }

    @Test
    void patientCanBookAndDoctorCanConfirmAppointment() throws Exception {
        String patientEmail = "patient.api@cliniccare.com";
        registerPatient(patientEmail);

        String patientToken = loginAndGetToken(patientEmail, "Password@123");
        String doctorToken = loginAndGetToken(doctorEmail, doctorPassword);

        String bookingPayload = """
                {
                  "doctorId": %d,
                  "serviceId": %d,
                  "timeSlotId": %d,
                  "patientNote": "Need quick check"
                }
                """.formatted(doctorId, serviceId, slotId);

        ResponseEntity<String> bookingResponse = restTemplate.exchange(
                "/api/patient/appointments",
                HttpMethod.POST,
                authorizedJsonEntity(patientToken, bookingPayload),
                String.class
        );

        assertEquals(HttpStatus.CREATED, bookingResponse.getStatusCode());
        JsonNode bookedBody = objectMapper.readTree(bookingResponse.getBody());
        long appointmentId = bookedBody.get("id").asLong();
        assertEquals("PENDING", bookedBody.get("status").asText());

        ResponseEntity<String> confirmResponse = restTemplate.exchange(
                "/api/appointments/" + appointmentId + "/confirm",
                HttpMethod.PATCH,
                authorizedJsonEntity(doctorToken, null),
                String.class
        );

        assertEquals(HttpStatus.OK, confirmResponse.getStatusCode());
        JsonNode confirmBody = objectMapper.readTree(confirmResponse.getBody());
        assertEquals("CONFIRMED", confirmBody.get("status").asText());
    }

    private void registerPatient(String email) {
        String registerPayload = """
                {
                  "firstName":"Patient",
                  "lastName":"Api",
                  "email":"%s",
                  "password":"Password@123",
                  "phoneNumber":"6666666666"
                }
                """.formatted(email);

        ResponseEntity<String> registerResponse = restTemplate.exchange(
                "/api/auth/register",
                HttpMethod.POST,
                jsonEntity(registerPayload),
                String.class
        );

        assertEquals(HttpStatus.CREATED, registerResponse.getStatusCode());
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
