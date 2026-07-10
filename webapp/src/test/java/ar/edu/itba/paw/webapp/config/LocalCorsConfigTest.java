package ar.edu.itba.paw.webapp.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Local CORS policy: permissive only under {@code local} profile wiring.
 */
class LocalCorsConfigTest {

    @Test
    void testDevCorsAllowsWildcardOrigins() {
        // 1.Arrange
        final CorsConfigurationSource source = LocalCorsConfig.devCorsConfigurationSource();
        final MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/api/cars");

        // 2.Act
        final CorsConfiguration config =
                ((UrlBasedCorsConfigurationSource) source).getCorsConfiguration(request);

        // 3.Assert
        assertNotNull(config);
        assertEquals(java.util.List.of("*"), config.getAllowedOriginPatterns());
        assertTrue(config.getExposedHeaders().contains("Vary"));
    }
}
