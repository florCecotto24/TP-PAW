package ar.edu.itba.paw.webapp.controller.user;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import ar.edu.itba.paw.webapp.support.CredentialHttpSupport;

/**
 * Re-issues the email-verification OTP for a pending account ({@code POST /users/{id}/credentials}).
 * Anti-enumeration: always {@code 200 OK} with empty body.
 * Issuance is rate-limited per email (persisted window, same store as failed OTP attempts);
 * failed Basic OTP attempts remain rate-limited separately by {@code OtpAttemptLimiter}.
 */
@Path("/users/{id}/credentials")
@Component
public final class UserCredentialController {

    private final CredentialHttpSupport credentialHttpSupport;

    @Autowired
    public UserCredentialController(final CredentialHttpSupport credentialHttpSupport) {
        this.credentialHttpSupport = credentialHttpSupport;
    }

    @POST
    public Response issueEmailVerificationCredential(@PathParam("id") final long id) {
        return credentialHttpSupport.issueEmailVerificationCredential(
                id, LocaleContextHolder.getLocale());
    }
}
