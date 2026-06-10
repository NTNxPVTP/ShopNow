package com.example.shopnow.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exploration test for BUG-6 ({@code .oauth2Login(...)} commented out).
 *
 * <p><b>Validates: Requirements 2.6</b> (Property 6 in design.md).
 *
 * <p>Phase 1 bug-condition exploration test for the bugfix workflow
 * {@code shopnow-codebase-bugfixes}. It is EXPECTED TO FAIL on unfixed code;
 * the failure surfaces a concrete counterexample that proves the bug
 * exists. After the fix in
 * {@link SecurityConfiguration} (either re-enable {@code .oauth2Login(...)}
 * with the registered handler, OR fully remove
 * {@link OAuth2AuthenticationSuccessHandler} and the
 * {@code spring.security.oauth2.client.*} properties), this test must turn
 * green.
 *
 * <p><b>Bug condition C(6):</b>
 * {@code SecurityConfiguration} contains a {@code .oauth2Login(...)} call
 * but the line is commented out (preceded by {@code //} on the same line).
 * As a result, the OAuth2 login filter is never registered with the
 * {@code SecurityFilterChain} and {@code GET /oauth2/authorization/google}
 * (or {@code /github}) returns 401/403/404 instead of redirecting to the
 * provider, while {@link OAuth2AuthenticationSuccessHandler} stays
 * injected as dead code and the
 * {@code spring.security.oauth2.client.registration.*} properties remain
 * in {@code application.properties}, misleading new contributors into
 * thinking OAuth2 is wired.
 *
 * <p><b>Property P(6):</b> for any startup of the application, EITHER
 * <ol>
 *   <li><b>OAuth2 enabled branch</b> — {@code SecurityConfiguration}
 *       declares an active (un-commented) {@code .oauth2Login(...)}
 *       call AND keeps {@link OAuth2AuthenticationSuccessHandler} as a
 *       collaborator, so {@code GET /oauth2/authorization/{provider}}
 *       triggers a 302 redirect to the provider; OR</li>
 *   <li><b>OAuth2 removed branch</b> — {@link OAuth2AuthenticationSuccessHandler}
 *       is no longer referenced by {@code SecurityConfiguration} AND the
 *       {@code spring.security.oauth2.client.*} properties are absent
 *       from {@code application.properties}, signalling the feature was
 *       intentionally dropped.</li>
 * </ol>
 *
 * <p><b>Test design:</b> a static source scan, deliberately decoupled from
 * the Spring context. Booting a full {@code @SpringBootTest} would require
 * Postgres + Google/GitHub OAuth2 credentials at test time (the production
 * {@code application.properties} reads {@code DB_URL}, {@code JWT_SECRET_KEY},
 * {@code GG_OAUTH2ID_CLIENT}, etc. via {@code ${...}} placeholders), so the
 * test reads the on-disk source files directly. This is consistent with the
 * sibling {@code ShopnowApplicationBug3ExplorationTest} which also opts for
 * a context-free assertion.
 *
 * <p>The scan implements the EITHER/OR property as described in the bugfix
 * tasks file: it accepts the OAuth2-enabled branch, accepts the
 * OAuth2-removed branch, and rejects the in-between state where
 * {@code .oauth2Login(...)} exists but is commented out.
 *
 * <p><b>Expected counterexample on unfixed code:</b>
 * {@code SecurityConfiguration.java} contains the line
 * <pre>
 *   //        .oauth2Login(oauth2 -&gt; oauth2
 *   //                .successHandler(oAuth2AuthenticationSuccessHandler))
 * </pre>
 * — i.e. {@code .oauth2Login(} is preceded by {@code //} on the same line,
 * while {@link OAuth2AuthenticationSuccessHandler} is still injected and
 * {@code application.properties} still defines
 * {@code spring.security.oauth2.client.registration.google.client-id=...}
 * etc. The single {@code @Test} below fails with both pieces of evidence
 * concatenated into the message.
 */
class SecurityConfigurationBug6ExplorationTest {

    /**
     * Project-relative path to the production {@code SecurityConfiguration}
     * source. Maven runs tests from the project root (the directory
     * containing {@code pom.xml}), so a relative path resolves correctly
     * under both Maven and most IDE runners.
     */
    private static final Path SECURITY_CONFIG_SOURCE = Paths.get(
            "src", "main", "java", "com", "example", "shopnow",
            "security", "SecurityConfiguration.java");

    /** Project-relative path to the production {@code application.properties}. */
    private static final Path APPLICATION_PROPERTIES = Paths.get(
            "src", "main", "resources", "application.properties");

    /**
     * Matches a Java single-line comment that immediately precedes a
     * {@code .oauth2Login(} token on the same line. The leading
     * {@code .*} is non-greedy enough to skip whitespace and
     * indentation that appears before the {@code //}.
     */
    private static final Pattern OAUTH2_LOGIN_COMMENTED = Pattern.compile(
            "^\\s*//.*\\.oauth2Login\\s*\\(");

    /**
     * Matches an active (un-commented) {@code .oauth2Login(} method call.
     * The line MUST NOT start with optional whitespace followed by
     * {@code //} — that case is handled separately as the bug condition.
     */
    private static final Pattern OAUTH2_LOGIN_ACTIVE = Pattern.compile(
            "^(?!\\s*//).*\\.oauth2Login\\s*\\(");

    @Test
    @DisplayName("SecurityConfiguration SHALL EITHER enable .oauth2Login(...) OR fully remove the OAuth2 wiring")
    void oauth2Login_shall_be_enabled_or_fully_removed() throws Exception {
        assertThat(SECURITY_CONFIG_SOURCE)
                .as("Production SecurityConfiguration source must exist for the static scan")
                .exists();
        assertThat(APPLICATION_PROPERTIES)
                .as("Production application.properties must exist for the static scan")
                .exists();

        String securityConfigSource = Files.readString(SECURITY_CONFIG_SOURCE);
        String applicationProperties = Files.readString(APPLICATION_PROPERTIES);

        // Scan SecurityConfiguration line-by-line so we can distinguish
        // an *active* .oauth2Login(...) call from one that has been
        // disabled with a leading // comment.
        boolean hasActiveOauth2Login = false;
        boolean hasCommentedOauth2Login = false;
        String firstCommentedOauth2LoginLine = null;
        for (String rawLine : securityConfigSource.split("\\R", -1)) {
            String line = rawLine; // keep leading whitespace for the regex
            if (OAUTH2_LOGIN_COMMENTED.matcher(line).find()) {
                hasCommentedOauth2Login = true;
                if (firstCommentedOauth2LoginLine == null) {
                    firstCommentedOauth2LoginLine = line.strip();
                }
            } else if (OAUTH2_LOGIN_ACTIVE.matcher(line).find()) {
                hasActiveOauth2Login = true;
            }
        }

        // Branch A: OAuth2 enabled — the production class has an active
        // .oauth2Login(...) call AND keeps OAuth2AuthenticationSuccessHandler
        // as a wired dependency (constructor / field reference).
        boolean handlerInjected =
                securityConfigSource.contains("OAuth2AuthenticationSuccessHandler");

        boolean oauth2EnabledBranchSatisfied =
                hasActiveOauth2Login && handlerInjected;

        // Branch B: OAuth2 fully removed — neither the handler reference
        // nor any spring.security.oauth2.client.* property remains in
        // production sources, so contributors cannot mistake the dead
        // code for a live integration.
        boolean propertiesReferenceOauth2Client =
                applicationProperties.contains("spring.security.oauth2.client.");

        boolean oauth2RemovedBranchSatisfied =
                !handlerInjected && !propertiesReferenceOauth2Client;

        boolean propertyHolds =
                oauth2EnabledBranchSatisfied || oauth2RemovedBranchSatisfied;

        // On unfixed code the assertion message embeds the exact
        // counterexample: the commented .oauth2Login(...) line plus the
        // surviving handler injection / oauth2 client property keys.
        String counterexample = String.format(
                "%n  hasActiveOauth2Login    = %s"
                        + "%n  hasCommentedOauth2Login = %s"
                        + "%n  firstCommentedLine      = %s"
                        + "%n  handlerInjected         = %s"
                        + "%n  oauth2ClientProperties  = %s",
                hasActiveOauth2Login,
                hasCommentedOauth2Login,
                firstCommentedOauth2LoginLine == null
                        ? "<none>"
                        : firstCommentedOauth2LoginLine,
                handlerInjected,
                propertiesReferenceOauth2Client
                        ? collectOauth2ClientPropertyKeys(applicationProperties)
                        : "<none>");

        assertThat(propertyHolds)
                .as("BUG-6: SecurityConfiguration SHALL either keep .oauth2Login(...) "
                        + "active (with OAuth2AuthenticationSuccessHandler injected) "
                        + "OR fully remove the OAuth2 wiring (handler reference + "
                        + "spring.security.oauth2.client.* properties).%s",
                        counterexample)
                .isTrue();
    }

    /**
     * Collect the keys of every {@code spring.security.oauth2.client.*}
     * property declared in {@code application.properties}, joined with
     * {@code ", "}. Used purely to enrich the failure message with a
     * concrete counterexample (the exact property keys that prove the
     * "removed" branch is not satisfied).
     */
    private static String collectOauth2ClientPropertyKeys(String applicationProperties) {
        return Arrays.stream(applicationProperties.split("\\R"))
                .map(String::strip)
                .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                .filter(line -> line.startsWith("spring.security.oauth2.client."))
                .map(line -> {
                    int eq = line.indexOf('=');
                    return eq > 0 ? line.substring(0, eq) : line;
                })
                .reduce((a, b) -> a + ", " + b)
                .orElse("<none>");
    }
}
