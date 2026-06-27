package ar.edu.itba.paw.webapp.controller.user;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import ar.edu.itba.paw.exception.MessageKeys;
import ar.edu.itba.paw.exception.RydenException;
import ar.edu.itba.paw.exception.user.UserNotFoundException;
import ar.edu.itba.paw.models.domain.user.User;
import ar.edu.itba.paw.services.user.EmailVerificationService;
import ar.edu.itba.paw.services.user.UserService;

/**
 * Re-issues the email-verification OTP for a pending account ({@code POST /users/{id}/credentials}).
 */
@Path("/users/{id}/credentials")
@Component
public final class UserCredentialController {

    private final UserService userService;
    private final EmailVerificationService emailVerificationService;

    @Autowired
    public UserCredentialController(
            final UserService userService,
            final EmailVerificationService emailVerificationService) {
        this.userService = userService;
        this.emailVerificationService = emailVerificationService;
    }

    @POST
    public Response issueEmailVerificationCredential(@PathParam("id") final long id) {
        final User user = userService.getUserById(id)
                .orElseThrow(() -> new UserNotFoundException(MessageKeys.USER_ACCOUNT_NOT_FOUND));
        if (Boolean.TRUE.equals(user.getEmailValidated().orElse(false))) {
            return Response.status(Response.Status.CONFLICT).build();
        }
        try {
            emailVerificationService.resendVerificationCode(
                    user.getId(), user.getEmail(), LocaleContextHolder.getLocale());
        } catch (RydenException ex) {
            return Response.status(Response.Status.CONFLICT).build();
        }
        return Response.accepted().build();
    }
}
