package org.example.authserver;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Profile("seed-data")
public class UserDataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(UserDataInitializer.class);

    @PersistenceContext
    private EntityManager entityManager;

    private final PasswordEncoder passwordEncoder;

    public UserDataInitializer(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        log.info("Starting user data seeding...");

        // Check if users already exist
        Long count = entityManager.createQuery("SELECT COUNT(u) FROM User u", Long.class).getSingleResult();
        if (count > 0) {
            log.info("Users already exist ({} found), skipping seeding", count);
            return;
        }

        // Admin user
        User admin = new User();
        admin.setEmail("admin@example.com");
        admin.setPasswordHash(passwordEncoder.encode("admin123"));
        admin.setFirstName("Admin");
        admin.setLastName("User");
        admin.setPhoneNumber("+1-555-0001");
        admin.setRoles(List.of("ROLE_ADMIN", "ROLE_USER", "ROLE_MANAGER"));
        admin.setEnabled(true);
        entityManager.persist(admin);

        // Regular users
        User user1 = new User();
        user1.setEmail("user1@example.com");
        user1.setPasswordHash(passwordEncoder.encode("password123"));
        user1.setFirstName("John");
        user1.setLastName("Doe");
        user1.setPhoneNumber("+1-555-0002");
        user1.setRoles(List.of("ROLE_USER"));
        user1.setEnabled(true);
        entityManager.persist(user1);

        User user2 = new User();
        user2.setEmail("user2@example.com");
        user2.setPasswordHash(passwordEncoder.encode("password123"));
        user2.setFirstName("Jane");
        user2.setLastName("Smith");
        user2.setPhoneNumber("+1-555-0003");
        user2.setRoles(List.of("ROLE_USER"));
        user2.setEnabled(true);
        entityManager.persist(user2);

        User user3 = new User();
        user3.setEmail("user3@example.com");
        user3.setPasswordHash(passwordEncoder.encode("password123"));
        user3.setFirstName("Bob");
        user3.setLastName("Wilson");
        user3.setPhoneNumber("+1-555-0004");
        user3.setRoles(List.of("ROLE_USER", "ROLE_PREMIUM"));
        user3.setEnabled(true);
        entityManager.persist(user3);

        User user4 = new User();
        user4.setEmail("user4@example.com");
        user4.setPasswordHash(passwordEncoder.encode("password123"));
        user4.setFirstName("Alice");
        user4.setLastName("Brown");
        user4.setPhoneNumber("+1-555-0005");
        user4.setRoles(List.of("ROLE_USER"));
        user4.setEnabled(true);
        entityManager.persist(user4);

        // Inactive user for testing
        User inactiveUser = new User();
        inactiveUser.setEmail("inactive@example.com");
        inactiveUser.setPasswordHash(passwordEncoder.encode("password123"));
        inactiveUser.setFirstName("Inactive");
        inactiveUser.setLastName("User");
        inactiveUser.setPhoneNumber("+1-555-0006");
        inactiveUser.setRoles(List.of("ROLE_USER"));
        inactiveUser.setEnabled(false);
        entityManager.persist(inactiveUser);

        entityManager.flush();

        log.info("User data seeding completed! Created 6 users (1 admin, 4 regular, 1 inactive)");
    }
}