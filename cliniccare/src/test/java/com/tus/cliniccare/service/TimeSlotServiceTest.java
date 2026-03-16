package com.tus.cliniccare.service;

import com.tus.cliniccare.entity.Doctor;
import com.tus.cliniccare.entity.TimeSlot;
import com.tus.cliniccare.entity.enums.TimeSlotStatus;
import com.tus.cliniccare.exception.ConflictException;
import com.tus.cliniccare.exception.ResourceNotFoundException;
import com.tus.cliniccare.repository.DoctorRepository;
import com.tus.cliniccare.repository.TimeSlotRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TimeSlotServiceTest {

    private static final Long DOCTOR_ID = 5L;

    @Mock
    private TimeSlotRepository timeSlotRepository;

    @Mock
    private DoctorRepository doctorRepository;

    @InjectMocks
    private TimeSlotService timeSlotService;

    @Test
    void getSlotsByDoctor_shouldReturnDoctorSlots() {
        TimeSlot slot1 = new TimeSlot();
        TimeSlot slot2 = new TimeSlot();
        List<TimeSlot> expected = List.of(slot1, slot2);

        when(timeSlotRepository.findByDoctorId(DOCTOR_ID)).thenReturn(expected);

        List<TimeSlot> result = timeSlotService.getSlotsByDoctor(DOCTOR_ID);

        assertSame(expected, result);
        assertEquals(2, result.size());
    }

    @Test
    void getAvailableSlotsByDoctor_shouldReturnOnlyAvailableSlots() {
        TimeSlot availableSlot = new TimeSlot();
        availableSlot.setStatus(TimeSlotStatus.AVAILABLE);
        List<TimeSlot> expected = List.of(availableSlot);

        when(timeSlotRepository.findByDoctorIdAndStatus(DOCTOR_ID, TimeSlotStatus.AVAILABLE)).thenReturn(expected);

        List<TimeSlot> result = timeSlotService.getAvailableSlotsByDoctor(DOCTOR_ID);

        assertSame(expected, result);
        assertEquals(TimeSlotStatus.AVAILABLE, result.get(0).getStatus());
    }

    @Test
    void hasOverlappingSlot_shouldReturnTrue_whenSlotOverlaps() {
        LocalDateTime requestedStart = LocalDateTime.of(2026, 3, 20, 10, 0);
        LocalDateTime requestedEnd = LocalDateTime.of(2026, 3, 20, 10, 30);

        TimeSlot existingSlot = new TimeSlot();
        existingSlot.setStartTime(LocalDateTime.of(2026, 3, 20, 9, 50));
        existingSlot.setEndTime(LocalDateTime.of(2026, 3, 20, 10, 10));

        when(timeSlotRepository.findByDoctorId(DOCTOR_ID)).thenReturn(List.of(existingSlot));

        boolean result = timeSlotService.hasOverlappingSlot(DOCTOR_ID, requestedStart, requestedEnd);

        assertTrue(result);
    }

    @Test
    void hasOverlappingSlot_shouldReturnFalse_whenSlotDoesNotOverlap() {
        LocalDateTime requestedStart = LocalDateTime.of(2026, 3, 20, 10, 0);
        LocalDateTime requestedEnd = LocalDateTime.of(2026, 3, 20, 10, 30);

        TimeSlot existingSlot = new TimeSlot();
        existingSlot.setStartTime(LocalDateTime.of(2026, 3, 20, 10, 30));
        existingSlot.setEndTime(LocalDateTime.of(2026, 3, 20, 11, 0));

        when(timeSlotRepository.findByDoctorId(DOCTOR_ID)).thenReturn(List.of(existingSlot));

        boolean result = timeSlotService.hasOverlappingSlot(DOCTOR_ID, requestedStart, requestedEnd);

        assertFalse(result);
    }

    @Test
    void hasOverlappingSlot_shouldReturnFalse_whenInputIsInvalid() {
        LocalDateTime start = LocalDateTime.of(2026, 3, 22, 10, 0);
        LocalDateTime end = LocalDateTime.of(2026, 3, 22, 10, 30);

        assertFalse(timeSlotService.hasOverlappingSlot(null, start, end));
        assertFalse(timeSlotService.hasOverlappingSlot(DOCTOR_ID, null, end));
        assertFalse(timeSlotService.hasOverlappingSlot(DOCTOR_ID, start, null));
        assertFalse(timeSlotService.hasOverlappingSlot(DOCTOR_ID, end, start));
        assertFalse(timeSlotService.hasOverlappingSlot(DOCTOR_ID, start, start));

        verifyNoInteractions(timeSlotRepository);
    }

    @Test
    void createTimeSlot_shouldSucceed_whenDoctorExistsAndNoOverlap() {
        Doctor doctor = new Doctor();
        doctor.setId(DOCTOR_ID);
        LocalDateTime start = LocalDateTime.of(2026, 3, 21, 9, 0);
        LocalDateTime end = LocalDateTime.of(2026, 3, 21, 9, 30);

        when(doctorRepository.findById(DOCTOR_ID)).thenReturn(Optional.of(doctor));
        when(timeSlotRepository.findByDoctorId(DOCTOR_ID)).thenReturn(List.of());
        when(timeSlotRepository.save(any(TimeSlot.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TimeSlot result = timeSlotService.createTimeSlot(DOCTOR_ID, start, end);

        assertEquals(DOCTOR_ID, result.getDoctor().getId());
        assertEquals(start, result.getStartTime());
        assertEquals(end, result.getEndTime());
        assertEquals(TimeSlotStatus.AVAILABLE, result.getStatus());
        verify(timeSlotRepository).save(any(TimeSlot.class));
    }

    @Test
    void createTimeSlot_shouldThrowConflict_whenOverlapExists() {
        Doctor doctor = new Doctor();
        doctor.setId(DOCTOR_ID);
        LocalDateTime start = LocalDateTime.of(2026, 3, 21, 10, 0);
        LocalDateTime end = LocalDateTime.of(2026, 3, 21, 10, 30);

        TimeSlot existingSlot = new TimeSlot();
        existingSlot.setStartTime(LocalDateTime.of(2026, 3, 21, 9, 45));
        existingSlot.setEndTime(LocalDateTime.of(2026, 3, 21, 10, 15));

        when(doctorRepository.findById(DOCTOR_ID)).thenReturn(Optional.of(doctor));
        when(timeSlotRepository.findByDoctorId(DOCTOR_ID)).thenReturn(List.of(existingSlot));

        assertThrows(
                ConflictException.class,
                () -> timeSlotService.createTimeSlot(DOCTOR_ID, start, end)
        );

        verify(timeSlotRepository, never()).save(any(TimeSlot.class));
    }

    @Test
    void createTimeSlot_shouldThrowResourceNotFound_whenDoctorMissing() {
        LocalDateTime start = LocalDateTime.of(2026, 3, 21, 11, 0);
        LocalDateTime end = LocalDateTime.of(2026, 3, 21, 11, 30);
        when(doctorRepository.findById(DOCTOR_ID)).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> timeSlotService.createTimeSlot(DOCTOR_ID, start, end)
        );

        verify(timeSlotRepository, never()).findByDoctorId(any());
        verify(timeSlotRepository, never()).save(any(TimeSlot.class));
    }
}
