package ar.edu.itba.paw.webapp.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.RememberMeAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.servlet.util.matcher.MvcRequestMatcher;
import org.springframework.security.web.util.matcher.AndRequestMatcher;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestHeaderRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.accept.HeaderContentNegotiationStrategy;
import org.springframework.web.servlet.handler.HandlerMappingIntrospector;

import ar.edu.itba.paw.services.user.UserService;
import ar.edu.itba.paw.webapp.security.access.CarNotOwnedByCallerAuthorization;
import ar.edu.itba.paw.webapp.security.access.CarOwnerWebAuthorization;
import ar.edu.itba.paw.webapp.security.access.ProfileWebAuthorization;
import ar.edu.itba.paw.webapp.security.access.ReservationWebAuthorization;
import ar.edu.itba.paw.webapp.security.auth.RydenAuthenticationProvider;
import ar.edu.itba.paw.webapp.security.auth.userdetails.RydenUserDetailsService;

/**
 * Spring Security filter chain: form login, remember-me, CSRF disabled, and path-specific authorization managers.
 */
@Configuration
@EnableWebSecurity
public class WebAuthConfig {

    private final String rememberMeKey;

    public WebAuthConfig(@Value("${app.security.remember-me.key}") final String rememberMeKey) {
        this.rememberMeKey = rememberMeKey;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }

    @Bean
    public UserDetailsService userDetailsService(final UserService userService) {
        return new RydenUserDetailsService(userService);
    }

    @Bean
    public RememberMeAuthenticationProvider rememberMeAuthenticationProvider() {
        return new RememberMeAuthenticationProvider(rememberMeKey);
    }

    @Bean
    public AuthenticationManager authenticationManager(
            final RydenAuthenticationProvider rydenAuthenticationProvider,
            final RememberMeAuthenticationProvider rememberMeAuthenticationProvider) {
        return new ProviderManager(Arrays.asList(rydenAuthenticationProvider, rememberMeAuthenticationProvider));
    }

    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    public RequestCache requestCache() {
        final HttpSessionRequestCache cache = new HttpSessionRequestCache();
        cache.setRequestMatcher(savedRequestMatcher());
        return cache;
    }

    @Bean
    public LogoutHandler contextPathAuthCookieClearingLogoutHandler() {
        return WebAuthConfig::expireAuthCookies;
    }

