package ar.edu.itba.paw.webapp.security.auth;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import ar.edu.itba.paw.webapp.security.auth.exception.EmailNotValidatedException;
import ar.edu.itba.paw.webapp.security.auth.exception.LegacyPasswordMailedException;
import ar.edu.itba.paw.webapp.security.http.RegistrationSessionAttributes;

/**
 * Redirects failed logins to {@code /login?error} and stores session flags for verify-email or legacy-password mail flows.
 */
@Component
public final class RydenAuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    public RydenAuthenticationFailureHandler() {
        setDefaultFailureUrl("/login?error");
        setAllowSessionCreation(true);
    }

    @Override
    public void onAuthenticationFailure(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final AuthenticationException exception) throws IOException, ServletException {
        if (exception instanceof LegacyPasswordMailedException) {
            getRedirectStrategy().sendRedirect(request, response, "/login?legacyPasswordSent=1");
            return;
        }
        final EmailNotValidatedException emailNotValidated = findEmailNotValidated(exception);
        if (emailNotValidated != null) {
            final HttpSession session = request.getSession(true);
            final String em = emailNotValidated.getEmail();
            if (em != null && !em.isBlank()) {
                session.setAttribute(RegistrationSessionAttributes.PENDING_VERIFY_EMAIL, em.trim());
            } else {
                session.removeAttribute(RegistrationSessionAttributes.PENDING_VERIFY_EMAIL);
            }
            getRedirectStrategy().sendRedirect(request, response, "/verify-email?fromLogin=1");
            return;
        }
        super.onAuthenticationFailure(request, response, exception);
    }

    private static EmailNotValidatedException findEmailNotValidated(final AuthenticationException exception) {
        Throwable current = exception;
        while (current != null) {
            if (current instanceof EmailNotValidatedException) {
                return (EmailNotValidatedException) current;
            }
            current = current.getCause();
        }
        return null;
    }
}
