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
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM shops", Integer.class);
        if (count != null && count == 0) {
            
            UUID shopId = UUID.fromString("55555555-5555-5555-5555-555555555555");
            UUID sellerId = UUID.fromString("33333333-3333-3333-3333-333333333333");
            UUID catId = UUID.fromString("cccccccc-1111-1111-1111-111111111111");
            UUID p1Id = UUID.fromString("bbbbbbbb-1111-1111-1111-111111111111");
            UUID p2Id = UUID.fromString("bbbbbbbb-2222-2222-2222-222222222222");

            // Shop
            jdbcTemplate.update("INSERT INTO shops (id, name, address, avatar_url, is_active, owner_id) VALUES (?, ?, ?, ?, ?, ?)",
                    shopId, "Tech Store", "HCM", "", true, sellerId);

            // Category
            jdbcTemplate.update("INSERT INTO categories (id, name) VALUES (?, ?)",
                    catId, "Electronics");

            // Products
            jdbcTemplate.update("INSERT INTO products (id, name, picture_url, quantity, price, status, is_deleted, shop_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                    p1Id, "Gaming Laptop", "https://picsum.photos/400/400?1", 10, 1500.0, "ACTIVE", false, shopId);

            jdbcTemplate.update("INSERT INTO products (id, name, picture_url, quantity, price, status, is_deleted, shop_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                    p2Id, "Smartphone", "https://picsum.photos/400/400?2", 20, 800.0, "ACTIVE", false, shopId);

            // Product Categories
            jdbcTemplate.update("INSERT INTO product_categories (product_id, category_id) VALUES (?, ?)", p1Id, catId);
            jdbcTemplate.update("INSERT INTO product_categories (product_id, category_id) VALUES (?, ?)", p2Id, catId);

            System.out.println("🌱 Product Service Data Seeded Successfully!");
        }
    }
}
