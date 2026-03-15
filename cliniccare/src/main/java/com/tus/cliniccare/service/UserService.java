package com.tus.cliniccare.service;

import com.tus.cliniccare.entity.enums.Role;
import com.tus.cliniccare.exception.BadRequestException;
import com.tus.cliniccare.exception.ConflictException;
import com.tus.cliniccare.entity.User;
import com.tus.cliniccare.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public List<User> getUsersByRole(Role role) {
        return userRepository.findByRole(role);
    }

    public boolean emailExists(String email) {
        return userRepository.findByEmail(email).isPresent();
    }

    @Transactional
    public User registerPatient(
            String firstName,
            String lastName,
            String email,
            String rawPassword,
            String phoneNumber
    ) {
        if (emailExists(email)) {
            throw new ConflictException("Email is already registered.");
        }

        User user = new User();
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setPhoneNumber(phoneNumber);
        user.setRole(Role.PATIENT);

        return userRepository.save(user);
    }

    @Transactional
    public User createUserByAdmin(
            String firstName,
            String lastName,
            String email,
            String rawPassword,
            String phoneNumber,
            Role role
    ) {
        if (role != Role.DOCTOR) {
            throw new BadRequestException("Admin can only create users with DOCTOR role.");
        }

        if (emailExists(email)) {
            throw new ConflictException("Email is already registered.");
        }

        User user = new User();
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setPhoneNumber(phoneNumber);
        user.setRole(role);

        return userRepository.save(user);
    }
}
