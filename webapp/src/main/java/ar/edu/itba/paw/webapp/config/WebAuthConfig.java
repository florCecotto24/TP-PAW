package ar.edu.itba.paw.webapp.config;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;

import ar.edu.itba.paw.services.UserService;
import ar.edu.itba.paw.webapp.security.RydenAuthenticationProvider;
import ar.edu.itba.paw.webapp.security.RydenUserDetailsService;

@Configuration
@EnableWebSecurity
public class WebAuthConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService(final UserService userService) {
        return new RydenUserDetailsService(userService);
    }

    @Bean
    public AuthenticationManager authenticationManager(final RydenAuthenticationProvider rydenAuthenticationProvider) {
        return new ProviderManager(Collections.singletonList(rydenAuthenticationProvider));
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            final HttpSecurity http,
            final AuthenticationManager authenticationManager,
            final UserDetailsService userDetailsService,
            final AuthenticationFailureHandler authenticationFailureHandler) throws Exception {
        http
                .authenticationManager(authenticationManager)
                .authorizeHttpRequests(auth -> auth
                        .antMatchers("/css/**", "/js/**", "/assets/**").permitAll()
                        .antMatchers("/error").permitAll()
                        .antMatchers("/search", "/car-detail").permitAll()
                        .antMatchers("/image/**").permitAll()
                        .antMatchers("/register", "/register/**", "/verify-email").permitAll()
                        .antMatchers("/publish-car", "/publish-car/**").authenticated()
                        .antMatchers("/reservation", "/reservation/**").authenticated()
                        .antMatchers("/login").permitAll()
                        .antMatchers("/logout").authenticated()
                        .antMatchers("/profile", "/profile/**").hasRole("USER")
                        .anyRequest().permitAll())
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
                        .logoutSuccessUrl("/login?logout")
                        .invalidateHttpSession(true)
                        .deleteCookies("remember-me", "JSESSIONID"))
                .rememberMe(remember -> remember
                        .key("paw-webapp-remember-me-key")
                        .tokenValiditySeconds((int) TimeUnit.DAYS.toSeconds(30))
                        .userDetailsService(userDetailsService));
        return http.build();
    }
}
