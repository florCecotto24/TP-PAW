package ar.edu.itba.paw.webapp.security;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

@Component
public class RydenAuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    public RydenAuthenticationFailureHandler() {
        setDefaultFailureUrl("/login?error");
        setAllowSessionCreation(true);
    }

    @Override
    public void onAuthenticationFailure(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final AuthenticationException exception) throws IOException, ServletException {
        final EmailNotValidatedException emailNotValidated = findEmailNotValidated(exception);
        if (emailNotValidated != null) {
            final HttpSession session = request.getSession(true);
            final String email = emailNotValidated.getEmail();
            if (email != null && !email.isBlank()) {
                session.setAttribute(RegistrationSessionAttributes.PENDING_VERIFY_EMAIL, email.trim());
            } else {
                session.removeAttribute(RegistrationSessionAttributes.PENDING_VERIFY_EMAIL);
            }
            getRedirectStrategy().sendRedirect(request, response, "/verify-email?fromLogin=1");
            return;
        }
        if (exception instanceof RegistrationIncompleteException) {
            final HttpSession session = request.getSession(true);
            session.setAttribute(RegistrationSessionAttributes.PENDING_PASSWORD_USER_ID,
                    ((RegistrationIncompleteException) exception).getUserId());
            getRedirectStrategy().sendRedirect(request, response, "/register/password");
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
