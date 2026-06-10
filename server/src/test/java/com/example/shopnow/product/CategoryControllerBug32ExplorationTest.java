package com.example.shopnow.product;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exploration test for BUG-32 (thiếu PATCH/DELETE category).
 *
 * <p><b>Validates: Requirements 2.32</b> (Property 28 in design.md).
 *
 * <p>Phase&nbsp;1 bug-condition exploration test for the bugfix workflow
 * {@code shopnow-codebase-bugfixes}. It is EXPECTED TO FAIL on unfixed
 * code; the failure surfaces a counterexample that proves the bug
 * exists (no PATCH/DELETE endpoints for categories).
 *
 * <p><b>Bug condition C(32):</b> requests {@code PATCH /api/categories/{id}}
 * and {@code DELETE /api/categories/{id}} do not exist in CategoryController.
 *
 * <p><b>Property P(28):</b> CategoryController SHALL have handler methods
 * annotated with {@code @PatchMapping} (for renaming a category) and
 * {@code @DeleteMapping} (for deleting a category). Status != 405 for valid input.
 *
 * <p><b>Test approach:</b> Reflection-based (no Spring context). Check
 * {@code CategoryController} class for handler methods with
 * {@code @PatchMapping} and {@code @DeleteMapping} annotations.
 *
 * <p><b>Expected counterexample on unfixed code:</b>
 * {@code CategoryController} only has {@code @PostMapping} and {@code @GetMapping};
 * no methods with {@code @PatchMapping} or {@code @DeleteMapping} exist
 * → both assertions fail → runtime 405 Method Not Allowed for these requests.
 */
class CategoryControllerBug32ExplorationTest {

    private static final String CONTROLLER_CLASS_NAME =
            "com.example.shopnow.product.rest.CategoryController";

    /**
     * Asserts that {@code CategoryController} has a handler method
     * annotated with {@code @PatchMapping} — the endpoint for updating
     * (renaming) a category: PATCH /api/categories/{id}.
     */
    @Test
    @DisplayName("BUG-32: CategoryController must have PATCH endpoint for updating category")
    void categoryController_hasPatchEndpoint() throws ClassNotFoundException {
        Class<?> controllerClass = Class.forName(CONTROLLER_CLASS_NAME);

        boolean hasPatch = hasMappingForMethod(controllerClass, RequestMethod.PATCH);

        assertThat(hasPatch)
                .as("CategoryController should have a @PatchMapping (or @RequestMapping(method=PATCH)) "
                        + "handler for PATCH /api/categories/{id}. "
                        + "Counterexample: no such endpoint exists → runtime 405 Method Not Allowed.")
                .isTrue();
    }

    /**
     * Asserts that {@code CategoryController} has a handler method
     * annotated with {@code @DeleteMapping} — the endpoint for deleting
     * a category: DELETE /api/categories/{id}.
     */
    @Test
    @DisplayName("BUG-32: CategoryController must have DELETE endpoint for deleting category")
    void categoryController_hasDeleteEndpoint() throws ClassNotFoundException {
        Class<?> controllerClass = Class.forName(CONTROLLER_CLASS_NAME);

        boolean hasDelete = hasMappingForMethod(controllerClass, RequestMethod.DELETE);

        assertThat(hasDelete)
                .as("CategoryController should have a @DeleteMapping (or @RequestMapping(method=DELETE)) "
                        + "handler for DELETE /api/categories/{id}. "
                        + "Counterexample: no such endpoint exists → runtime 405 Method Not Allowed.")
                .isTrue();
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /**
     * Checks whether any method in the given class is annotated with the
     * specific HTTP method mapping annotation (@PatchMapping, @DeleteMapping)
     * or with @RequestMapping specifying the corresponding HTTP method.
     */
    private boolean hasMappingForMethod(Class<?> clazz, RequestMethod httpMethod) {
        for (Method method : clazz.getDeclaredMethods()) {
            if (httpMethod == RequestMethod.PATCH) {
                PatchMapping patchMapping = method.getAnnotation(PatchMapping.class);
                if (patchMapping != null) {
                    return true;
                }
            }

            if (httpMethod == RequestMethod.DELETE) {
                DeleteMapping deleteMapping = method.getAnnotation(DeleteMapping.class);
                if (deleteMapping != null) {
                    return true;
                }
            }

            // Also check @RequestMapping with the corresponding method
            RequestMapping requestMapping = method.getAnnotation(RequestMapping.class);
            if (requestMapping != null) {
                boolean matchesMethod = Arrays.stream(requestMapping.method())
                        .anyMatch(m -> m == httpMethod);
                if (matchesMethod) {
                    return true;
                }
            }
        }
        return false;
    }
}
