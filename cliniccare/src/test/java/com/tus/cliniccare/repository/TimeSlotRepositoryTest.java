package com.tus.cliniccare.repository;

import com.tus.cliniccare.entity.Doctor;
import com.tus.cliniccare.entity.TimeSlot;
import com.tus.cliniccare.entity.User;
import com.tus.cliniccare.entity.enums.Role;
import com.tus.cliniccare.entity.enums.TimeSlotStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:time_slot_repository_test;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.sql.init.mode=never",
        "app.jwt.secret=Y2xpbmljY2FyZS1hY2FkZW1pYy1qd3Qtc2VjcmV0LWtleS1mb3ItcHJvamVjdC1kZW1vLXNpZ25pbmctMzJieXRlcw=="
})
@Transactional
class TimeSlotRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private TimeSlotRepository timeSlotRepository;

    @Test
    void findByDoctorIdAndStatus_shouldReturnOnlyMatchingSlots() {
        User doctorUser = new User();
        doctorUser.setFirstName("Doctor");
        doctorUser.setLastName("Repo");
        doctorUser.setEmail("doctor.timeslot.repo@cliniccare.com");
        doctorUser.setPassword("encoded-password");
        doctorUser.setPhoneNumber("9999999999");
        doctorUser.setRole(Role.DOCTOR);
        doctorUser = userRepository.saveAndFlush(doctorUser);

        Doctor doctor = new Doctor();
        doctor.setUser(doctorUser);
        doctor.setSpecialization("General");
        doctor = doctorRepository.saveAndFlush(doctor);

        timeSlotRepository.saveAndFlush(newSlot(doctor, TimeSlotStatus.AVAILABLE, 10, 0));
        timeSlotRepository.saveAndFlush(newSlot(doctor, TimeSlotStatus.RESERVED, 11, 0));

        List<TimeSlot> available = timeSlotRepository.findByDoctorIdAndStatus(doctor.getId(), TimeSlotStatus.AVAILABLE);
        List<TimeSlot> allSlots = timeSlotRepository.findByDoctorId(doctor.getId());

        assertEquals(1, available.size());
        assertEquals(TimeSlotStatus.AVAILABLE, available.get(0).getStatus());
        assertEquals(2, allSlots.size());
    }

    private TimeSlot newSlot(Doctor doctor, TimeSlotStatus status, int hour, int minute) {
        TimeSlot slot = new TimeSlot();
        slot.setDoctor(doctor);
        slot.setStartTime(LocalDateTime.now().plusDays(1).withHour(hour).withMinute(minute).withSecond(0).withNano(0));
        slot.setEndTime(LocalDateTime.now().plusDays(1).withHour(hour).withMinute(minute + 30).withSecond(0).withNano(0));
        slot.setStatus(status);
        return slot;
    }
}
