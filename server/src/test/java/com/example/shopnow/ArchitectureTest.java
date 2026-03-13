package com.example.shopnow;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

public class ArchitectureTest {
    @Test
    void verifyBoundary() {
        ApplicationModules.of(ShopnowApplication.class).verify();
    }
}
