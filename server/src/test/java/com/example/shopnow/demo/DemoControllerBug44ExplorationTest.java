package com.example.shopnow.demo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exploration test for BUG-44: DemoController leaks PII (email + role).
 *
 * <p><b>Validates: Requirements 2.44</b>
 *
 * <p>Property 38: Bug Condition — DemoController không leak PII.
 * Read DemoController.java source and assert the response body of the hello
 * endpoint does NOT contain user email or role.
 *
 * <p>On unfixed code this test FAILS because the hello method returns a string
 * that includes {@code user.getEmail()} and {@code user.getRole()}, leaking PII
 * in the response body.
 *
 * <p><b>Bug condition C(44):</b> {@code GET /demo/hello} response body contains
 * email OR role of the authenticated user.
 *
 * <p><b>Expected counterexample on unfixed code:</b>
 * {@code DemoController.helloWorld()} returns
 * {@code "Hello world " + user.getEmail() + " " + user.getRole()} — response
 * body contains PII (email and role).
 */
class DemoControllerBug44ExplorationTest {

    private static final Path DEMO_CONTROLLER_SOURCE = Paths.get(
            "src", "main", "java", "com", "example", "shopnow", "demo", "DemoController.java");

    /**
     * Asserts that DemoController's hello endpoint does NOT leak PII.
     *
     * <p>Strategy: Read the source file and check that the method body of
     * the hello endpoint does not reference {@code getEmail()} or
     * {@code getRole()} in its return statement. If the controller has been
     * deleted entirely (endpoint returns 404), the property is also satisfied.
     */
    @Test
    @DisplayName("BUG-44: DemoController hello endpoint SHALL NOT leak email or role in response body")
    void demoController_helloEndpoint_shallNotLeakPII() throws IOException {
        // If DemoController source has been deleted, the bug is fixed (no endpoint = no leak)
        if (!Files.exists(DEMO_CONTROLLER_SOURCE)) {
            return; // Property satisfied — endpoint removed
        }

        List<String> lines = Files.readAllLines(DEMO_CONTROLLER_SOURCE);
        String sourceContent = String.join("\n", lines);

        // Check if the hello method body contains PII-leaking patterns
        // We look for getEmail() or getRole() usage in the return statement context
        boolean leaksEmail = sourceContent.contains("getEmail()");
        boolean leaksRole = sourceContent.contains("getRole()");

        assertThat(leaksEmail)
                .as("DemoController hello endpoint SHALL NOT include user.getEmail() in response. "
                        + "Counterexample: return statement contains getEmail() → response body "
                        + "exposes user's email address (PII leak).")
                .isFalse();

        assertThat(leaksRole)
                .as("DemoController hello endpoint SHALL NOT include user.getRole() in response. "
                        + "Counterexample: return statement contains getRole() → response body "
                        + "exposes user's role (PII leak).")
                .isFalse();
    }

    /**
     * Complementary reflection check: if DemoController class exists, verify
     * that the hello method does NOT accept a User parameter (which would
     * enable PII access). If the class is deleted, property is satisfied.
     */
    @Test
    @DisplayName("BUG-44: DemoController hello method SHALL NOT accept User parameter for PII access")
    void demoController_helloMethod_shallNotAcceptUserParam() {
        Class<?> controllerClass;
        try {
            controllerClass = Class.forName("com.example.shopnow.demo.DemoController");
        } catch (ClassNotFoundException e) {
            // Controller deleted — property satisfied
            return;
        }

        // Find the hello method — check if it has a User-typed parameter
        boolean hasUserParam = false;
        for (Method method : controllerClass.getDeclaredMethods()) {
            if (method.getName().equals("helloWorld") || method.getName().equals("hello")) {
                for (Class<?> paramType : method.getParameterTypes()) {
                    if (paramType.getSimpleName().equals("User")) {
                        hasUserParam = true;
                        break;
                    }
                }
            }
        }

        assertThat(hasUserParam)
                .as("DemoController hello method SHALL NOT accept a User parameter "
                        + "(enables PII access). Counterexample: method signature includes "
                        + "@AuthenticationPrincipal User user → can leak email/role in response.")
                .isFalse();
    }
}
