package ar.edu.itba.paw.webapp.support;

import java.time.LocalDate;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import ar.edu.itba.paw.exception.MessageKeys;
import ar.edu.itba.paw.exception.user.UserNotFoundException;
import ar.edu.itba.paw.models.domain.user.User;
import ar.edu.itba.paw.models.dto.profile.UserPatchCommand;
import ar.edu.itba.paw.services.user.PasswordResetService;
import ar.edu.itba.paw.services.user.UserService;
import ar.edu.itba.paw.webapp.form.user.ProfilePasswordChangeForm;
import ar.edu.itba.paw.webapp.form.user.UserPatchForm;
import ar.edu.itba.paw.webapp.security.auth.AuthenticationAuthorities;
import ar.edu.itba.paw.webapp.security.auth.userdetails.RydenUserDetails;
import ar.edu.itba.paw.webapp.validation.ValidationGroups;

/**
 * Maps {@link UserPatchForm} to {@link UserPatchCommand} and applies password OTP / profile / admin
 * branches so {@code UserController} only routes the PATCH.
 */
@Component
public final class UserPatchSupport {

    private final UserService userService;
    private final PasswordResetService passwordResetService;
    private final FormValidationSupport formValidationSupport;
    private final UserResourceAccess userResourceAccess;
    private final UserRepresentationSupport userRepresentationSupport;
    private final CurrentUserResolver currentUserResolver;

    public UserPatchSupport(
            final UserService userService,
            final PasswordResetService passwordResetService,
            final FormValidationSupport formValidationSupport,
            final UserResourceAccess userResourceAccess,
            final UserRepresentationSupport userRepresentationSupport,
            final CurrentUserResolver currentUserResolver) {
        this.userService = userService;
        this.passwordResetService = passwordResetService;
        this.formValidationSupport = formValidationSupport;
        this.userResourceAccess = userResourceAccess;
        this.userRepresentationSupport = userRepresentationSupport;
        this.currentUserResolver = currentUserResolver;
    }

    public Response apply(
            final long userId,
            final UserPatchForm patch,
            final RydenUserDetails viewer,
            final UriInfo uriInfo,
            final HttpHeaders httpHeaders) {
        final User user = userService.getUserById(userId)
                .orElseThrow(() -> new UserNotFoundException(MessageKeys.USER_ACCOUNT_NOT_FOUND));
        final UserPatchCommand command = toPatchCommand(patch);
        final boolean passwordResetOtpPatch = isPasswordResetOtpPatch();
        assertPasswordResetOtpPatchAllowed(patch, command);

        // Authorize profile/admin fields before any mutation so a password+admin body cannot
        // commit the password and then 403 on the admin facet.
        if (!passwordResetOtpPatch && (command.hasProfileFields() || command.hasAdminFields())) {
            final long actingUserId = viewer != null
                    ? viewer.getUserId()
                    : currentUserResolver.requireUserId();
            userService.assertCanPatchUser(userId, command, actingUserId);
        }

        if (passwordResetOtpPatch || patch.getPassword() != null) {
            applyPasswordPatch(user, patch, viewer);
        }
        if (!passwordResetOtpPatch && (command.hasProfileFields() || command.hasAdminFields())) {
            final long actingUserId = viewer != null
                    ? viewer.getUserId()
                    : currentUserResolver.requireUserId();
            userService.patchUser(userId, command, actingUserId);
        }

        final User updated = userService.getUserById(userId)
                .orElseThrow(() -> new UserNotFoundException(MessageKeys.USER_ACCOUNT_NOT_FOUND));
        return userRepresentationSupport.buildUserResponse(updated, uriInfo, viewer, httpHeaders);
    }

    public UserPatchCommand toPatchCommand(final UserPatchForm patch) {
        final UserPatchCommand.Builder builder = UserPatchCommand.builder();
        if (patch.getForename() != null) {
            builder.forename(patch.getForename());
        }
        if (patch.getSurname() != null) {
            builder.surname(patch.getSurname());
        }
        if (patch.getPhoneNumber() != null) {
            builder.phoneNumber(patch.getPhoneNumber());
        }
        if (patch.getBirthDate() != null) {
            builder.birthDate(toBirthDate(patch.getBirthDate()));
        }
        if (patch.getAbout() != null) {
            builder.about(patch.getAbout());
        }
        if (patch.getCbu() != null) {
            builder.cbu(patch.getCbu());
        }
        if (patch.getLatestLocale() != null) {
            builder.latestLocale(patch.getLatestLocale());
        }
        if (patch.getRole() != null) {
            builder.role(patch.getRole());
        }
        if (patch.getBlocked() != null) {
            builder.blocked(patch.getBlocked());
        }
        if (patch.getIdentityValidated() != null) {
            builder.identityValidated(patch.getIdentityValidated());
        }
        if (patch.getLicenseValidated() != null) {
            builder.licenseValidated(patch.getLicenseValidated());
        }
        return builder.build();
    }

    public boolean isPasswordResetOtpPatch() {
        return AuthenticationAuthorities.hasPasswordResetOtp(SecurityContextHolder.getContext().getAuthentication());
    }

    public void assertPasswordResetOtpPatchAllowed(
            final UserPatchForm patch,
            final UserPatchCommand command) {
        if (!isPasswordResetOtpPatch()) {
            return;
        }
        if (command.hasProfileFields() || command.hasAdminFields() || patch.getCurrentPassword() != null) {
            throw new AccessDeniedException("Password reset credentials can only change the password.");
        }
    }

    public void applyPasswordPatch(
            final User user,
            final UserPatchForm patch,
            final RydenUserDetails viewer) {
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (AuthenticationAuthorities.hasPasswordResetOtp(authentication)) {
            if (!(authentication.getPrincipal() instanceof RydenUserDetails principal)
                    || principal.getUserId() != user.getId()) {
                throw new AccessDeniedException("You do not have permission to perform this action.");
            }
            final String otp = authentication.getCredentials() != null
                    ? authentication.getCredentials().toString()
                    : "";
            passwordResetService.completePasswordReset(
                    user.getEmail(),
                    otp,
                    patch.getPassword(),
                    patch.getPasswordConfirm());
            return;
        }

        userResourceAccess.requireSelf(user.getId(), viewer);
        final ProfilePasswordChangeForm passwordForm = new ProfilePasswordChangeForm();
        passwordForm.setCurrentPassword(patch.getCurrentPassword());
        passwordForm.setPassword(patch.getPassword());
        passwordForm.setPasswordConfirm(patch.getPasswordConfirm());
        formValidationSupport.validate(passwordForm, ValidationGroups.OnProfilePassword.class);
        userService.changePassword(
                user.getId(),
                passwordForm.getCurrentPassword(),
                passwordForm.getPassword(),
                passwordForm.getPasswordConfirm());
    }

    private static LocalDate toBirthDate(final String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return LocalDate.parse(raw.trim());
    }
}
