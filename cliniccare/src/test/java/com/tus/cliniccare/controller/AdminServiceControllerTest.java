package com.tus.cliniccare.controller;

import com.tus.cliniccare.entity.ServiceEntity;
import com.tus.cliniccare.exception.GlobalExceptionHandler;
import com.tus.cliniccare.security.CustomUserDetailsService;
import com.tus.cliniccare.security.JwtService;
import com.tus.cliniccare.service.ServiceEntityService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminServiceController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class AdminServiceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ServiceEntityService serviceEntityService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void createService_shouldReturnCreatedService() throws Exception {
        ServiceEntity created = serviceEntity(1L, "Cardio Consultation", true, 30);
        when(serviceEntityService.createService(
                eq("Cardio Consultation"),
                eq("Cardio test service"),
                eq(30),
                eq(true)
        )).thenReturn(created);

        String payload = """
                {
                  "name":"Cardio Consultation",
                  "description":"Cardio test service",
                  "durationMinutes":30,
                  "isEnabled":true
                }
                """;

        mockMvc.perform(post("/api/admin/services")
                        .contentType("application/json")
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Cardio Consultation"))
                .andExpect(jsonPath("$.isEnabled").value(true));
    }

    @Test
    void getAllServices_shouldReturnServiceList() throws Exception {
        ServiceEntity s1 = serviceEntity(1L, "General Consultation", true, 20);
        ServiceEntity s2 = serviceEntity(2L, "Dental Checkup", false, 25);

        when(serviceEntityService.getAllServices()).thenReturn(List.of(s1, s2));

        mockMvc.perform(get("/api/admin/services"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("General Consultation"))
                .andExpect(jsonPath("$[1].name").value("Dental Checkup"));
    }

    @Test
    void updateService_shouldReturnUpdatedService() throws Exception {
        ServiceEntity updated = serviceEntity(5L, "Updated Service", false, 45);
        when(serviceEntityService.updateService(
                eq(5L),
                eq("Updated Service"),
                eq("Updated Description"),
                eq(45),
                eq(false)
        )).thenReturn(updated);

        String payload = """
                {
                  "name":"Updated Service",
                  "description":"Updated Description",
                  "durationMinutes":45,
                  "isEnabled":false
                }
                """;

        mockMvc.perform(put("/api/admin/services/5")
                        .contentType("application/json")
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.name").value("Updated Service"))
                .andExpect(jsonPath("$.isEnabled").value(false));
    }

    @Test
    void disableService_shouldReturnDisabledService() throws Exception {
        ServiceEntity disabled = serviceEntity(7L, "Dental Checkup", false, 25);
        when(serviceEntityService.disableService(7L)).thenReturn(disabled);

        mockMvc.perform(patch("/api/admin/services/7/disable"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.isEnabled").value(false));
    }

    private ServiceEntity serviceEntity(Long id, String name, Boolean enabled, Integer durationMinutes) {
        ServiceEntity service = new ServiceEntity();
        service.setId(id);
        service.setName(name);
        service.setDescription("Description");
        service.setDurationMinutes(durationMinutes);
        service.setIsEnabled(enabled);
        return service;
    }
}
