package ar.edu.itba.paw.webapp.config;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.RememberMeAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.Authentication;
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
import org.springframework.http.HttpMethod;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;

import ar.edu.itba.paw.models.security.UserRole;
import ar.edu.itba.paw.services.UserService;
import ar.edu.itba.paw.webapp.security.ListingWebAuthorization;
import ar.edu.itba.paw.webapp.security.ReservationWebAuthorization;
import ar.edu.itba.paw.webapp.security.RydenAuthenticationProvider;
import ar.edu.itba.paw.webapp.security.RydenUserDetailsService;

@Configuration
@EnableWebSecurity
public class WebAuthConfig {

    static final String REMEMBER_ME_KEY = "ryden-webapp-remember-me-key";

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService(final UserService userService) {
        return new RydenUserDetailsService(userService);
    }

    @Bean
    public RememberMeAuthenticationProvider rememberMeAuthenticationProvider() {
        return new RememberMeAuthenticationProvider(REMEMBER_ME_KEY);
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
        return new HttpSessionRequestCache();
    }

    @Bean
    public LogoutHandler contextPathAuthCookieClearingLogoutHandler() {
        return WebAuthConfig::expireAuthCookies;
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
            final ListingWebAuthorization listingWebAuthorization,
            final ReservationWebAuthorization reservationWebAuthorization) throws Exception {
        http
                .authenticationManager(authenticationManager)
                .securityContext(ctx -> ctx.securityContextRepository(securityContextRepository))
                .requestCache(rc -> rc.requestCache(requestCache))
                .authorizeHttpRequests(auth -> auth
                        .antMatchers("/css/**", "/js/**", "/assets/**").permitAll()
                        .antMatchers("/error").permitAll()
                        .antMatchers("/search", "/car-detail").permitAll()
                        .antMatchers("/image/**").permitAll()
                        .antMatchers("/register", "/verify-email", "/verify-email/**", "/forgot-password", "/forgot-password/**")
                        .permitAll()
                        .antMatchers("/publish-car", "/publish-car/**").authenticated()
                        .antMatchers("/my-listings").authenticated()
                        .antMatchers("/my-listings/**")
                                .access(listingWebAuthorization.ownerAccess())
                        .antMatchers("/my-reservations").authenticated()
                        .antMatchers("/my-reservations/*/payment-receipt/approval")
                                .access(reservationWebAuthorization.ownerAccess())
                        .antMatchers("/my-reservations/*/payment-receipt/download")
                                .access(reservationWebAuthorization.participantAccess())
                        .antMatchers(HttpMethod.POST, "/my-reservations/*/payment-receipt")
                                .access(reservationWebAuthorization.riderAccess())
                        .antMatchers("/my-reservations/*/car-returned")
                                .access(reservationWebAuthorization.ownerAccess())
                        .antMatchers("/my-reservations/*/owner-review-rider")
                                .access(reservationWebAuthorization.ownerAccess())
                        .antMatchers("/my-reservations/*/rider-review-owner")
                                .access(reservationWebAuthorization.riderAccess())
                        .antMatchers("/my-reservations/*/cancel")
                                .access(reservationWebAuthorization.participantAccess())
                        .antMatchers("/my-reservations/*/counterparty-profile")
                                .access(reservationWebAuthorization.participantAccess())
                        .antMatchers("/my-reservations/*")
                                .access(reservationWebAuthorization.participantAccess())
                        .antMatchers("/reservation", "/reservation/**").authenticated()
                        .antMatchers("/login").permitAll()
                        .antMatchers("/logout").authenticated()
                        .antMatchers("/profile", "/profile/**").hasRole(UserRole.USER.name())
                        .anyRequest().permitAll())
                .exceptionHandling(ex -> ex.accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.sendRedirect(request.getContextPath() + "/");
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
                        .key(REMEMBER_ME_KEY)
                        .rememberMeParameter("remember-me")
                        .rememberMeCookieName("remember-me")
                        .tokenValiditySeconds((int) TimeUnit.DAYS.toSeconds(30))
                        .userDetailsService(userDetailsService));
        return http.build();
    }

    private static void expireAuthCookies(
            final HttpServletRequest request,
            final HttpServletResponse response,
            @SuppressWarnings("unused") final Authentication authentication) {
        final String path = request.getContextPath() != null && !request.getContextPath().isEmpty()
                ? request.getContextPath()
                : "/";
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
