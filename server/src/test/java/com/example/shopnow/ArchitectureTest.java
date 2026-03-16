package com.example.shopnow;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

public class ArchitectureTest {
    ApplicationModules modules = ApplicationModules.of(ShopnowApplication.class);

    @Test
    void verifyBoundary() {
        modules.verify();
    }

    // @Test
    // void internal_class_should_not_be_access_from_outside() {
    //     JavaClasses importedClass = new ClassFileImporter().importPackages("com.example.shopnow");

    //     ArchRule encapsulationRule = classes()
    //             .that().resideInAPackage("..product..")
    //             .should().onlyBeAccessed().byAnyPackage("..product..");

    //     encapsulationRule.check(importedClass);
    // }

}
