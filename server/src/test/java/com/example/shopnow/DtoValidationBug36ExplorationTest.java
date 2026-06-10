package com.example.shopnow;

import com.example.shopnow.order.rest.dto.OrderItemRequest;
import com.example.shopnow.product.rest.dto.CreateProductRequest;
import com.example.shopnow.security.rest.dto.AuthenticationRequest;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Exploration test for BUG-36: DTO thiếu Jakarta Validation annotations.
 *
 * <p><b>Property 30: Bug Condition</b> — DTO Jakarta Validation.
 * Checks that DTO classes have Jakarta Validation annotations:
 * <ul>
 *   <li>CreateProductRequest: @Positive for quantity/price, @NotBlank for name</li>
 *   <li>OrderItemRequest: @Min(1) for quantity</li>
 *   <li>AuthenticationRequest: @Email for email, @NotBlank for password</li>
 * </ul>
 *
 * <p><b>CRITICAL</b>: This test MUST FAIL on unfixed code.
 * On unfixed code, CreateProductRequest lacks @Positive and @NotBlank,
 * and AuthenticationRequest lacks @Email and @NotBlank.
 *
 * <p><b>Validates: Requirements 2.36</b>
 */
@DisplayName("BUG-36 Exploration: DTO Jakarta Validation annotations")
class DtoValidationBug36ExplorationTest {

    // ─── CreateProductRequest ───────────────────────────────────────────────

    @Test
    @DisplayName("CreateProductRequest.name SHALL have @NotBlank")
    void createProductRequest_name_shallHaveNotBlank() {
        assertRecordComponentHasAnnotation(
                CreateProductRequest.class, "name", NotBlank.class,
                "CreateProductRequest.name must have @NotBlank annotation");
    }

    @Test
    @DisplayName("CreateProductRequest.quantity SHALL have @Positive")
    void createProductRequest_quantity_shallHavePositive() {
        assertRecordComponentHasAnnotation(
                CreateProductRequest.class, "quantity", Positive.class,
                "CreateProductRequest.quantity must have @Positive annotation");
    }

    @Test
    @DisplayName("CreateProductRequest.price SHALL have @Positive")
    void createProductRequest_price_shallHavePositive() {
        assertRecordComponentHasAnnotation(
                CreateProductRequest.class, "price", Positive.class,
                "CreateProductRequest.price must have @Positive annotation");
    }

    // ─── OrderItemRequest ───────────────────────────────────────────────────

    @Test
    @DisplayName("OrderItemRequest.quantity SHALL have @Min(1)")
    void orderItemRequest_quantity_shallHaveMin1() {
        // Check record component level
        RecordComponent component = findRecordComponent(OrderItemRequest.class, "quantity");
        Min onComponent = component.getAnnotation(Min.class);
        // Check field level (where Jakarta annotations typically land)
        Min onField = null;
        try {
            onField = OrderItemRequest.class.getDeclaredField("quantity").getAnnotation(Min.class);
        } catch (NoSuchFieldException e) {
            fail("OrderItemRequest does not have field 'quantity'");
        }
        boolean hasMin1 = (onComponent != null && onComponent.value() == 1)
                || (onField != null && onField.value() == 1);
        assertTrue(hasMin1, "OrderItemRequest.quantity must have @Min(1) annotation");
    }

    // ─── AuthenticationRequest ──────────────────────────────────────────────

    @Test
    @DisplayName("AuthenticationRequest.email SHALL have @Email")
    void authenticationRequest_email_shallHaveEmail() {
        assertRecordComponentHasAnnotation(
                AuthenticationRequest.class, "email", Email.class,
                "AuthenticationRequest.email must have @Email annotation");
    }

    @Test
    @DisplayName("AuthenticationRequest.password SHALL have @NotBlank")
    void authenticationRequest_password_shallHaveNotBlank() {
        assertRecordComponentHasAnnotation(
                AuthenticationRequest.class, "password", NotBlank.class,
                "AuthenticationRequest.password must have @NotBlank annotation");
    }

    // ─── Helpers ────────────────────────────────────────────────────────────

    /**
     * Checks that the given annotation is present on the record component OR
     * on the underlying declared field. Jakarta Validation annotations typically
     * have @Target that includes FIELD but not RECORD_COMPONENT, so the compiler
     * propagates them to the field.
     */
    private <A extends Annotation> void assertRecordComponentHasAnnotation(
            Class<?> recordClass, String componentName, Class<A> annotationType, String message) {
        RecordComponent component = findRecordComponent(recordClass, componentName);
        // Check record component level
        A onComponent = component.getAnnotation(annotationType);
        // Check field level (where Jakarta annotations typically land)
        A onField = null;
        try {
            onField = recordClass.getDeclaredField(componentName).getAnnotation(annotationType);
        } catch (NoSuchFieldException e) {
            // field must exist for a record component
            fail(recordClass.getSimpleName() + " does not have field '" + componentName + "'");
        }
        assertTrue(onComponent != null || onField != null, message);
    }

    private RecordComponent findRecordComponent(Class<?> recordClass, String componentName) {
        assertTrue(recordClass.isRecord(),
                recordClass.getSimpleName() + " must be a record class");
        Optional<RecordComponent> found = Arrays.stream(recordClass.getRecordComponents())
                .filter(rc -> rc.getName().equals(componentName))
                .findFirst();
        if (found.isEmpty()) {
            fail(recordClass.getSimpleName() + " does not have record component '" + componentName + "'");
        }
        return found.get();
    }
}
