package ar.edu.itba.paw.webapp.support;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import ar.edu.itba.paw.webapp.security.auth.userdetails.RydenUserDetails;

/**
 * Authz predicates for user resources (401/403 intent): self vs stranger vs admin.
 * Complements {@link ar.edu.itba.paw.webapp.security.PreAuthorizeMethodSecurityTest}
 * without duplicating the CGLIB/@PreAuthorize wiring smoke.
 */
class UserResourceAccessTest {

    private final UserResourceAccess access = new UserResourceAccess();

    private RydenUserDetails self;
    private RydenUserDetails stranger;

    @BeforeEach
    void setUp() {
        self = viewer(10L);
        stranger = viewer(99L);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testSelfCanAccessOwnProfile() {
        // 1.Arrange — viewer {@code self} (id 10) from {@link #setUp}

        // 2.Act / 3.Assert
        assertTrue(access.isSelf(10L, self));
        assertTrue(access.isSelfOrAdmin(10L, self));
    }

    @Test
    void testStrangerCannotAccessOtherProfile() {
        // 1.Arrange — viewer {@code stranger} (id 99) from {@link #setUp}; no admin in SecurityContext

        // 2.Act / 3.Assert
        assertFalse(access.isSelf(10L, stranger));
        assertFalse(access.isSelfOrAdmin(10L, stranger));
        assertThrows(AccessDeniedException.class, () -> access.requireSelfOrAdmin(10L, stranger));
    }

    @Test
    void testAnonymousViewerIsDenied() {
        // 1.Arrange / 2.Act / 3.Assert
        assertFalse(access.isSelf(10L, null));
        assertFalse(access.isSelfOrAdmin(10L, null));
        assertThrows(AccessDeniedException.class, () -> access.requireSelf(10L, null));
    }

    @Test
    void testAdminCanAccessOtherProfile() {
        // 1.Arrange
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken(
                        "admin@example.com",
                        "x",
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));

        // 2.Act / 3.Assert
        assertTrue(access.isAdmin());
        assertTrue(access.isSelfOrAdmin(10L, stranger));
    }

    private static RydenUserDetails viewer(final long userId) {
        return new RydenUserDetails(userId, "u@example.com", "A", "B", "hash", List.of(), null);
    }
}
