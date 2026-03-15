-- 1) Seed base users (ADMIN + DOCTOR)
INSERT INTO users (first_name, last_name, email, password, phone_number, role)
VALUES
    ('Admin', 'User', 'admin@cliniccare.com', '$2y$10$2lMwTZVE5xKR1MPHbxpfbOjoxvQIR5GWAdny6nk0XW6cA.WxcMZoO', '1111111111', 'ADMIN'),
    ('Doctor', 'User', 'doctor@cliniccare.com', '$2y$10$2lMwTZVE5xKR1MPHbxpfbOjoxvQIR5GWAdny6nk0XW6cA.WxcMZoO', '2222222222', 'DOCTOR')
ON DUPLICATE KEY UPDATE
    first_name = VALUES(first_name),
    last_name = VALUES(last_name),
    password = VALUES(password),
    phone_number = VALUES(phone_number),
    role = VALUES(role);

-- 2) Create doctor profile linked to the DOCTOR role user
INSERT INTO doctors (user_id, specialization)
SELECT u.id, 'General Physician'
FROM users u
WHERE u.email = 'doctor@cliniccare.com'
ON DUPLICATE KEY UPDATE
    specialization = VALUES(specialization);

-- 3) Seed service catalog (enabled by default)
INSERT INTO services (name, description, duration_minutes, is_enabled)
VALUES
    ('General Consultation', 'Basic consultation with a doctor', 30, TRUE),
    ('Dental Checkup', 'Routine dental diagnosis and advice', 45, TRUE)
ON DUPLICATE KEY UPDATE
    description = VALUES(description),
    duration_minutes = VALUES(duration_minutes),
    is_enabled = VALUES(is_enabled);

-- 4) Map doctor to available services
INSERT INTO doctor_services (doctor_id, service_id)
SELECT d.id, s.id
FROM doctors d
JOIN users u ON u.id = d.user_id
JOIN services s ON s.name = 'General Consultation'
WHERE u.email = 'doctor@cliniccare.com'
ON DUPLICATE KEY UPDATE
    doctor_id = VALUES(doctor_id);

INSERT INTO doctor_services (doctor_id, service_id)
SELECT d.id, s.id
FROM doctors d
JOIN users u ON u.id = d.user_id
JOIN services s ON s.name = 'Dental Checkup'
WHERE u.email = 'doctor@cliniccare.com'
ON DUPLICATE KEY UPDATE
    doctor_id = VALUES(doctor_id);

-- 5) Seed three available appointment time slots
INSERT INTO time_slots (doctor_id, start_time, end_time, status)
SELECT d.id, '2030-01-15 09:00:00', '2030-01-15 09:30:00', 'AVAILABLE'
FROM doctors d
JOIN users u ON u.id = d.user_id
WHERE u.email = 'doctor@cliniccare.com'
ON DUPLICATE KEY UPDATE
    status = VALUES(status);

INSERT INTO time_slots (doctor_id, start_time, end_time, status)
SELECT d.id, '2030-01-15 10:00:00', '2030-01-15 10:30:00', 'AVAILABLE'
FROM doctors d
JOIN users u ON u.id = d.user_id
WHERE u.email = 'doctor@cliniccare.com'
ON DUPLICATE KEY UPDATE
    status = VALUES(status);

INSERT INTO time_slots (doctor_id, start_time, end_time, status)
SELECT d.id, '2030-01-15 11:00:00', '2030-01-15 11:30:00', 'AVAILABLE'
FROM doctors d
JOIN users u ON u.id = d.user_id
WHERE u.email = 'doctor@cliniccare.com'
ON DUPLICATE KEY UPDATE
    status = VALUES(status);
