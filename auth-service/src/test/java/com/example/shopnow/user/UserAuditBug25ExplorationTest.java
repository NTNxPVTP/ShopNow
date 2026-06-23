package com.example.shopnow.user;

import com.example.shopnow.user.models.User;
import jakarta.persistence.EntityListeners;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.lang.reflect.Field;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exploration test for BUG-25 (User audit fields).
 *
 * <p><b>Property 25: Bug Condition</b> — User auditing fields.
 *
 * <p>The User entity MUST have JPA auditing configured so that
 * {@code createdAt} and {@code updatedAt} are automatically populated
 * on insert/update. This can be achieved via:
 * <ul>
 *   <li>{@code @EntityListeners(AuditingEntityListener.class)} on User or its superclass</li>
 *   <li>Fields annotated with {@code @CreatedDate} / {@code @LastModifiedDate}</li>
 * </ul>
 *
 * <p><b>CRITICAL:</b> This test MUST FAIL on unfixed code because the User
 * entity (and its BaseEntity superclass) lacks auditing annotations entirely.
 * After the fix (adding auditing to User or BaseEntity), this test turns green.
 *
 * <p><b>Validates: Requirements 2.25</b>
 */
@DisplayName("BUG-25 Exploration: User entity SHALL have JPA auditing configured")
class UserAuditBug25ExplorationTest {

    /**
     * Verifies that the User class (or its superclass hierarchy) has
     * {@code @EntityListeners} containing {@code AuditingEntityListener.class}.
     *
     * <p>On unfixed code → FAIL: neither User nor BaseEntity has this annotation.
     */
    @Test
    @DisplayName("User class hierarchy SHALL have @EntityListeners(AuditingEntityListener.class)")
    void userShallHaveAuditingEntityListener() {
        boolean hasAuditingListener = false;

        // Walk the class hierarchy (User → BaseEntity → Object)
        Class<?> clazz = User.class;
        while (clazz != null && clazz != Object.class) {
            EntityListeners annotation = clazz.getAnnotation(EntityListeners.class);
            if (annotation != null) {
                boolean containsAuditing = Arrays.asList(annotation.value())
                        .contains(AuditingEntityListener.class);
                if (containsAuditing) {
                    hasAuditingListener = true;
                    break;
                }
            }
            clazz = clazz.getSuperclass();
        }

        assertThat(hasAuditingListener)
                .as("User class hierarchy SHALL have @EntityListeners containing "
                        + "AuditingEntityListener.class; on unfixed code neither User "
                        + "nor BaseEntity has this annotation — counterexample: "
                        + "createdAt/updatedAt will be null on persist")
                .isTrue();
    }

    /**
     * Verifies that the User class hierarchy declares a field annotated
     * with {@code @CreatedDate}.
     *
     * <p>On unfixed code → FAIL: no such field exists in User or BaseEntity.
     */
    @Test
    @DisplayName("User class hierarchy SHALL have a @CreatedDate field")
    void userShallHaveCreatedDateField() {
        boolean hasCreatedDate = fieldWithAnnotationExists(User.class, CreatedDate.class);

        assertThat(hasCreatedDate)
                .as("User class hierarchy SHALL declare a field annotated with "
                        + "@CreatedDate; on unfixed code no such field exists — "
                        + "counterexample: users.created_at will be null")
                .isTrue();
    }

    /**
     * Verifies that the User class hierarchy declares a field annotated
     * with {@code @LastModifiedDate}.
     *
     * <p>On unfixed code → FAIL: no such field exists in User or BaseEntity.
     */
    @Test
    @DisplayName("User class hierarchy SHALL have a @LastModifiedDate field")
    void userShallHaveLastModifiedDateField() {
        boolean hasLastModifiedDate = fieldWithAnnotationExists(User.class, LastModifiedDate.class);

        assertThat(hasLastModifiedDate)
                .as("User class hierarchy SHALL declare a field annotated with "
                        + "@LastModifiedDate; on unfixed code no such field exists — "
                        + "counterexample: users.updated_at will be null")
                .isTrue();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Walks the class hierarchy and checks if any declared field carries
     * the given annotation.
     */
    private boolean fieldWithAnnotationExists(Class<?> startClass,
                                              Class<? extends java.lang.annotation.Annotation> annotation) {
        Class<?> clazz = startClass;
        while (clazz != null && clazz != Object.class) {
            for (Field field : clazz.getDeclaredFields()) {
                if (field.isAnnotationPresent(annotation)) {
                    return true;
                }
            }
            clazz = clazz.getSuperclass();
        }
        return false;
    }
}
