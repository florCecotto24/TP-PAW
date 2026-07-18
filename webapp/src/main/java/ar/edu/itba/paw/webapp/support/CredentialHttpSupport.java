package ar.edu.itba.paw.webapp.support;

import java.util.Locale;

import javax.ws.rs.core.Response;

import org.springframework.stereotype.Component;

import ar.edu.itba.paw.services.user.EmailVerificationService;
import ar.edu.itba.paw.services.user.PasswordResetService;
import ar.edu.itba.paw.webapp.form.user.ForgotPasswordRequestForm;
import ar.edu.itba.paw.webapp.validation.ValidationGroups;

/**
 * HTTP binding for credential OTP flows: {@code POST /credentials} (password-reset) and
 * {@code POST /users/{id}/credentials} (email-verification re-issue).
 */
@Component
public final class CredentialHttpSupport {

    private final PasswordResetService passwordResetService;
    private final EmailVerificationService emailVerificationService;
    private final FormValidationSupport formValidationSupport;

    public CredentialHttpSupport(
            final PasswordResetService passwordResetService,
            final EmailVerificationService emailVerificationService,
            final FormValidationSupport formValidationSupport) {
        this.passwordResetService = passwordResetService;
        this.emailVerificationService = emailVerificationService;
        this.formValidationSupport = formValidationSupport;
    }

    public Response requestPasswordReset(final ForgotPasswordRequestForm form, final Locale locale) {
        formValidationSupport.validate(form, ValidationGroups.OnForgotPasswordRequest.class);
        passwordResetService.initiatePasswordReset(form.getEmail(), locale);
        return Response.ok().build();
    }

    /**
     * Re-issues the email-verification OTP. Anti-enumeration: always {@code 200 OK} with empty body.
     */
    public Response issueEmailVerificationCredential(final long userId, final Locale locale) {
        emailVerificationService.issuePublicVerificationCode(userId, locale);
        return Response.ok().build();
    }
}
