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
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM orders", Integer.class);
        if (count != null && count == 0) {
            
            UUID orderId = UUID.randomUUID();
            UUID subOrderId = UUID.randomUUID();
            UUID orderDetailId = UUID.randomUUID();

            UUID customerId = UUID.fromString("22222222-2222-2222-2222-222222222222");
            UUID sellerId = UUID.fromString("33333333-3333-3333-3333-333333333333");
            UUID shopId = UUID.fromString("55555555-5555-5555-5555-555555555555");
            UUID productId = UUID.fromString("bbbbbbbb-1111-1111-1111-111111111111");

            // Order
            jdbcTemplate.update("INSERT INTO orders (id, status, customer_id, total_price, address_shipping, phone_number, customer_name) VALUES (?, ?, ?, ?, ?, ?, ?)",
                    orderId, "PENDING_PAYMENT", customerId, 1500.0, "HCM", "0123456789", "Customer User");

            // SubOrder
            jdbcTemplate.update("INSERT INTO sub_orders (id, order_id, status, shop_id, shop_owner_id, total_price) VALUES (?, ?, ?, ?, ?, ?)",
                    subOrderId, orderId, "PENDING_PAYMENT", shopId, sellerId, 1500.0);

            // OrderDetail
            jdbcTemplate.update("INSERT INTO order_detail (id, sub_order_id, product_name, product_id, price, quantity) VALUES (?, ?, ?, ?, ?, ?)",
                    orderDetailId, subOrderId, "Gaming Laptop", productId, 1500.0, 1);

            System.out.println("? Order Service Data Seeded!");
        }
    }
}
