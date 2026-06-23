package com.example.shopnow;

import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class DatabaseSeeder implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseSeeder(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM user_profiles", Integer.class);
        if (count != null && count == 0) {
            
            // Admin Profile
            jdbcTemplate.update("INSERT INTO user_profiles (id, user_id, phone, address, avatar_url) VALUES (?, ?, ?, ?, ?)",
                    UUID.randomUUID(), UUID.fromString("11111111-1111-1111-1111-111111111111"),
                    "0987654321", "Hanoi, Vietnam", "https://i.pravatar.cc/150?img=11");

            // Customer Profile
            jdbcTemplate.update("INSERT INTO user_profiles (id, user_id, phone, address, avatar_url) VALUES (?, ?, ?, ?, ?)",
                    UUID.randomUUID(), UUID.fromString("22222222-2222-2222-2222-222222222222"),
                    "0123456789", "HCMC, Vietnam", "https://i.pravatar.cc/150?img=12");

            // Seller Profile
            jdbcTemplate.update("INSERT INTO user_profiles (id, user_id, phone, address, avatar_url) VALUES (?, ?, ?, ?, ?)",
                    UUID.randomUUID(), UUID.fromString("33333333-3333-3333-3333-333333333333"),
                    "0912345678", "Da Nang, Vietnam", "https://i.pravatar.cc/150?img=13");

            System.out.println("? User Service Data Seeded!");
        }
    }
}
