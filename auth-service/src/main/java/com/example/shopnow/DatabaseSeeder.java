package com.example.shopnow;

import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class DatabaseSeeder implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;

    public DatabaseSeeder(JdbcTemplate jdbcTemplate, PasswordEncoder passwordEncoder) {
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users", Integer.class);
        if (count != null && count == 0) {
            String encodedPassword = passwordEncoder.encode("password");

            String insertSql = "INSERT INTO users (id, email, password, name, role) VALUES (?, ?, ?, ?, ?)";

            // Admin
            jdbcTemplate.update(insertSql,
                    UUID.fromString("11111111-1111-1111-1111-111111111111"),
                    "admin@example.com", encodedPassword, "Admin User", "ADMIN");

            // Customer
            jdbcTemplate.update(insertSql,
                    UUID.fromString("22222222-2222-2222-2222-222222222222"),
                    "user@example.com", encodedPassword, "Customer User", "CUSTOMER");

            // Seller
            jdbcTemplate.update(insertSql,
                    UUID.fromString("33333333-3333-3333-3333-333333333333"),
                    "seller@example.com", encodedPassword, "Seller User", "SELLER");

            System.out.println("🌱 Auth Service Data Seeded Successfully!");
        }
    }
}
