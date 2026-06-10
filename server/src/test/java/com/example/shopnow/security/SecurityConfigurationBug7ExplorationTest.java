package com.example.shopnow.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exploration test for BUG-7 (public endpoints + CORS + custom handlers
 * missing from {@link SecurityConfiguration}).
 *
 * <p><b>Validates: Requirements 2.7</b> (Property 7 in design.md).
 *
 * <p>Phase 1 bug-condition exploration test for the bugfix workflow
 * {@code shopnow-codebase-bugfixes}. It is EXPECTED TO FAIL on unfixed
 * code; the failure surfaces concrete counterexamples that prove the bug
 * exists. After the fix in {@link SecurityConfiguration} (add
 * {@code permitAll()} for the public read endpoints, configure CORS, and
 * register a custom {@code AuthenticationEntryPoint} /
 * {@code AccessDeniedHandler}), this test must turn green.
 *
 * <p><b>Bug condition C(7):</b>
 * {@code SecurityConfiguration.securityFilterChain} authorises ONLY
 * {@code /v3/api-docs/**}, {@code /swagger-ui/**}, {@code /swagger-ui.html}
 * and {@code /authenticate} via {@code permitAll()} and falls through to
 * {@code anyRequest().authenticated()} for everything else, while NOT
 * configuring CORS and NOT registering an
 * {@code AuthenticationEntryPoint} or {@code AccessDeniedHandler}. As a
 * result anonymous {@code GET /api/products}, {@code GET /api/products/{id}},
 * {@code GET /api/categories}, {@code GET /h2-console/} and
 * {@code GET /error} return 401, browser CORS preflights from
 * {@code http://localhost:3000} are rejected, and 401/403 responses come
 * back as empty bodies instead of {@code application/problem+json}.
 *
 * <p><b>Property P(7):</b> for any startup of the application,
 * {@code SecurityConfiguration} SHALL declare ALL of the following:
 * <ol>
 *   <li>{@code permitAll()} for the request matchers
 *       {@code /api/products}, {@code /api/products/{id}} (or
 *       {@code /api/products/**}), {@code /api/categories} (or
 *       {@code /api/categories/**}), {@code /h2-console/**} and
 *       {@code /error};</li>
 *   <li>An active CORS configuration on the {@link
 *       org.springframework.security.config.annotation.web.builders.HttpSecurity}
 *       chain (typically a {@code .cors(...)} call backed by a
 *       {@code CorsConfigurationSource} bean);</li>
 *   <li>A custom {@code AuthenticationEntryPoint} AND a custom
 *       {@code AccessDeniedHandler} wired into {@code .exceptionHandling(...)}
 *       so that 401/403 responses carry an
 *       {@code application/problem+json} body.</li>
 * </ol>
 *
 * <p><b>Test design:</b> a static source scan, deliberately decoupled from
 * the Spring context. Booting a full {@code @SpringBootTest} would require
 * Postgres + JWT secrets + Google/GitHub OAuth2 credentials at test time
 * (the production {@code application.properties} reads {@code DB_URL},
 * {@code JWT_SECRET_KEY}, {@code GG_OAUTH2ID_CLIENT}, etc. via
 * {@code ${...}} placeholders), so the test reads the on-disk source file
 * directly. This is consistent with the sibling
 * {@link SecurityConfigurationBug6ExplorationTest} which also opts for a
 * context-free assertion. All three sub-properties are checked in a single
 * test so multiple counterexamples surface in one run.
 *
 * <p><b>Expected counterexample on unfixed code:</b>
 * {@code SecurityConfiguration.java} only registers {@code permitAll()}
 * for the four matchers listed above (so {@code /api/products},
 * {@code /api/products/{id}}, {@code /api/categories},
 * {@code /h2-console/**} and {@code /error} are missing), never calls
 * {@code .cors(}, and never references
 * {@code AuthenticationEntryPoint} / {@code AccessDeniedHandler} /
 * {@code .exceptionHandling(}. The single {@code @Test} below fails with
 * every missing piece concatenated into the message.
 */
class SecurityConfigurationBug7ExplorationTest {

    /**
     * Project-relative path to the production {@code SecurityConfiguration}
     * source. Maven runs tests from the project root (the directory
     * containing {@code pom.xml}), so a relative path resolves correctly
     * under both Maven and most IDE runners.
     */
    private static final Path SECURITY_CONFIG_SOURCE = Paths.get(
            "src", "main", "java", "com", "example", "shopnow",
            "security", "SecurityConfiguration.java");

    /**
     * Regex fragments for each matcher that MUST be {@code permitAll()}-ed
     * per Property 7. The patterns deliberately accept either the exact
     * spelling from the requirements or a slightly broader equivalent
     * (e.g. {@code /api/products/**} as a superset of
     * {@code /api/products} + {@code /api/products/{id}}) so the fix
     * author has flexibility while still satisfying the contract.
     *
     * <p>Each entry is {@code [human-readable label, regex pattern]}.
     */
    private static final String[][] REQUIRED_PUBLIC_MATCHERS = new String[][] {
            {"/api/products",        "/api/products(?:/\\*\\*|\")"},
            {"/api/products/{id}",   "/api/products/(?:\\{id\\}|\\*\\*)"},
            {"/api/categories",      "/api/categories(?:/\\*\\*|\")"},
            {"/h2-console/**",       "/h2-console/\\*\\*"},
            {"/error",               "/error\""}
    };

    /**
     * Detects an active (un-commented) {@code .cors(} method call on the
     * {@code HttpSecurity} chain. The leading negative-lookahead skips
     * lines whose first non-whitespace characters are {@code //}.
     */
    private static final Pattern CORS_ACTIVE = Pattern.compile(
            "^(?!\\s*//).*\\.cors\\s*\\(");

    /**
     * Detects an active (un-commented) {@code .exceptionHandling(} method
     * call. Combined with the entry-point / access-denied-handler symbol
     * checks below, this proves the application installs a custom 401/403
     * response writer.
     */
    private static final Pattern EXCEPTION_HANDLING_ACTIVE = Pattern.compile(
            "^(?!\\s*//).*\\.exceptionHandling\\s*\\(");

    @Test
    @DisplayName("SecurityConfiguration SHALL permitAll public endpoints, configure CORS, and register custom 401/403 handlers")
    void public_endpoints_cors_and_custom_handlers_shall_be_configured() throws Exception {
        assertThat(SECURITY_CONFIG_SOURCE)
                .as("Production SecurityConfiguration source must exist for the static scan")
                .exists();

        String source = Files.readString(SECURITY_CONFIG_SOURCE);

        // Strip single-line // comments so a commented-out matcher /
        // .cors( / handler reference cannot satisfy the property. Block
        // comments are intentionally NOT stripped: they are not used for
        // disabled code in this file and removing them would mangle
        // multi-line Javadoc that happens to mention these tokens.
        String activeSource = stripLineComments(source);

        // --- Sub-property 1: permitAll() for the public read endpoints ---
        List<String> missingPublicMatchers = new ArrayList<>();
        for (String[] entry : REQUIRED_PUBLIC_MATCHERS) {
            String label = entry[0];
            Pattern pattern = Pattern.compile(entry[1]);
            if (!pattern.matcher(activeSource).find()) {
                missingPublicMatchers.add(label);
            }
        }
        // permitAll() must actually be invoked at least once on the
        // authorize-requests builder; without it the matcher list above
        // would still leave the endpoints behind anyRequest().authenticated().
        boolean permitAllInvoked = Pattern.compile("\\.permitAll\\s*\\(")
                .matcher(activeSource).find();

        // --- Sub-property 2: CORS configured on the HttpSecurity chain ---
        boolean corsConfigured = false;
        for (String rawLine : activeSource.split("\\R", -1)) {
            if (CORS_ACTIVE.matcher(rawLine).find()) {
                corsConfigured = true;
                break;
            }
        }

        // --- Sub-property 3: custom AuthenticationEntryPoint + AccessDeniedHandler ---
        boolean exceptionHandlingInvoked = false;
        for (String rawLine : activeSource.split("\\R", -1)) {
            if (EXCEPTION_HANDLING_ACTIVE.matcher(rawLine).find()) {
                exceptionHandlingInvoked = true;
                break;
            }
        }
        boolean authenticationEntryPointReferenced =
                activeSource.contains("AuthenticationEntryPoint");
        boolean accessDeniedHandlerReferenced =
                activeSource.contains("AccessDeniedHandler");

        boolean customHandlersWired = exceptionHandlingInvoked
                && authenticationEntryPointReferenced
                && accessDeniedHandlerReferenced;

        boolean publicMatchersOk =
                missingPublicMatchers.isEmpty() && permitAllInvoked;

        boolean propertyHolds =
                publicMatchersOk && corsConfigured && customHandlersWired;

        // On unfixed code the assertion message embeds an itemised
        // counterexample so the maintainer can see exactly which
        // sub-property failed.
        String counterexample = String.format(
                "%n  permitAllInvoked                  = %s"
                        + "%n  missingPublicMatchers             = %s"
                        + "%n  corsConfigured                    = %s"
                        + "%n  exceptionHandlingInvoked          = %s"
                        + "%n  authenticationEntryPointReferenced= %s"
                        + "%n  accessDeniedHandlerReferenced     = %s",
                permitAllInvoked,
                missingPublicMatchers.isEmpty() ? "<none>" : missingPublicMatchers,
                corsConfigured,
                exceptionHandlingInvoked,
                authenticationEntryPointReferenced,
                accessDeniedHandlerReferenced);

        assertThat(propertyHolds)
                .as("BUG-7: SecurityConfiguration SHALL permitAll the public read "
                        + "endpoints (/api/products, /api/products/{id}, /api/categories, "
                        + "/h2-console/**, /error), enable CORS for the SPA origin, AND "
                        + "register custom AuthenticationEntryPoint + AccessDeniedHandler "
                        + "via .exceptionHandling(...) so 401/403 responses carry a "
                        + "ProblemDetail body.%s",
                        counterexample)
                .isTrue();
    }

    /**
     * Remove any text that follows {@code //} on each line so that
     * commented-out matchers, {@code .cors(} or handler references do
     * NOT satisfy the property checks above. Block comments are left
     * intact: this configuration file uses them only for class-level
     * Javadoc, not to disable code.
     */
    private static String stripLineComments(String source) {
        StringBuilder out = new StringBuilder(source.length());
        for (String rawLine : source.split("\\R", -1)) {
            int slashSlash = indexOfLineComment(rawLine);
            if (slashSlash >= 0) {
                out.append(rawLine, 0, slashSlash);
            } else {
                out.append(rawLine);
            }
            out.append('\n');
        }
        return out.toString();
    }

    /**
     * Find the start of a {@code //} line comment, ignoring occurrences
     * inside string literals. Returns {@code -1} if the line has no
     * line comment. Block comments and character literals are not
     * supported because this configuration file does not use them in
     * positions that could collide with the tokens we scan for.
     */
    private static int indexOfLineComment(String line) {
        boolean inString = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '\\' && i + 1 < line.length()) {
                // skip escaped char inside a string literal
                i++;
                continue;
            }
            if (c == '"') {
                inString = !inString;
                continue;
            }
            if (!inString && c == '/' && i + 1 < line.length() && line.charAt(i + 1) == '/') {
                return i;
            }
        }
        return -1;
    }
}
