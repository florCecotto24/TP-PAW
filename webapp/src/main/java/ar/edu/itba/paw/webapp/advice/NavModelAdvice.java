package ar.edu.itba.paw.webapp.advice;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import ar.edu.itba.paw.services.UserService;
import ar.edu.itba.paw.webapp.util.WebAuthUtils;

@ControllerAdvice
public final class NavModelAdvice {

    private final UserService userService;

    public NavModelAdvice(final UserService userService) {
        this.userService = userService;
    }

    @ModelAttribute
    public void addNavUserAttributes(final Model model) {
        WebAuthUtils.currentUserDetails(SecurityContextHolder.getContext().getAuthentication())
                .ifPresent(details -> {
                    model.addAttribute("navUserForename", details.getForename());
                    model.addAttribute("navUserSurname", details.getSurname());
                    userService.getUserById(details.getUserId())
                            .flatMap(u -> u.getProfilePictureId())
                            .ifPresent(id -> model.addAttribute("navProfilePictureImageId", id));
                });
    }
}
