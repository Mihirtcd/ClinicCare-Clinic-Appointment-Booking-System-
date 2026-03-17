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

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.datasource.url=jdbc:h2:mem:patient_cancel_it;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "spring.sql.init.mode=never",
                "app.jwt.secret=Y2xpbmljY2FyZS1hY2FkZW1pYy1qd3Qtc2VjcmV0LWtleS1mb3ItcHJvamVjdC1kZW1vLXNpZ25pbmctMzJieXRlcw=="
        }
)
class PatientCancellationIntegrationTest extends AbstractApiIntegrationTest {

    private final String patientOneEmail = "patient.one.cancel@cliniccare.com";
    private final String patientTwoEmail = "patient.two.cancel@cliniccare.com";
    private final String patientPassword = "Password@123";

    private Long doctorId;
    private Long serviceId;
    private Long slotId;

    @BeforeEach
    void setUp() {
        resetDatabase();

        createUser(patientOneEmail, patientPassword, Role.PATIENT);
        createUser(patientTwoEmail, patientPassword, Role.PATIENT);

        com.tus.cliniccare.entity.User doctorUser = createUser("doctor.cancel@cliniccare.com", patientPassword, Role.DOCTOR);

        Doctor doctor = new Doctor();
        doctor.setUser(doctorUser);
        doctor.setSpecialization("General");
        doctor = doctorRepository.save(doctor);
        this.doctorId = doctor.getId();

        ServiceEntity service = new ServiceEntity();
        service.setName("General Consultation");
        service.setDescription("General");
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
        slot.setStartTime(LocalDateTime.now().plusDays(1).withHour(14).withMinute(0).withSecond(0).withNano(0));
        slot.setEndTime(LocalDateTime.now().plusDays(1).withHour(14).withMinute(30).withSecond(0).withNano(0));
        slot.setStatus(TimeSlotStatus.AVAILABLE);
        slot = timeSlotRepository.save(slot);
        this.slotId = slot.getId();
    }

    @Test
    void onlyBookingPatientCanCancel_andSlotBecomesAvailable() throws Exception {
        String patientOneToken = loginAndGetToken(patientOneEmail, patientPassword);
        String patientTwoToken = loginAndGetToken(patientTwoEmail, patientPassword);

        String bookingPayload = """
                {
                  "doctorId": %d,
                  "serviceId": %d,
                  "timeSlotId": %d,
                  "patientNote": "Need consultation"
                }
                """.formatted(doctorId, serviceId, slotId);
        ResponseEntity<String> bookingResponse = restTemplate.exchange(
                "/api/patient/appointments",
                HttpMethod.POST,
                authorizedJsonEntity(patientOneToken, bookingPayload),
                String.class
        );
        assertEquals(HttpStatus.CREATED, bookingResponse.getStatusCode());
        Long appointmentId = objectMapper.readTree(bookingResponse.getBody()).get("id").asLong();

        ResponseEntity<String> otherPatientCancelResponse = restTemplate.exchange(
                "/api/patient/appointments/" + appointmentId + "/cancel",
                HttpMethod.PATCH,
                authorizedJsonEntity(patientTwoToken, null),
                String.class
        );
        assertEquals(HttpStatus.BAD_REQUEST, otherPatientCancelResponse.getStatusCode());

        ResponseEntity<String> ownerCancelResponse = restTemplate.exchange(
                "/api/patient/appointments/" + appointmentId + "/cancel",
                HttpMethod.PATCH,
                authorizedJsonEntity(patientOneToken, null),
                String.class
        );
        assertEquals(HttpStatus.OK, ownerCancelResponse.getStatusCode());
        JsonNode cancelledBody = objectMapper.readTree(ownerCancelResponse.getBody());
        assertEquals("CANCELLED", cancelledBody.get("status").asText());

        TimeSlot slot = timeSlotRepository.findById(slotId).orElseThrow();
        assertEquals(TimeSlotStatus.AVAILABLE, slot.getStatus());
    }
}
