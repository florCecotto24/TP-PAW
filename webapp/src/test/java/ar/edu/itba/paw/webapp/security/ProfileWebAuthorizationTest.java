package ar.edu.itba.paw.webapp.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;

import ar.edu.itba.paw.services.UserService;

import java.util.List;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
public class ProfileWebAuthorizationTest {

    @Test
    public void ownerAccess_allowsOwner() {
        final UserService us = mock(UserService.class);
        final ProfileWebAuthorization authz = new ProfileWebAuthorization(us);

        final RydenUserDetails principal = new RydenUserDetails(
                123L,
                "owner@example.com",
                "Owner",
                "User",
                "hashed",
                List.of(new SimpleGrantedAuthority("ROLE_USER")));

        final Authentication auth = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        final MockHttpServletRequest req = new MockHttpServletRequest();
        req.setServletPath("/profile/123/edit");

        final Supplier<Authentication> sup = () -> auth;
        final var mgr = authz.ownerAccess();
        final var decision = mgr.check(sup, new RequestAuthorizationContext(req));
        assertTrue(decision.isGranted());
    }

    @Test
    public void ownerAccess_deniesDifferentUser() {
        final UserService us = mock(UserService.class);
        final ProfileWebAuthorization authz = new ProfileWebAuthorization(us);

        final RydenUserDetails principal = new RydenUserDetails(
                100L,
                "other@example.com",
                "Other",
                "User",
                "hashed",
                List.of(new SimpleGrantedAuthority("ROLE_USER")));

        final Authentication auth = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        final MockHttpServletRequest req = new MockHttpServletRequest();
        req.setServletPath("/profile/123/edit");

        final Supplier<Authentication> sup = () -> auth;
        final var mgr = authz.ownerAccess();
        final var decision = mgr.check(sup, new RequestAuthorizationContext(req));
        assertFalse(decision.isGranted());
    }

    @Test
    public void ownerAccess_deniesAnonymous() {
        final UserService us = mock(UserService.class);
        final ProfileWebAuthorization authz = new ProfileWebAuthorization(us);

        final Authentication auth = new AnonymousAuthenticationToken("key", "anonymousUser", List.of(new SimpleGrantedAuthority("ROLE_ANON")));

        final MockHttpServletRequest req = new MockHttpServletRequest();
        req.setServletPath("/profile/123/edit");

        final Supplier<Authentication> sup = () -> auth;
        final var mgr = authz.ownerAccess();
        final var decision = mgr.check(sup, new RequestAuthorizationContext(req));
        assertFalse(decision.isGranted());
    }
}

