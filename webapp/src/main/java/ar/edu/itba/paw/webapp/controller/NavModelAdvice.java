package ar.edu.itba.paw.webapp.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import ar.edu.itba.paw.services.UserService;
import ar.edu.itba.paw.webapp.security.RydenUserDetails;

@ControllerAdvice
public class NavModelAdvice {

    private final UserService userService;

    @Autowired
    public NavModelAdvice(final UserService userService) {
        this.userService = userService;
    }

    @ModelAttribute
    public void addNavUserAttributes(final Authentication authentication, final Model model) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof RydenUserDetails)) {
            return;
        }
        final RydenUserDetails details = (RydenUserDetails) authentication.getPrincipal();
        model.addAttribute("navUserForename", details.getForename());
        model.addAttribute("navUserSurname", details.getSurname());
        userService.getUserById(details.getUserId())
                .flatMap(u -> u.getProfilePictureId())
                .ifPresent(id -> model.addAttribute("navProfilePictureImageId", id));
    }
}
