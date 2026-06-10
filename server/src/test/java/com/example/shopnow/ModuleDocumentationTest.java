package com.example.shopnow;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

/**
 * Generates Module Documentation (C4/PlantUML component diagrams + module
 * canvases) for every discovered Application Module, listing each module's
 * Named Interfaces and declared allowedDependencies.
 *
 * <p><b>Validates: Requirements 9.1, 9.2, 9.3, 9.4, 9.5.</b>
 *
 * <p>Output is written under {@code target/spring-modulith-docs/}. Structural
 * refactor spec — no Property-Based Testing.
 */
class ModuleDocumentationTest {

    static final ApplicationModules modules = ApplicationModules.of(ShopnowApplication.class);

    @Test
    void writesDocumentationSnippets() {
        new Documenter(modules)
                .writeModulesAsPlantUml()
                .writeIndividualModulesAsPlantUml()
                .writeModuleCanvases();
    }
}
