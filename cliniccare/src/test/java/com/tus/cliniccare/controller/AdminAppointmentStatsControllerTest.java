package com.tus.cliniccare.controller;

import com.tus.cliniccare.entity.enums.AppointmentStatus;
import com.tus.cliniccare.exception.GlobalExceptionHandler;
import com.tus.cliniccare.security.CustomUserDetailsService;
import com.tus.cliniccare.security.JwtService;
import com.tus.cliniccare.service.AppointmentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminAppointmentStatsController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class AdminAppointmentStatsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AppointmentService appointmentService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void getAppointmentStats_shouldReturnStatsPayload() throws Exception {
        when(appointmentService.getTotalAppointmentsCount()).thenReturn(15L);
        when(appointmentService.getAppointmentsCountByStatus(AppointmentStatus.PENDING)).thenReturn(4L);
        when(appointmentService.getAppointmentsCountByStatus(AppointmentStatus.CONFIRMED)).thenReturn(5L);
        when(appointmentService.getAppointmentsCountByStatus(AppointmentStatus.REJECTED)).thenReturn(2L);
        when(appointmentService.getAppointmentsCountByStatus(AppointmentStatus.COMPLETED)).thenReturn(3L);
        when(appointmentService.getAppointmentsCountByStatus(AppointmentStatus.CANCELLED)).thenReturn(1L);

        mockMvc.perform(get("/api/admin/appointments/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(15))
                .andExpect(jsonPath("$.pending").value(4))
                .andExpect(jsonPath("$.confirmed").value(5))
                .andExpect(jsonPath("$.rejected").value(2))
                .andExpect(jsonPath("$.completed").value(3))
                .andExpect(jsonPath("$.cancelled").value(1));
    }
}
