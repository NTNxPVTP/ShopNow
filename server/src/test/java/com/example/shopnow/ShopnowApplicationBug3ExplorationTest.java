package com.example.shopnow;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.core.env.Environment;
import org.springframework.mock.env.MockEnvironment;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exploration test for BUG-3 (env dump on startup leaks secrets).
 *
 * <p><b>Validates: Requirements 2.3</b> (Property 3 in design.md).
 *
 * <p>Phase 1 bug-condition exploration test for the bugfix workflow
 * {@code shopnow-codebase-bugfixes}. It is EXPECTED TO FAIL on unfixed code;
 * the failure surfaces a counterexample that proves the bug exists. After
 * the fix in {@link ShopnowApplication} (removal of the {@code debugEnv}
 * {@link CommandLineRunner}), this test must turn green.
 *
 * <p><b>Bug condition C(3):</b> {@code ShopnowApplication} declares a
 * {@code @Bean CommandLineRunner debugEnv} that, on application startup,
 * prints the injected {@link Environment#toString()} to {@code System.out}.
 * Whenever an environment variable named {@code JWT_SECRET_KEY},
 * {@code DB_PASSWORD}, {@code GG_OAUTH2ID_SECRET}, or
 * {@code GIT_OAUTH2ID_SECRET} is exposed in any property source visible to
 * that environment, its value (or at minimum its key) is dumped to stdout
 * and persisted in container/CI logs.
 *
 * <p><b>Property P(3):</b> for every application startup, stdout SHALL NOT
 * contain any of the sensitive env-var names listed above, nor the canonical
 * {@code Environment.toString()} signature (e.g. {@code StandardEnvironment {}).
 *
 * <p><b>Test design:</b> the production runner only prints whatever
 * {@code env.toString()} produces. Rather than booting a full Spring context
 * (which would require live DB and OAuth2 credentials), this test isolates
 * the bean: it constructs a {@link ShopnowApplication}, injects a stub
 * {@link Environment} whose {@link Object#toString() toString()} embeds the
 * four sensitive keys, locates the {@code debugEnv} bean factory method via
 * reflection, invokes it, and runs the returned {@link CommandLineRunner}.
 * Captured stdout is then asserted free of every sensitive marker.
 *
 * <p>If the bean (or its backing {@code env} field) has been removed as part
 * of the BUG-3 fix, the reflective lookup short-circuits with no dump
 * possible — exactly the post-fix behaviour the property requires.
 *
 * <p><b>Expected counterexample on unfixed code:</b> stub
 * {@code env.toString()} returns
 * {@code "StandardEnvironment {... JWT_SECRET_KEY=... DB_PASSWORD=... ...}"},
 * the unfixed {@code debugEnv} runner prints that string verbatim via
 * {@code System.out.println(env.toString())}, and the captured output
 * contains every banned marker — assertion fails.
 */
@ExtendWith(OutputCaptureExtension.class)
class ShopnowApplicationBug3ExplorationTest {

    @Test
    @DisplayName("debugEnv runner SHALL NOT dump sensitive env vars to stdout on startup")
    void debugEnv_shall_not_dump_sensitive_env_vars(CapturedOutput output) throws Exception {
        // --- Arrange --------------------------------------------------------
        // Stub Environment whose toString() embeds the four sensitive marker
        // keys called out by Requirements 2.3, plus the canonical
        // StandardEnvironment signature. Any code path that prints
        // env.toString() will visibly leak these markers.
        Environment leakyEnv = new MockEnvironment() {
            @Override
            public String toString() {
                return "StandardEnvironment {"
                        + "activeProfiles=[], "
                        + "defaultProfiles=[default], "
                        + "propertySources=[systemEnvironment {"
                        + "JWT_SECRET_KEY=super-secret-jwt-bug3-marker, "
                        + "DB_PASSWORD=super-secret-db-bug3-marker, "
                        + "GG_OAUTH2ID_SECRET=google-oauth2-secret-bug3-marker, "
                        + "GIT_OAUTH2ID_SECRET=github-oauth2-secret-bug3-marker"
                        + "}]}";
            }
        };

        // --- Act ------------------------------------------------------------
        // Invoke the debugEnv bean factory directly (no full Spring boot)
        // with the leaky Environment injected. If the bean is gone post-fix
        // the helper short-circuits and nothing is written to stdout.
        invokeDebugEnvIfPresent(leakyEnv);

        // --- Assert ---------------------------------------------------------
        // On unfixed code the runner echoes env.toString() verbatim, so each
        // of these checks fails with a precise counterexample. On fixed code
        // (bean removed) captured output is empty for our markers, so all
        // assertions hold.
        String captured = output.getAll();
        assertThat(captured)
                .as("stdout SHALL NOT contain JWT_SECRET_KEY marker")
                .doesNotContain("JWT_SECRET_KEY");
        assertThat(captured)
                .as("stdout SHALL NOT contain DB_PASSWORD marker")
                .doesNotContain("DB_PASSWORD");
        assertThat(captured)
                .as("stdout SHALL NOT contain GG_OAUTH2ID_SECRET marker")
                .doesNotContain("GG_OAUTH2ID_SECRET");
        assertThat(captured)
                .as("stdout SHALL NOT contain GIT_OAUTH2ID_SECRET marker")
                .doesNotContain("GIT_OAUTH2ID_SECRET");
        assertThat(captured)
                .as("stdout SHALL NOT contain the Environment.toString() signature")
                .doesNotContain("StandardEnvironment {");
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /**
     * Reflectively invoke {@code ShopnowApplication.debugEnv()} after
     * injecting the supplied {@link Environment} into the {@code env} field.
     * Both the field and the bean factory method are looked up reflectively
     * so the exploration test stays valid after the BUG-3 fix removes them.
     */
    private static void invokeDebugEnvIfPresent(Environment env) throws Exception {
        ShopnowApplication app = new ShopnowApplication();

        Field envField;
        try {
            envField = ShopnowApplication.class.getDeclaredField("env");
        } catch (NoSuchFieldException e) {
            // Post-fix: env field removed → nothing to dump.
            return;
        }
        envField.setAccessible(true);
        envField.set(app, env);

        Method debugEnvMethod;
        try {
            debugEnvMethod = ShopnowApplication.class.getMethod("debugEnv");
        } catch (NoSuchMethodException e) {
            // Post-fix: bean factory removed → nothing to dump.
            return;
        }

        Object result = debugEnvMethod.invoke(app);
        if (result instanceof CommandLineRunner runner) {
            runner.run();
        }
    }
}
