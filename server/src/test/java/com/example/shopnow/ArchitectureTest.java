package com.example.shopnow;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ArchitectureTest {
    static final ApplicationModules modules = ApplicationModules.of(ShopnowApplication.class);

    @Test
    void verifiesModuleBoundaries() {
        modules.verify();
    }
}
