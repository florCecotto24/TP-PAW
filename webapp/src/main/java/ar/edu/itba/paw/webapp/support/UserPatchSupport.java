package ar.edu.itba.paw.webapp.support;

import java.time.LocalDate;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

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
 * Maps {@link UserPatchForm} to {@link UserPatchCommand} and applies password OTP / self-change
 * branches so {@code UserController} stays load → authorize → service → response.
 */
@Component
public final class UserPatchSupport {

    private final UserService userService;
    private final PasswordResetService passwordResetService;
    private final FormValidationSupport formValidationSupport;
    private final UserResourceAccess userResourceAccess;

    public UserPatchSupport(
            final UserService userService,
            final PasswordResetService passwordResetService,
            final FormValidationSupport formValidationSupport,
            final UserResourceAccess userResourceAccess) {
        this.userService = userService;
        this.passwordResetService = passwordResetService;
        this.formValidationSupport = formValidationSupport;
        this.userResourceAccess = userResourceAccess;
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
