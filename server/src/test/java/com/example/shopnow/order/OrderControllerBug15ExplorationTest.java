package com.example.shopnow.order;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exploration test for BUG-15 (thiếu endpoint update order status).
 *
 * <p><b>Validates: Requirements 2.15</b> (Property 15 in design.md).
 *
 * <p>Phase&nbsp;1 bug-condition exploration test for the bugfix workflow
 * {@code shopnow-codebase-bugfixes}. It is EXPECTED TO FAIL on unfixed
 * code; the failure surfaces a counterexample that proves the bug
 * exists (no PATCH endpoints for order state transitions).
 *
 * <p><b>Bug condition C(15):</b> seller needs to transition
 * {@code IN_PROCESS → DELIVERING → PAID}, or customer needs to cancel
 * an order, or seller needs to reject — but no endpoint exists.
 *
 * <p><b>Property P(15):</b> for every valid state transition, there
 * SHALL exist an endpoint that accepts the request and returns
 * status != 404. Specifically:
 * <ul>
 *   <li>{@code PATCH /api/subOrders/{id}/status} — seller updates sub-order status</li>
 *   <li>{@code PATCH /api/orders/{id}/cancel} — customer cancels order</li>
 *   <li>{@code PATCH /api/subOrders/{id}/reject} — seller rejects sub-order</li>
 * </ul>
 *
 * <p><b>Test approach:</b> Static source scan + reflection. We verify
 * that the controller classes contain handler methods annotated with
 * {@code @PatchMapping} for the required paths. No Spring context is
 * needed — this is a pure compile-time / reflection check.
 *
 * <p><b>Expected counterexample on unfixed code:</b>
 * {@code OrderController} has no method with {@code @PatchMapping("/{id}/cancel")};
 * {@code SubOrderController} has no method with {@code @PatchMapping("/{id}/status")}
 * or {@code @PatchMapping("/{id}/reject")}. All three assertions fail → 404 at runtime.
 */
class OrderControllerBug15ExplorationTest {

    /**
     * Asserts that {@code SubOrderController} has a handler method
     * annotated with {@code @PatchMapping} whose value/path contains
     * {@code "/{id}/status"} — the endpoint for seller to update
     * sub-order status (e.g. IN_PROCESS → DELIVERING).
     */
    @Test
    @DisplayName("SubOrderController must have PATCH /{id}/status endpoint for status transitions")
    void subOrderController_hasPatchStatusEndpoint() throws ClassNotFoundException {
        Class<?> controllerClass = Class.forName(
                "com.example.shopnow.order.rest.SubOrderController");

        boolean hasPatchStatus = hasPatchMappingContaining(controllerClass, "status");

        assertThat(hasPatchStatus)
                .as("SubOrderController should have a @PatchMapping handler "
                        + "with path containing 'status' for PATCH /api/subOrders/{id}/status. "
                        + "Counterexample: no such endpoint exists → runtime 404.")
                .isTrue();
    }

    /**
     * Asserts that {@code OrderController} has a handler method
     * annotated with {@code @PatchMapping} whose value/path contains
     * {@code "cancel"} — the endpoint for customer to cancel an order.
     */
    @Test
    @DisplayName("OrderController must have PATCH /{id}/cancel endpoint for customer cancellation")
    void orderController_hasPatchCancelEndpoint() throws ClassNotFoundException {
        Class<?> controllerClass = Class.forName(
                "com.example.shopnow.order.rest.OrderController");

        boolean hasPatchCancel = hasPatchMappingContaining(controllerClass, "cancel");

        assertThat(hasPatchCancel)
                .as("OrderController should have a @PatchMapping handler "
                        + "with path containing 'cancel' for PATCH /api/orders/{id}/cancel. "
                        + "Counterexample: no such endpoint exists → runtime 404.")
                .isTrue();
    }

    /**
     * Asserts that {@code SubOrderController} has a handler method
     * annotated with {@code @PatchMapping} whose value/path contains
     * {@code "reject"} — the endpoint for seller to reject a sub-order.
     */
    @Test
    @DisplayName("SubOrderController must have PATCH /{id}/reject endpoint for seller rejection")
    void subOrderController_hasPatchRejectEndpoint() throws ClassNotFoundException {
        Class<?> controllerClass = Class.forName(
                "com.example.shopnow.order.rest.SubOrderController");

        boolean hasPatchReject = hasPatchMappingContaining(controllerClass, "reject");

        assertThat(hasPatchReject)
                .as("SubOrderController should have a @PatchMapping handler "
                        + "with path containing 'reject' for PATCH /api/subOrders/{id}/reject. "
                        + "Counterexample: no such endpoint exists → runtime 404.")
                .isTrue();
    }

    // -----------------------------------------------------------------------
    // Helper
    // -----------------------------------------------------------------------

    /**
     * Checks whether any method in the given class is annotated with
     * {@code @PatchMapping} and the annotation's value/path array
     * contains a string matching the given keyword.
     */
    private boolean hasPatchMappingContaining(Class<?> clazz, String keyword) {
        for (Method method : clazz.getDeclaredMethods()) {
            // Check for @PatchMapping
            org.springframework.web.bind.annotation.PatchMapping patchMapping =
                    method.getAnnotation(org.springframework.web.bind.annotation.PatchMapping.class);
            if (patchMapping != null) {
                // Check value() and path() arrays
                String[] values = patchMapping.value();
                String[] paths = patchMapping.path();
                if (containsKeyword(values, keyword) || containsKeyword(paths, keyword)) {
                    return true;
                }
            }

            // Also check @RequestMapping with method = PATCH
            org.springframework.web.bind.annotation.RequestMapping requestMapping =
                    method.getAnnotation(org.springframework.web.bind.annotation.RequestMapping.class);
            if (requestMapping != null) {
                boolean isPatch = Arrays.stream(requestMapping.method())
                        .anyMatch(m -> m == org.springframework.web.bind.annotation.RequestMethod.PATCH);
                if (isPatch) {
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
