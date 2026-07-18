package ar.edu.itba.paw.webapp.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.cors.CorsConfigurationSource;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.savedrequest.NullRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import ar.edu.itba.paw.services.user.UserService;
import ar.edu.itba.paw.webapp.api.common.VndMediaType;
import ar.edu.itba.paw.webapp.security.auth.RydenAuthenticationProvider;
import ar.edu.itba.paw.webapp.security.auth.userdetails.RydenUserDetailsService;
import ar.edu.itba.paw.webapp.security.jwt.JwtAuthenticationFilter;
import ar.edu.itba.paw.webapp.security.jwt.TokenService;

/**
 * Stateless Spring Security for the REST API (LINEAMIENTOS-SPA-REST.md §1.8): no sessions, no form login,
 * no remember-me, no CSRF. Authentication is resolved per-request from the {@code Authorization} header by
 * {@link JwtAuthenticationFilter}. Coarse path rules live here (public reads vs. authenticated writes).
 *
 * Fine-grained owner/admin/participant scoping is delegated to Spring Security's method-security AOP
 * interceptor via {@code @PreAuthorize} on the JAX-RS resource methods (backed by the predicate beans in
 * {@code webapp.support}, e.g. {@code UserResourceAccess}/{@code ReservationResourceAccess}/
 * {@code CarResourceAccess}), instead of the resources throwing {@code ForbiddenException} by hand.
 * {@code proxyTargetClass = true} is required because JAX-RS resources are concrete classes with no
 * interface (Jersey's {@code SpringComponentProvider} only recognizes {@code @Component} beans and resolves
 * them through the Spring proxy, so the advised bean must be CGLIB-proxyable — the resource classes touched
 * by {@code @PreAuthorize} are therefore non-final). A handful of checks stay imperative in the resource
 * body where the decision genuinely depends on parsed request content (e.g. per-field PATCH routing) rather
 * than being a pure precondition on the path/query parameters.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(proxyTargetClass = true)
public class WebAuthConfig {

    /** Thread-safe; reused to serialize the error body so message text is always JSON-escaped. */
    private static final ObjectMapper ERROR_MAPPER = new ObjectMapper();

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService(final UserService userService) {
        return new RydenUserDetailsService(userService);
    }

    @Bean
    public AuthenticationManager authenticationManager(
            final RydenAuthenticationProvider rydenAuthenticationProvider) {
        return new ProviderManager(List.of(rydenAuthenticationProvider));
    }

    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new RequestAttributeSecurityContextRepository();
    }

    /** Stateless: no SavedRequest / post-login redirects (legacy {@code RegistrationController}). */
    @Bean
    public RequestCache requestCache() {
        return new NullRequestCache();
    }

    /**
     * Static SPA assets bypass the security filter chain entirely (no SecurityContext work, no headers
     * processing). Revved {@code /public} and non-revved {@code /assets} (LINEAMIENTOS §4.3).
     */
    @Bean
    public WebSecurityCustomizer staticResourcesSecurityCustomizer() {
        return web -> web.ignoring().requestMatchers("/public/**", "/assets/**");
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(
            final AuthenticationManager authenticationManager,
            final TokenService tokenService,
            final UserService userService) {
        return new JwtAuthenticationFilter(authenticationManager, tokenService, userService);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            final HttpSecurity http,
            final AuthenticationManager authenticationManager,
            final JwtAuthenticationFilter jwtFilter,
            final ObjectProvider<CorsConfigurationSource> corsConfigurationSource,
            final SecurityContextRepository securityContextRepository) throws Exception {

        http
                .csrf(csrf -> csrf.disable())
                .formLogin(form -> form.disable())
                .logout(logout -> logout.disable())
                .anonymous(anon -> {
                })
                .authenticationManager(authenticationManager)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .securityContext(ctx -> ctx.securityContextRepository(securityContextRepository))
                .headers(headers -> headers
                        // X-Content-Type-Options: nosniff — never let the browser MIME-sniff a response
                        // body into an executable type (defence in depth alongside per-binary headers).
                        .contentTypeOptions(contentTypeOptions -> {
                        })
                        // X-Frame-Options: DENY — the SPA never frames itself (clickjacking protection),
                        // aligned with the CSP frame-ancestors 'none'.
                        .frameOptions(frameOptions -> frameOptions.deny())
                        // Referrer-Policy: strip path/query when navigating cross-origin so internal
                        // resource URLs (which may carry ids) never leak in the Referer header.
                        .referrerPolicy(referrer -> referrer.policy(
                                org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter
                                        .ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                        // Conservative CSP. Scripts stay 'self' (the Vite bundle is same-origin).
                        // Google Fonts remain allowlisted until the SPA self-hosts Plus Jakarta /
                        // Inter under /public (then drop fonts.googleapis.com / fonts.gstatic.com from CSP).
                        // 'unsafe-inline' on style-src covers React/Bootstrap inline styles.
                        // Images/media also allow {@code data:} and {@code blob:} (local upload
                        // previews via {@code URL.createObjectURL}). Everything else is locked to self,
                        // no plugins (object-src 'none'), no framing ancestors, fixed base-uri.
                        .contentSecurityPolicy(csp -> csp.policyDirectives(
                                "default-src 'self'; img-src 'self' data: blob:; "
                                        + "media-src 'self' blob:; "
                                        + "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; "
                                        + "font-src 'self' https://fonts.gstatic.com; "
                                        + "object-src 'none'; base-uri 'self'; frame-ancestors 'none'")))
                .authorizeHttpRequests(auth -> auth
                        // SPA entrypoint + static fallbacks (served by the default servlet on Jersey 404).
                        .requestMatchers(ant("/"), ant("/index.html"), ant("/favicon.ico")).permitAll()
                        // SPA deep-links (same-origin shell; API auth via Bearer).
                        .requestMatchers(
                                ant("/search"), ant("/home"), ant("/cars/**"), ant("/login"), ant("/register"),
                                ant("/verify-email"), ant("/forgot-password"), ant("/publish-car"),
                                ant("/my-cars/**"), ant("/my-reservations/**"), ant("/profile"),
                                ant("/my-favorites"), ant("/users/**"), ant("/admin/**"))
                        .permitAll()
                        // CORS preflight (harmless when same-origin).
                        .requestMatchers(ant(HttpMethod.OPTIONS, "/**")).permitAll()
                        // Sensitive car sub-resources: authenticated at the filter (defense in depth);
                        // ownership remains enforced by @PreAuthorize on the resource.
                        .requestMatchers(ant(HttpMethod.GET, "/api/cars/*/insurance")).authenticated()
                        // Public reads: catalog + public car/profile data (Jersey under /api/*).
                        .requestMatchers(
                                ant(HttpMethod.GET, "/api"),
                                ant(HttpMethod.GET, "/api/"),
                                ant(HttpMethod.HEAD, "/api"),
                                ant(HttpMethod.HEAD, "/api/"),
                                ant(HttpMethod.GET, "/api/config"),
                                ant(HttpMethod.GET, "/api/cars"),
                                ant(HttpMethod.GET, "/api/cars/**"),
                                ant(HttpMethod.GET, "/api/brands"),
                                ant(HttpMethod.GET, "/api/brands/**"),
                                ant(HttpMethod.GET, "/api/models"),
                                ant(HttpMethod.GET, "/api/neighborhoods"),
                                ant(HttpMethod.GET, "/api/neighborhoods/**"),
                                ant(HttpMethod.GET, "/api/users/*"),
                                ant(HttpMethod.GET, "/api/users/*/profile-picture"),
                                ant(HttpMethod.GET, "/api/reviews"),
                                ant(HttpMethod.GET, "/api/reviews/*"),
                                ant(HttpMethod.GET, "/api/image/*"))
                        .permitAll()
                        // Account bootstrap: registration + OTP credential issuance (no verb URLs).
                        .requestMatchers(ant(HttpMethod.POST, "/api/users")).permitAll()
                        .requestMatchers(ant(HttpMethod.POST, "/api/credentials")).permitAll()
                        .requestMatchers(ant(HttpMethod.POST, "/api/users/*/credentials")).permitAll()
                        // PATCH /users/{id}: password reset via Basic OTP (authenticated with
                        // ROLE_PASSWORD_RESET_OTP), or self/admin profile updates (Bearer JWT).
                        .requestMatchers(ant(HttpMethod.PATCH, "/api/users/*")).authenticated()
                        // Everything else needs authentication; owner/admin/participant scoping is per-resource.
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(restAuthenticationEntryPoint())
                        .accessDeniedHandler(restAccessDeniedHandler()))
                // Runs before anonymous + authorization so a valid token populates the context first.
                .addFilterBefore(jwtFilter, AnonymousAuthenticationFilter.class)
                .cors(cors -> {
                    final CorsConfigurationSource source = corsConfigurationSource.getIfAvailable();
                    if (source != null) {
                        cors.configurationSource(source);
                    } else {
                        cors.disable();
                    }
                });

        return http.build();
    }

    private static AntPathRequestMatcher ant(final String pattern) {
        return new AntPathRequestMatcher(pattern);
    }

    private static AntPathRequestMatcher ant(final HttpMethod method, final String pattern) {
        return new AntPathRequestMatcher(pattern, method.name());
    }

    /** 401 (no authentication) as a vnd.paw.error body — never a redirect to a login page. */
    private static AuthenticationEntryPoint restAuthenticationEntryPoint() {
        return (request, response, authException) -> {
            // RFC 7235 §4.1 requires WWW-Authenticate on every 401. This belongs on the 401 only —
            // never on the 403 (the caller IS authenticated there) — Q&A G11 Vitae "Header
            // www-authenticate". Only advertise Bearer: Basic is accepted on login-style requests
            // (`Authorization: Basic` built by the SPA's own JS, never a browser-native prompt) but is
            // never the scheme actually protecting a resource here — advertising "Basic" in the
            // challenge makes any browser that reaches a 401 directly (typed URL, broken subresource
            // link, no-JS request) pop up its native credential dialog, which this stateless JWT API
            // never wants (another group's graded correction: "login form nativo del browser producto
            // de un 401 mal manejado").
            response.setHeader("WWW-Authenticate", "Bearer realm=\"ryden\"");
            writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "unauthorized",
                    "Authentication required.");
        };
    }

    /** 403 (authenticated but not allowed). */
    private static AccessDeniedHandler restAccessDeniedHandler() {
        return (request, response, accessDeniedException) ->
                writeError(response, HttpServletResponse.SC_FORBIDDEN, "forbidden",
                        "You do not have permission to perform this action.");
    }

    private static void writeError(final HttpServletResponse response, final int status,
                                   final String code, final String message) throws IOException {
        response.setStatus(status);
        response.setContentType(VndMediaType.ERROR_V1_JSON);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        // Serialize via Jackson so a code/message containing a quote or newline is properly escaped
        // instead of breaking the hand-built JSON. LinkedHashMap keeps the field order stable.
        final Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", status);
        body.put("code", code);
        body.put("message", message);
        response.getWriter().write(ERROR_MAPPER.writeValueAsString(body));
    }
}
