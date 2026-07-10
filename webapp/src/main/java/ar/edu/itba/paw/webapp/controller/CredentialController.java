package ar.edu.itba.paw.webapp.controller;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import ar.edu.itba.paw.services.user.PasswordResetService;
import ar.edu.itba.paw.webapp.api.common.VndMediaType;
import ar.edu.itba.paw.webapp.form.user.ForgotPasswordRequestForm;
import ar.edu.itba.paw.webapp.support.FormValidationSupport;
import ar.edu.itba.paw.webapp.validation.ValidationGroups;

/**
 * Top-level temporary credentials ({@code POST /credentials}).
 * Issues a password-reset OTP by email (anti-enumeration: always {@code 200 OK} with empty body).
 */
@Path("/credentials")
@Component
public final class CredentialController {

    private final PasswordResetService passwordResetService;
    private final FormValidationSupport formValidationSupport;

    @Autowired
    public CredentialController(
            final PasswordResetService passwordResetService,
            final FormValidationSupport formValidationSupport) {
        this.passwordResetService = passwordResetService;
        this.formValidationSupport = formValidationSupport;
    }

    @POST
    @Consumes(VndMediaType.CREDENTIAL_V1_JSON)
    public Response requestPasswordResetCredential(final ForgotPasswordRequestForm form) {
        formValidationSupport.validate(form, ValidationGroups.OnForgotPasswordRequest.class);
        passwordResetService.initiatePasswordReset(form.getEmail(), LocaleContextHolder.getLocale());
        return Response.ok().build();
    }
}
