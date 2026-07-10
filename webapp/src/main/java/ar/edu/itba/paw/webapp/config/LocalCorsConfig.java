package ar.edu.itba.paw.webapp.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Permissive CORS for local development ({@code spring.profiles.active=local}): Vite dev server,
 * Postman, or cross-origin tooling against Jetty/Tomcat. Not registered in deployed profile — the
 * WAR serves SPA + API same-origin and {@link WebAuthConfig} disables CORS there (auditoría A1).
 */
@Configuration
@Profile("local")
public class LocalCorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        return devCorsConfigurationSource();
    }

    static CorsConfigurationSource devCorsConfigurationSource() {
        final CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of(
                "Link",
                "X-Total-Count",
                "X-Access-Token",
                "X-Refresh-Token",
                "Location",
                "WWW-Authenticate",
                "Content-Type",
                "Cache-Control",
                "ETag",
                "Vary"));

        final UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
