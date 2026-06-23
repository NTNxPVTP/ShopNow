package com.example.shopnow.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exploration test for BUG-31 (thiếu register/refresh-token/logout).
 *
 * <p><b>Validates: Requirements 2.31</b> (Property 27 in design.md).
 *
 * <p>Phase&nbsp;1 bug-condition exploration test for the bugfix workflow
 * {@code shopnow-codebase-bugfixes}. It is EXPECTED TO FAIL on unfixed
 * code; the failure surfaces a counterexample that proves the bug
 * exists (no POST endpoints for register, refresh-token, logout).
 *
 * <p><b>Bug condition C(31):</b> paths {@code POST /api/auth/register},
 * {@code POST /api/auth/refresh-token}, {@code POST /api/auth/logout}
 * do not exist in any controller.
 *
 * <p><b>Property P(27):</b> for each of the three auth endpoints,
 * there SHALL exist a handler method annotated with {@code @PostMapping}
 * whose path contains the respective keyword. Status != 404 for valid input.
 *
 * <p><b>Test approach:</b> Reflection-based (no Spring context). Check
 * {@code AuthenticationController} class for handler methods mapped to
 * paths containing "register", "refresh-token", and "logout".
 *
 * <p><b>Expected counterexample on unfixed code:</b>
 * {@code AuthenticationController} only has {@code @PostMapping("/authenticate")};
 * no methods with paths containing "register", "refresh-token", or "logout"
 * exist → all three assertions fail → runtime 404 for these endpoints.
 */
class AuthControllerBug31ExplorationTest {

    private static final String CONTROLLER_CLASS_NAME =
            "com.example.shopnow.security.rest.AuthenticationController";

    /**
     * Asserts that {@code AuthenticationController} has a handler method
     * annotated with {@code @PostMapping} whose value/path contains
     * {@code "register"} — the endpoint for user registration.
     */
    @Test
    @DisplayName("AuthenticationController must have POST register endpoint")
    void authController_hasPostRegisterEndpoint() throws ClassNotFoundException {
        Class<?> controllerClass = Class.forName(CONTROLLER_CLASS_NAME);

        boolean hasRegister = hasPostMappingContaining(controllerClass, "register");

        assertThat(hasRegister)
                .as("AuthenticationController should have a @PostMapping handler "
                        + "with path containing 'register' for POST /api/auth/register. "
                        + "Counterexample: no such endpoint exists → runtime 404.")
                .isTrue();
    }

    /**
     * Asserts that {@code AuthenticationController} has a handler method
     * annotated with {@code @PostMapping} whose value/path contains
     * {@code "refresh-token"} — the endpoint for refreshing JWT tokens.
     */
    @Test
    @DisplayName("AuthenticationController must have POST refresh-token endpoint")
    void authController_hasPostRefreshTokenEndpoint() throws ClassNotFoundException {
        Class<?> controllerClass = Class.forName(CONTROLLER_CLASS_NAME);

        boolean hasRefreshToken = hasPostMappingContaining(controllerClass, "refresh-token");

        assertThat(hasRefreshToken)
                .as("AuthenticationController should have a @PostMapping handler "
                        + "with path containing 'refresh-token' for POST /api/auth/refresh-token. "
                        + "Counterexample: no such endpoint exists → runtime 404.")
                .isTrue();
    }

    /**
     * Asserts that {@code AuthenticationController} has a handler method
     * annotated with {@code @PostMapping} whose value/path contains
     * {@code "logout"} — the endpoint for user logout.
     */
    @Test
    @DisplayName("AuthenticationController must have POST logout endpoint")
    void authController_hasPostLogoutEndpoint() throws ClassNotFoundException {
        Class<?> controllerClass = Class.forName(CONTROLLER_CLASS_NAME);

        boolean hasLogout = hasPostMappingContaining(controllerClass, "logout");

        assertThat(hasLogout)
                .as("AuthenticationController should have a @PostMapping handler "
                        + "with path containing 'logout' for POST /api/auth/logout. "
                        + "Counterexample: no such endpoint exists → runtime 404.")
                .isTrue();
    }

    // -----------------------------------------------------------------------
    // Helper
    // -----------------------------------------------------------------------

    /**
     * Checks whether any method in the given class is annotated with
     * {@code @PostMapping} and the annotation's value/path array
     * contains a string matching the given keyword.
     */
    private boolean hasPostMappingContaining(Class<?> clazz, String keyword) {
        for (Method method : clazz.getDeclaredMethods()) {
            // Check for @PostMapping
            org.springframework.web.bind.annotation.PostMapping postMapping =
                    method.getAnnotation(org.springframework.web.bind.annotation.PostMapping.class);
            if (postMapping != null) {
                String[] values = postMapping.value();
                String[] paths = postMapping.path();
                if (containsKeyword(values, keyword) || containsKeyword(paths, keyword)) {
                    return true;
                }
            }

            // Also check @RequestMapping with method = POST
            org.springframework.web.bind.annotation.RequestMapping requestMapping =
                    method.getAnnotation(org.springframework.web.bind.annotation.RequestMapping.class);
            if (requestMapping != null) {
                boolean isPost = Arrays.stream(requestMapping.method())
                        .anyMatch(m -> m == org.springframework.web.bind.annotation.RequestMethod.POST);
                if (isPost) {
                    String[] values = requestMapping.value();
                    String[] paths = requestMapping.path();
                    if (containsKeyword(values, keyword) || containsKeyword(paths, keyword)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean containsKeyword(String[] arr, String keyword) {
        if (arr == null) return false;
        return Arrays.stream(arr).anyMatch(s -> s.contains(keyword));
    }
}
