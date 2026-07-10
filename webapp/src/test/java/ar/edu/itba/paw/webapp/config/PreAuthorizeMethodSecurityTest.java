package ar.edu.itba.paw.webapp.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Verifies the mechanism {@code WebAuthConfig} relies on for A3 (declarative {@code @PreAuthorize}
 * on JAX-RS resources): Jersey's {@code SpringComponentProvider} only recognizes {@code @Component}
 * beans with no JDK interface, so Spring AOP must proxy the *concrete class* (CGLIB), which requires
 * {@code @EnableMethodSecurity(proxyTargetClass = true)}. This test wires that exact annotation on a
 * throwaway non-final, interface-less {@code @Component} — standing in for a real resource class
 * such as {@code UserController} — and checks the advice actually runs through the proxy.
 */
class PreAuthorizeMethodSecurityTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testPreAuthorizeDeniesOnConcreteComponentWithoutInterface() {
        // 1.Arrange
        try (AnnotationConfigApplicationContext context =
                new AnnotationConfigApplicationContext(TestConfig.class)) {
            SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("user", "pass"));
            final GuardedComponent bean = context.getBean(GuardedComponent.class);

            // 2.Act / 3.Assert
            assertThrows(AccessDeniedException.class, () -> bean.guarded(false));
        }
    }

    @Test
    void testPreAuthorizeAllowsOnConcreteComponentWithoutInterface() {
        // 1.Arrange
        try (AnnotationConfigApplicationContext context =
                new AnnotationConfigApplicationContext(TestConfig.class)) {
            SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("user", "pass"));
            final GuardedComponent bean = context.getBean(GuardedComponent.class);

            // 2.Act
            final String result = bean.guarded(true);

            // 3.Assert
            assertEquals("ok", result);
        }
    }

    @Configuration
    @EnableMethodSecurity(proxyTargetClass = true)
    static class TestConfig {
        @Bean
        GuardedComponent guardedComponent() {
            return new GuardedComponent();
        }
    }

    /** Deliberately non-final and interface-less, like the real JAX-RS resource classes. */
    @Component
    static class GuardedComponent {
        @PreAuthorize("#allowed")
        public String guarded(final boolean allowed) {
            return "ok";
        }
    }
}
