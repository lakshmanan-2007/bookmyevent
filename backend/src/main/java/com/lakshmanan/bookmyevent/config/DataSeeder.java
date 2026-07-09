package com.lakshmanan.bookmyevent.config;

import com.lakshmanan.bookmyevent.domain.Event;
import com.lakshmanan.bookmyevent.domain.Role;
import com.lakshmanan.bookmyevent.domain.User;
import com.lakshmanan.bookmyevent.repository.EventRepository;
import com.lakshmanan.bookmyevent.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Seeds demo data (dev and prod) on first boot only (idempotent: skipped if data exists) so the app is usable immediately.
 * Login: admin@bookmyevent.com / Admin@123   and   demo@bookmyevent.com / Demo@123
 */
@Configuration
@Profile({"dev", "prod"})
public class DataSeeder {

    @Bean
    CommandLineRunner seed(UserRepository users, EventRepository events, PasswordEncoder encoder) {
        return args -> {
            if (users.count() == 0) {
                users.save(new User("Admin User", "admin@bookmyevent.com",
                        encoder.encode("Admin@123"), Role.ADMIN));
                users.save(new User("Demo User", "demo@bookmyevent.com",
                        encoder.encode("Demo@123"), Role.USER));
            }
            if (events.count() == 0) {
                events.save(new Event("Sunburn Music Festival", "Marina Grounds", "Chennai",
                        LocalDateTime.now().plusDays(30), 500, new BigDecimal("1499.00")));
                events.save(new Event("Tech Conclave 2026", "Convention Center", "Bengaluru",
                        LocalDateTime.now().plusDays(50), 200, new BigDecimal("999.00")));
                events.save(new Event("Standup Comedy Night", "Phoenix Arena", "Coimbatore",
                        LocalDateTime.now().plusDays(15), 3, new BigDecimal("599.00")));
            }
        };
    }
}
