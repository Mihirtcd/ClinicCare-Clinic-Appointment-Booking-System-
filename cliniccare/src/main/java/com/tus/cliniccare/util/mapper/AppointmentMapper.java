package com.tus.cliniccare.util.mapper;

import com.tus.cliniccare.dto.response.AppointmentResponse;
import com.tus.cliniccare.entity.Appointment;

public final class AppointmentMapper {

    private AppointmentMapper() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static AppointmentResponse toResponse(Appointment appointment) {
        AppointmentResponse response = new AppointmentResponse();
        response.setId(appointment.getId());
        response.setPatientId(appointment.getPatient().getId());
        response.setPatientName(
                ((appointment.getPatient().getFirstName() == null ? "" : appointment.getPatient().getFirstName()) + " "
                        + (appointment.getPatient().getLastName() == null ? "" : appointment.getPatient().getLastName()))
                        .trim()
        );
        response.setDoctorId(appointment.getDoctor().getId());
        response.setDoctorName(
                ((appointment.getDoctor().getUser().getFirstName() == null ? "" : appointment.getDoctor().getUser().getFirstName()) + " "
                        + (appointment.getDoctor().getUser().getLastName() == null ? "" : appointment.getDoctor().getUser().getLastName()))
                        .trim()
        );
        response.setServiceId(appointment.getService().getId());
        response.setServiceName(appointment.getService().getName());
        response.setTimeSlotId(appointment.getTimeSlot().getId());
        response.setSlotStartTime(appointment.getTimeSlot().getStartTime());
        response.setSlotEndTime(appointment.getTimeSlot().getEndTime());
        response.setStatus(appointment.getStatus());
        response.setPatientNote(appointment.getPatientNote());
        response.setBookedAt(appointment.getBookedAt());
        return response;
    }
}
