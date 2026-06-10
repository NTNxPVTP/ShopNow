package com.example.shopnow.security;

import com.example.shopnow.user.models.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exploration test for BUG-23 (race revokeAllUserTokens).
 *
 * <p><b>Validates: Requirements 2.23</b> (Property 23 in design.md).
 *
 * <p>Phase 1 bug-condition exploration — MUST FAIL on unfixed code = SUCCESS.
 *
 * <p><b>Bug condition C(23):</b> {@code TokenService.revokeAllUserTokens}
 * uses a read-then-write pattern (read valid tokens, set revoked/expired,
 * saveAll) without {@code @Transactional}. When multiple threads call this
 * method concurrently for the same user, each thread reads a snapshot of
 * valid tokens independently. Without transactional isolation, concurrent
 * saveAll calls can overwrite each other (lost update), leaving some tokens
 * with {@code revoked=false}.
 *
 * <p><b>Property P(23):</b> {@code revokeAllUserTokens} SHALL be annotated
 * {@code @Transactional} so that the read-then-write pattern executes
 * atomically. After all concurrent calls complete, every previously-valid
 * token SHALL have {@code revoked=true} and {@code expired=true}.
 *
 * <p><b>Test approach:</b> Reflection-based check asserting that
 * {@code TokenService.revokeAllUserTokens(User)} is annotated with
 * {@code @Transactional} (Spring or Jakarta). This annotation is the
 * mechanism that prevents lost updates in the read-then-write pattern
 * by ensuring each call runs within a transaction boundary.
 *
 * <p><b>Expected on unfixed code:</b> FAIL — annotation is null, confirming
 * the bug exists (no transactional boundary to prevent lost updates under
 * concurrent access).
 */
class TokenServiceBug23ExplorationTest {

    /**
     * Core assertion for BUG-23: the {@code revokeAllUserTokens(User)} method
     * MUST have {@code @Transactional} annotation to prevent lost updates
     * when multiple threads concurrently revoke tokens for the same user.
     *
     * <p>On unfixed code this FAILS because the method has no @Transactional.
     */
    @Test
    @DisplayName("BUG-23: revokeAllUserTokens(User) SHALL be annotated @Transactional to prevent lost updates")
    void revokeAllUserTokens_shallBeAnnotatedTransactional_toPreventLostUpdates() throws NoSuchMethodException {
        Method revokeMethod = TokenService.class.getMethod(
                "revokeAllUserTokens", User.class);

        Annotation springTx = revokeMethod.getAnnotation(
                org.springframework.transaction.annotation.Transactional.class);
        Annotation jakartaTx = revokeMethod.getAnnotation(
                jakarta.transaction.Transactional.class);

        assertThat(springTx != null || jakartaTx != null)
                .as("BUG-23 (Requirements 2.23): TokenService.revokeAllUserTokens(User) SHALL be "
                        + "annotated @Transactional (Spring or Jakarta). Without it, the "
                        + "read-then-write pattern (findAllValidTokenByUser → forEach set "
                        + "revoked/expired → saveAll) is not atomic. Concurrent calls read "
                        + "overlapping snapshots and saveAll overwrites each other, causing "
                        + "lost updates where some tokens remain revoked=false. "
                        + "Found springTx=%s, jakartaTx=%s", springTx, jakartaTx)
                .isTrue();
    }
}
