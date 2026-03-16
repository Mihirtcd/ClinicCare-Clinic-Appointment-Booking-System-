package com.tus.cliniccare.repository;

import com.tus.cliniccare.entity.User;
import com.tus.cliniccare.entity.enums.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:user_repository_test;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.sql.init.mode=never",
        "app.jwt.secret=Y2xpbmljY2FyZS1hY2FkZW1pYy1qd3Qtc2VjcmV0LWtleS1mb3ItcHJvamVjdC1kZW1vLXNpZ25pbmctMzJieXRlcw=="
})
@Transactional
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void findByEmail_shouldReturnSavedUser() {
        User user = userRepository.save(newUser("patient.repo@cliniccare.com", Role.PATIENT));

        Optional<User> found = userRepository.findByEmail("patient.repo@cliniccare.com");

        assertTrue(found.isPresent());
        assertEquals(user.getId(), found.get().getId());
        assertEquals(Role.PATIENT, found.get().getRole());
    }

    @Test
    void findByRole_shouldReturnOnlyMatchingRoleUsers() {
        userRepository.save(newUser("patient.repo.1@cliniccare.com", Role.PATIENT));
        userRepository.save(newUser("doctor.repo.1@cliniccare.com", Role.DOCTOR));

        List<User> doctors = userRepository.findByRole(Role.DOCTOR);

        assertEquals(1, doctors.size());
        assertEquals("doctor.repo.1@cliniccare.com", doctors.get(0).getEmail());
    }

    @Test
    void save_shouldThrowForDuplicateEmail() {
        userRepository.saveAndFlush(newUser("duplicate.repo@cliniccare.com", Role.PATIENT));

        assertThrows(
                DataIntegrityViolationException.class,
                () -> userRepository.saveAndFlush(newUser("duplicate.repo@cliniccare.com", Role.DOCTOR))
        );
    }

    private User newUser(String email, Role role) {
        User user = new User();
        user.setFirstName("Test");
        user.setLastName("User");
        user.setEmail(email);
        user.setPassword("encoded-password");
        user.setPhoneNumber("9999999999");
        user.setRole(role);
        return user;
    }
}
