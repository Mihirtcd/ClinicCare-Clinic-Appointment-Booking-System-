package com.tus.cliniccare.controller;

import com.tus.cliniccare.entity.Doctor;
import com.tus.cliniccare.entity.TimeSlot;
import com.tus.cliniccare.entity.enums.TimeSlotStatus;
import com.tus.cliniccare.exception.GlobalExceptionHandler;
import com.tus.cliniccare.security.CustomUserDetailsService;
import com.tus.cliniccare.security.JwtService;
import com.tus.cliniccare.service.TimeSlotService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminTimeSlotController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class AdminTimeSlotControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TimeSlotService timeSlotService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void createTimeSlot_shouldReturnCreatedSlot() throws Exception {
        LocalDateTime start = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime end = start.plusMinutes(30);
        TimeSlot slot = slotEntity(1L, 10L, start, end, TimeSlotStatus.AVAILABLE);

        when(timeSlotService.createTimeSlot(eq(10L), eq(start), eq(end))).thenReturn(slot);

        String payload = """
                {
                  "doctorId":10,
                  "startTime":"%s",
                  "endTime":"%s"
                }
                """.formatted(start, end);

        mockMvc.perform(post("/api/admin/timeslots")
                        .contentType("application/json")
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.doctorId").value(10))
                .andExpect(jsonPath("$.status").value("AVAILABLE"));
    }

    @Test
    void getSlotsByDoctor_shouldReturnSlotList() throws Exception {
        TimeSlot slot1 = slotEntity(
                1L,
                10L,
                LocalDateTime.of(2026, 3, 20, 10, 0),
                LocalDateTime.of(2026, 3, 20, 10, 30),
                TimeSlotStatus.AVAILABLE
        );
        TimeSlot slot2 = slotEntity(
                2L,
                10L,
                LocalDateTime.of(2026, 3, 20, 11, 0),
                LocalDateTime.of(2026, 3, 20, 11, 30),
                TimeSlotStatus.RESERVED
        );
        when(timeSlotService.getSlotsByDoctor(10L)).thenReturn(List.of(slot1, slot2));

        mockMvc.perform(get("/api/admin/timeslots/doctor/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].doctorId").value(10))
                .andExpect(jsonPath("$[1].status").value("RESERVED"));
    }

    private TimeSlot slotEntity(Long slotId, Long doctorId, LocalDateTime start, LocalDateTime end, TimeSlotStatus status) {
        Doctor doctor = new Doctor();
        doctor.setId(doctorId);

        TimeSlot slot = new TimeSlot();
        slot.setId(slotId);
        slot.setDoctor(doctor);
        slot.setStartTime(start);
        slot.setEndTime(end);
        slot.setStatus(status);
        return slot;
    }
}
