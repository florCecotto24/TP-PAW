package ar.edu.itba.paw.webapp.controller.user;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import ar.edu.itba.paw.services.user.EmailVerificationService;

/**
 * Re-issues the email-verification OTP for a pending account ({@code POST /users/{id}/credentials}).
 * Anti-enumeration: always {@code 200 OK} with empty body.
 */
@Path("/users/{id}/credentials")
@Component
public final class UserCredentialController {

    private final EmailVerificationService emailVerificationService;

    @Autowired
    public UserCredentialController(final EmailVerificationService emailVerificationService) {
        this.emailVerificationService = emailVerificationService;
    }

    @POST
    public Response issueEmailVerificationCredential(@PathParam("id") final long id) {
        emailVerificationService.issuePublicVerificationCode(id, LocaleContextHolder.getLocale());
        return Response.ok().build();
    }
}
