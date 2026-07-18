package ar.edu.itba.paw.webapp.controller;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import ar.edu.itba.paw.webapp.api.common.VndMediaType;
import ar.edu.itba.paw.webapp.form.user.ForgotPasswordRequestForm;
import ar.edu.itba.paw.webapp.support.CredentialHttpSupport;

/**
 * Top-level temporary credentials ({@code POST /credentials}). HTTP routing only.
 * Issues a password-reset OTP by email (anti-enumeration: always {@code 200 OK} with empty body).
 * Issuance is intentionally not IP-rate-limited; failed Basic OTP attempts
 * are rate-limited by {@code OtpAttemptLimiter}.
 */
@Path("/credentials")
@Component
public final class CredentialController {

    private final CredentialHttpSupport credentialHttpSupport;

    @Autowired
    public CredentialController(final CredentialHttpSupport credentialHttpSupport) {
        this.credentialHttpSupport = credentialHttpSupport;
    }

    @POST
    @Consumes(VndMediaType.CREDENTIAL_V1_JSON)
    public Response requestPasswordResetCredential(final ForgotPasswordRequestForm form) {
        return credentialHttpSupport.requestPasswordReset(form, LocaleContextHolder.getLocale());
    }
}
