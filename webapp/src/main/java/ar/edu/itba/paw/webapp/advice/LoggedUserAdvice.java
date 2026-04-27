package ar.edu.itba.paw.webapp.advice;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import ar.edu.itba.paw.webapp.util.WebAuthUtils;

/**
 * Exposes the session user in the model of all MVC requests under {@code currentUser}
 * (or {@code null} if there is no authentication or it is anonymous).
 */
@ControllerAdvice
public class LoggedUserAdvice {

    /** Attribute name for {@link #addCurrentUser(Model)}. */
    public static final String CURRENT_USER_MODEL_KEY = "currentUser";

    @ModelAttribute
    public void addCurrentUser(final Model model) {
        WebAuthUtils.viewerUser(SecurityContextHolder.getContext().getAuthentication())
                .ifPresent(user -> model.addAttribute(CURRENT_USER_MODEL_KEY, user));
    }
}
