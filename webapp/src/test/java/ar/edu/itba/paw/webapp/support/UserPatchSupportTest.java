package ar.edu.itba.paw.webapp.support;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import ar.edu.itba.paw.models.dto.profile.UserPatchCommand;
import ar.edu.itba.paw.webapp.form.user.UserPatchForm;
import ar.edu.itba.paw.webapp.security.auth.RydenAuthorities;
import ar.edu.itba.paw.webapp.security.auth.userdetails.RydenUserDetails;

class UserPatchSupportTest {

    private UserPatchSupport support;

    @BeforeEach
    void setUp() {
        support = new UserPatchSupport(null, null, null, null, null, null);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testPasswordResetOtpAllowsPasswordAndConfirmationOnly() {
        // 1.Arrange
        setPasswordResetOtpAuthentication();
        final UserPatchForm form = new UserPatchForm();
        form.setPassword("newPass12");
        form.setPasswordConfirm("newPass12");
        final UserPatchCommand command = support.toPatchCommand(form);

        // 2.Act / 3.Assert
        assertDoesNotThrow(() -> support.assertPasswordResetOtpPatchAllowed(form, command));
    }

    @Test
    void testPasswordResetOtpRejectsProfileFields() {
        // 1.Arrange
        setPasswordResetOtpAuthentication();
        final UserPatchForm form = new UserPatchForm();
        form.setPassword("newPass12");
        form.setPasswordConfirm("newPass12");
        form.setForename("Ada");
        final UserPatchCommand command = support.toPatchCommand(form);

        // 2.Act / 3.Assert
        assertThrows(AccessDeniedException.class, () -> support.assertPasswordResetOtpPatchAllowed(form, command));
    }

    @Test
    void testPasswordResetOtpRejectsAdminFields() {
        // 1.Arrange
        setPasswordResetOtpAuthentication();
        final UserPatchForm form = new UserPatchForm();
        form.setPassword("newPass12");
        form.setPasswordConfirm("newPass12");
        form.setBlocked(Boolean.TRUE);
        final UserPatchCommand command = support.toPatchCommand(form);

        // 2.Act / 3.Assert
        assertThrows(AccessDeniedException.class, () -> support.assertPasswordResetOtpPatchAllowed(form, command));
    }

    @Test
    void testPasswordResetOtpRejectsCurrentPasswordField() {
        // 1.Arrange
        setPasswordResetOtpAuthentication();
        final UserPatchForm form = new UserPatchForm();
        form.setPassword("newPass12");
        form.setPasswordConfirm("newPass12");
        form.setCurrentPassword("oldPass12");
        final UserPatchCommand command = support.toPatchCommand(form);

        // 2.Act / 3.Assert
        assertThrows(AccessDeniedException.class, () -> support.assertPasswordResetOtpPatchAllowed(form, command));
    }

    @Test
    void testNormalAuthenticationKeepsProfilePatchAllowedForServiceAuthorization() {
        // 1.Arrange
        final UserPatchForm form = new UserPatchForm();
        form.setForename("Ada");
        final UserPatchCommand command = support.toPatchCommand(form);

        // 2.Act / 3.Assert
        assertDoesNotThrow(() -> support.assertPasswordResetOtpPatchAllowed(form, command));
    }

    private static void setPasswordResetOtpAuthentication() {
        final RydenUserDetails principal = RydenUserDetails.builder()
                .userId(7L)
                .email("user@example.com")
                .forename("Ada")
                .surname("Lovelace")
                .encodedPassword("hash")
                .authorities(List.of(new SimpleGrantedAuthority(RydenAuthorities.PASSWORD_RESET_OTP)))
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, "123456", principal.getAuthorities()));
    }
}
