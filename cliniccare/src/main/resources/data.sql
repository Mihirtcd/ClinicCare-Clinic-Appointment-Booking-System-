-- 1) Seed base users (ADMIN + 2 DOCTORS)
INSERT INTO users (first_name, last_name, email, password, phone_number, role)
VALUES
    ('Admin', 'User', 'admin@cliniccare.com', '$2y$10$2lMwTZVE5xKR1MPHbxpfbOjoxvQIR5GWAdny6nk0XW6cA.WxcMZoO', '1111111111', 'ADMIN'),
    ('Doctor', 'User', 'doctor@cliniccare.com', '$2y$10$2lMwTZVE5xKR1MPHbxpfbOjoxvQIR5GWAdny6nk0XW6cA.WxcMZoO', '2222222222', 'DOCTOR'),
    ('Dentist', 'User', 'dentist@cliniccare.com', '$2y$10$2lMwTZVE5xKR1MPHbxpfbOjoxvQIR5GWAdny6nk0XW6cA.WxcMZoO', '3333333333', 'DOCTOR')
ON DUPLICATE KEY UPDATE
    first_name = VALUES(first_name),
    last_name = VALUES(last_name),
    password = VALUES(password),
    phone_number = VALUES(phone_number),
    role = VALUES(role);

-- 2) Create doctor profiles linked to DOCTOR role users
INSERT INTO doctors (user_id, specialization)
SELECT u.id, 'General Physician'
FROM users u
WHERE u.email = 'doctor@cliniccare.com'
ON DUPLICATE KEY UPDATE
    specialization = VALUES(specialization);

INSERT INTO doctors (user_id, specialization)
SELECT u.id, 'Dentist'
FROM users u
WHERE u.email = 'dentist@cliniccare.com'
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

-- 4) Map doctors to services based on specialization
INSERT INTO doctor_services (doctor_id, service_id)
SELECT d.id, s.id
FROM doctors d
JOIN users u ON u.id = d.user_id
JOIN services s ON s.name = 'General Consultation'
WHERE u.email = 'doctor@cliniccare.com'
ON DUPLICATE KEY UPDATE
    doctor_id = VALUES(doctor_id);

-- Remove any old incorrect GP -> Dental mapping if it exists
DELETE ds
FROM doctor_services ds
JOIN doctors d ON d.id = ds.doctor_id
JOIN users u ON u.id = d.user_id
JOIN services s ON s.id = ds.service_id
WHERE u.email = 'doctor@cliniccare.com'
  AND s.name = 'Dental Checkup';

INSERT INTO doctor_services (doctor_id, service_id)
SELECT d.id, s.id
FROM doctors d
JOIN users u ON u.id = d.user_id
JOIN services s ON s.name = 'Dental Checkup'
WHERE u.email = 'dentist@cliniccare.com'
ON DUPLICATE KEY UPDATE
    doctor_id = VALUES(doctor_id);

-- 5) Seed available appointment time slots
-- General physician slots
INSERT INTO time_slots (doctor_id, start_time, end_time, status)
SELECT d.id,
       TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL 1 DAY), '09:00:00'),
       TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL 1 DAY), '09:30:00'),
       'AVAILABLE'
FROM doctors d
JOIN users u ON u.id = d.user_id
WHERE u.email = 'doctor@cliniccare.com'
ON DUPLICATE KEY UPDATE
    status = VALUES(status);

INSERT INTO time_slots (doctor_id, start_time, end_time, status)
SELECT d.id,
       TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL 1 DAY), '10:00:00'),
       TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL 1 DAY), '10:30:00'),
       'AVAILABLE'
FROM doctors d
JOIN users u ON u.id = d.user_id
WHERE u.email = 'doctor@cliniccare.com'
ON DUPLICATE KEY UPDATE
    status = VALUES(status);

INSERT INTO time_slots (doctor_id, start_time, end_time, status)
SELECT d.id,
       TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL 1 DAY), '11:00:00'),
       TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL 1 DAY), '11:30:00'),
       'AVAILABLE'
FROM doctors d
JOIN users u ON u.id = d.user_id
WHERE u.email = 'doctor@cliniccare.com'
ON DUPLICATE KEY UPDATE
    status = VALUES(status);

-- Dentist slots
INSERT INTO time_slots (doctor_id, start_time, end_time, status)
SELECT d.id,
       TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL 1 DAY), '12:00:00'),
       TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL 1 DAY), '12:30:00'),
       'AVAILABLE'
FROM doctors d
JOIN users u ON u.id = d.user_id
WHERE u.email = 'dentist@cliniccare.com'
ON DUPLICATE KEY UPDATE
    status = VALUES(status);

INSERT INTO time_slots (doctor_id, start_time, end_time, status)
SELECT d.id,
       TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL 1 DAY), '13:00:00'),
       TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL 1 DAY), '13:30:00'),
       'AVAILABLE'
FROM doctors d
JOIN users u ON u.id = d.user_id
WHERE u.email = 'dentist@cliniccare.com'
ON DUPLICATE KEY UPDATE
    status = VALUES(status);

INSERT INTO time_slots (doctor_id, start_time, end_time, status)
SELECT d.id,
       TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL 1 DAY), '14:00:00'),
       TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL 1 DAY), '14:30:00'),
       'AVAILABLE'
FROM doctors d
JOIN users u ON u.id = d.user_id
WHERE u.email = 'dentist@cliniccare.com'
ON DUPLICATE KEY UPDATE
    status = VALUES(status);
