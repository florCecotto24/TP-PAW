package ar.edu.itba.paw.webapp.support;

import java.util.Locale;

import javax.ws.rs.core.Response;

import org.springframework.stereotype.Component;

import ar.edu.itba.paw.services.user.PasswordResetService;
import ar.edu.itba.paw.webapp.form.user.ForgotPasswordRequestForm;
import ar.edu.itba.paw.webapp.validation.ValidationGroups;

/**
 * HTTP binding for {@code POST /credentials} (password-reset OTP issuance).
 */
@Component
public final class CredentialHttpSupport {

    private final PasswordResetService passwordResetService;
    private final FormValidationSupport formValidationSupport;

    public CredentialHttpSupport(
            final PasswordResetService passwordResetService,
            final FormValidationSupport formValidationSupport) {
        this.passwordResetService = passwordResetService;
        this.formValidationSupport = formValidationSupport;
    }

    public Response requestPasswordReset(final ForgotPasswordRequestForm form, final Locale locale) {
        formValidationSupport.validate(form, ValidationGroups.OnForgotPasswordRequest.class);
        passwordResetService.initiatePasswordReset(form.getEmail(), locale);
        return Response.ok().build();
    }
}
