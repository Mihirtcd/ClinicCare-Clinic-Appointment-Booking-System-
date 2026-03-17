package com.tus.cliniccare.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.tus.cliniccare.entity.Doctor;
import com.tus.cliniccare.entity.DoctorService;
import com.tus.cliniccare.entity.ServiceEntity;
import com.tus.cliniccare.entity.TimeSlot;
import com.tus.cliniccare.entity.enums.Role;
import com.tus.cliniccare.entity.enums.TimeSlotStatus;
import com.tus.cliniccare.support.AbstractApiIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
class DoctorOwnershipIntegrationTest extends AbstractApiIntegrationTest {

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
        resetDatabase();

        createUser(patientEmail, patientPassword, Role.PATIENT);
        com.tus.cliniccare.entity.User doctorOneUser = createUser(doctorOneEmail, doctorPassword, Role.DOCTOR);
        com.tus.cliniccare.entity.User doctorTwoUser = createUser(doctorTwoEmail, doctorPassword, Role.DOCTOR);

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
}