    /**
     * Static asset folders served by {@code WebConfig.addResourceHandlers}. Ignored entirely by Spring
     * Security so they never traverse the filter chain (no SecurityContext load, no session touch, no
     * CSRF/cors/headers processing).
     */
    @Bean
    public WebSecurityCustomizer staticResourcesSecurityCustomizer() {
        return web -> web.ignoring().requestMatchers("/css/**", "/js/**", "/assets/**");
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            final HttpSecurity http,
            final AuthenticationManager authenticationManager,
            final UserDetailsService userDetailsService,
            final AuthenticationFailureHandler authenticationFailureHandler,
            final SecurityContextRepository securityContextRepository,
            final RequestCache requestCache,
            final LogoutHandler contextPathAuthCookieClearingLogoutHandler,
            final HandlerMappingIntrospector handlerMappingIntrospector,
            final CarOwnerWebAuthorization carOwnerWebAuthorization,
            final ReservationWebAuthorization reservationWebAuthorization,
            final ProfileWebAuthorization profileWebAuthorization,
            final CarNotOwnedByCallerAuthorization carNotOwnedByCallerAuthorization,
            final SessionRegistry sessionRegistry) throws Exception {
        http
                .headers(headers -> headers.cacheControl(Customizer.withDefaults()))
                .csrf(csrf -> csrf.disable())
                .authenticationManager(authenticationManager)
                .securityContext(ctx -> ctx.securityContextRepository(securityContextRepository))
                .requestCache(rc -> rc.requestCache(requestCache))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/error").permitAll()
                        .requestMatchers("/search", "/cars/*").permitAll()
                        .requestMatchers("/image/**").permitAll()
                        .requestMatchers("/car-media/**").permitAll()
                        .requestMatchers(
                                "/register", "/verify-email", "/verify-email/**", "/forgot-password", "/forgot-password/**")
                        .permitAll()
                        .requestMatchers("/publish-car", "/publish-car/**").authenticated()
                        .requestMatchers("/my-cars", "/my-cars/car/**").authenticated()
                        .requestMatchers(
                                mvc(handlerMappingIntrospector, "/my-cars/car/{carId}"),
                                mvc(handlerMappingIntrospector, "/my-cars/car/{carId}/**"))
                        .access(carOwnerWebAuthorization.ownerAccess())
                        // Owner-only filter-side check for the "owner reservations filtered by carId" family.
                        // The owner-only rule is enforced only here; controllers load the Car via
                        // OwnerCarLookup but do not re-check ownership.
                        .requestMatchers(
                                mvc(handlerMappingIntrospector, HttpMethod.GET, "/my-cars/reservations/{carId}"))
                        .access(carOwnerWebAuthorization.ownerAccessForPrefix("/my-cars/reservations/"))
                        .requestMatchers("/my-reservations").authenticated()
                        .requestMatchers(
                                mvc(
                                        handlerMappingIntrospector,
                                        HttpMethod.POST,
                                        "/my-favorites/toggle"))
                        .access(carNotOwnedByCallerAuthorization.nonOwnerAccess())
                        .requestMatchers("/my-favorites", "/my-favorites/**").authenticated()
                        // GET /cars/{carId}/reservation/new: filter-chain enforcement of the rider-cannot-
                        // reserve-own-car rule. The service layer (submitRiderReservationByCar) keeps a
                        // matching check for the POST /reservation submit because its carId travels in the
                        // form body (would force the filter to consume the multipart stream).
                        .requestMatchers(
                                mvc(
                                        handlerMappingIntrospector,
                                        HttpMethod.GET,
                                        "/cars/{carId}/reservation/new"))
                        .access(carNotOwnedByCallerAuthorization.nonOwnerAccess())
                        // Chat-style GETs are also reachable by admins (read-only audit access);
                        // every POST against the same family stays participant-only so admins
                        // cannot impersonate a rider/owner.
                        .requestMatchers(
                                mvc(
                                        handlerMappingIntrospector,
                                        HttpMethod.GET,
                                        "/my-reservations/{reservationId}/messages"))
                        .access(reservationWebAuthorization.participantOrAdminReadAccess())
                        .requestMatchers(
                                mvc(
                                        handlerMappingIntrospector,
                                        HttpMethod.GET,
                                        "/my-reservations/{reservationId}/messages/poll"))
                        .access(reservationWebAuthorization.participantOrAdminReadAccess())
                        .requestMatchers(
                                mvc(
                                        handlerMappingIntrospector,
                                        HttpMethod.POST,
                                        "/my-reservations/{reservationId}/messages"))
                        .access(reservationWebAuthorization.participantAccess())
                        .requestMatchers(
                                mvc(
                                        handlerMappingIntrospector,
                                        HttpMethod.GET,
                                        "/my-reservations/{reservationId}/messages/{messageId}/attachment/download"))
                        .access(reservationWebAuthorization.participantOrAdminReadAccess())
                        .requestMatchers(
                                mvc(
                                        handlerMappingIntrospector,
                                        HttpMethod.GET,
                                        "/my-reservations/{reservationId}/messages/{messageId}/attachment/view"))
                        .access(reservationWebAuthorization.participantOrAdminReadAccess())
                        .requestMatchers(
                                mvc(
                                        handlerMappingIntrospector,
                                        HttpMethod.GET,
                                        "/my-reservations/{reservationId}/payment-receipt/download"))
                        .access(reservationWebAuthorization.participantAccess())
                        .requestMatchers(
                                mvc(
                                        handlerMappingIntrospector,
                                        HttpMethod.GET,
                                        "/my-reservations/{reservationId}/payment-receipt/view"))
                        .access(reservationWebAuthorization.participantAccess())
                        .requestMatchers(
                                mvc(
                                        handlerMappingIntrospector,
                                        HttpMethod.POST,
                                        "/my-reservations/{reservationId}/payment-receipt"))
                        .access(reservationWebAuthorization.riderAccess())
                        .requestMatchers(
                                mvc(
                                        handlerMappingIntrospector,
                                        HttpMethod.GET,
                                        "/my-reservations/{reservationId}/refund-receipt/download"))
                        .access(reservationWebAuthorization.participantAccess())
                        .requestMatchers(
                                mvc(
                                        handlerMappingIntrospector,
                                        HttpMethod.GET,
                                        "/my-reservations/{reservationId}/refund-receipt/view"))
                        .access(reservationWebAuthorization.participantAccess())
                        .requestMatchers(
                                mvc(
                                        handlerMappingIntrospector,
                                        HttpMethod.POST,
                                        "/my-reservations/{reservationId}/refund-receipt"))
                        .access(reservationWebAuthorization.ownerAccess())
                        .requestMatchers(
                                mvc(
                                        handlerMappingIntrospector,
                                        HttpMethod.POST,
                                        "/my-reservations/{reservationId}/car-returned"))
                        .access(reservationWebAuthorization.ownerAccess())
                        .requestMatchers(
                                mvc(
                                        handlerMappingIntrospector,
                                        HttpMethod.POST,
                                        "/my-reservations/{reservationId}/owner-review-rider"))
                        .access(reservationWebAuthorization.ownerAccess())
                        .requestMatchers(
                                mvc(
                                        handlerMappingIntrospector,
                                        HttpMethod.POST,
                                        "/my-reservations/{reservationId}/rider-review-owner"))
                        .access(reservationWebAuthorization.riderAccess())
                        .requestMatchers(
                                mvc(
                                        handlerMappingIntrospector,
                                        HttpMethod.POST,
                                        "/my-reservations/{reservationId}/cancel"))
                        .access(reservationWebAuthorization.participantAccess())
                        // Rider-side period edit: only the rider on a still-unpaid PENDING reservation
                        // may reach either the GET form or the POST update. The service layer keeps an
                        // identical guard so a programmatic caller cannot bypass this filter check.
                        .requestMatchers(
                                mvc(
                                        handlerMappingIntrospector,
                                        HttpMethod.GET,
                                        "/my-reservations/{reservationId}/edit"))
                        .access(reservationWebAuthorization.riderUnpaidPendingAccess())
                        .requestMatchers(
                                mvc(
                                        handlerMappingIntrospector,
                                        HttpMethod.POST,
                                        "/my-reservations/{reservationId}/edit"))
                        .access(reservationWebAuthorization.riderUnpaidPendingAccess())
                        .requestMatchers(
                                mvc(
                                        handlerMappingIntrospector,
                                        HttpMethod.GET,
                                        "/my-reservations/{reservationId}/counterparty-profile"))
                        .access(reservationWebAuthorization.participantAccess())
                        .requestMatchers(
                                mvc(
                                        handlerMappingIntrospector,
                                        HttpMethod.GET,
                                        "/my-reservations/{reservationId}/chat"))
                        .access(reservationWebAuthorization.participantOrAdminReadAccess())
                        .requestMatchers(
                                mvc(
                                        handlerMappingIntrospector,
                                        HttpMethod.GET,
                                        "/my-reservations/{reservationId}"))
                        .access(reservationWebAuthorization.participantAccess())
                        .requestMatchers("/reservation", "/reservation/**").authenticated()
                        .requestMatchers("/admin", "/admin/**").hasRole("ADMIN")
                        .requestMatchers("/login").permitAll()
                        .requestMatchers("/logout").authenticated()
                        .requestMatchers("/profile", "/profile/**")
                        .access(profileWebAuthorization.selfProfileAccess())
                        .anyRequest().permitAll())
                .exceptionHandling(ex -> ex.accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.sendRedirect(request.getContextPath() + accessDeniedRedirectTarget(request));
                }))
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .usernameParameter("email")
                        .passwordParameter("password")
                        .defaultSuccessUrl("/", false)
                        .failureHandler(authenticationFailureHandler))
                .logout(logout -> logout
                        .logoutRequestMatcher(new OrRequestMatcher(
                                new AntPathRequestMatcher("/logout", "GET"),
                                new AntPathRequestMatcher("/logout", "POST")))
                        .addLogoutHandler(contextPathAuthCookieClearingLogoutHandler)
                        .logoutSuccessUrl("/login?logout")
                        .invalidateHttpSession(true)
                        .deleteCookies("remember-me", "JSESSIONID"))
                .rememberMe(remember -> remember
                        .key(rememberMeKey)
                        .rememberMeParameter("remember-me")
                        .rememberMeCookieName("remember-me")
                        .tokenValiditySeconds((int) TimeUnit.DAYS.toSeconds(30))
                        .userDetailsService(userDetailsService))
                .sessionManagement(sm -> sm
                        .maximumSessions(-1)
                        .sessionRegistry(sessionRegistry));
        return http.build();
    }

    private static void expireAuthCookies(
            final HttpServletRequest request,
            final HttpServletResponse response,
            @SuppressWarnings("unused") final Authentication authentication) {
        final String contextPath = request.getContextPath() != null ? request.getContextPath() : "";
        final String[] paths;
        if (contextPath.isEmpty() || "/".equals(contextPath)) {
            paths = new String[] {"/", "/webapp"};
        } else {
            paths = new String[] {contextPath};
        }
        for (final String path : paths) {
            for (final String name : new String[] {"remember-me", "JSESSIONID"}) {
                final Cookie c = new Cookie(name, null);
                c.setPath(path);
                c.setMaxAge(0);
                c.setHttpOnly(true);
                if (request.isSecure()) {
                    c.setSecure(true);
                }
                response.addCookie(c);
            }
        }
    }

    /**
     * Limits which unauthenticated requests are stored for post-login redirect. Excludes JSON/XHR
     * APIs (e.g. reservation chat poll) so a background fetch cannot become the login success target.
     */
    private static RequestMatcher savedRequestMatcher() {
        final List<RequestMatcher> matchers = new ArrayList<>();
        matchers.add(new AntPathRequestMatcher("/**", HttpMethod.GET.name()));
        matchers.add(new NegatedRequestMatcher(new AntPathRequestMatcher("/**/messages/poll")));
        matchers.add(new NegatedRequestMatcher(
                new RequestHeaderRequestMatcher("X-Requested-With", "XMLHttpRequest")));
        final MediaTypeRequestMatcher jsonRequest = new MediaTypeRequestMatcher(
                new HeaderContentNegotiationStrategy(), MediaType.APPLICATION_JSON);
        jsonRequest.setIgnoredMediaTypes(Collections.singleton(MediaType.ALL));
        jsonRequest.setUseEquals(true);
        matchers.add(new NegatedRequestMatcher(jsonRequest));
        return new AndRequestMatcher(matchers);
    }

    private static MvcRequestMatcher mvc(
            final HandlerMappingIntrospector introspector,
            final HttpMethod method,
            final String pattern) {
        final MvcRequestMatcher m = new MvcRequestMatcher(introspector, pattern);
        m.setMethod(method);
        return m;
    }

    private static MvcRequestMatcher mvc(
            final HandlerMappingIntrospector introspector, final String pattern) {
        return new MvcRequestMatcher(introspector, pattern);
    }

    /**
     * Picks a sensible same-hub fallback for {@code accessDeniedHandler} so a viewer that tripped the
     * filter chain on, say, {@code /my-cars/car/{otherUsersCar}/edit} lands on {@code /my-cars} (not
     * the unrelated home page). Mirrors the redirect targets that the controllers used to compute
     * inline before access control moved into the security filter chain.
     */
    private static String accessDeniedRedirectTarget(final HttpServletRequest request) {
        final String servletPath = request.getServletPath();
        final String path = servletPath != null ? servletPath : "";
        if (path.startsWith("/my-cars/reservations")) {
            return "/my-cars/reservations";
        }
        if (path.startsWith("/my-cars")) {
            return "/my-cars";
        }
        if (path.startsWith("/my-reservations")) {
            return "/my-reservations";
        }
        return "/";
    }
}
