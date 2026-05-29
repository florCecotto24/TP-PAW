package ar.edu.itba.paw.webapp.controller;

import javax.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import ar.edu.itba.paw.models.domain.User;
import ar.edu.itba.paw.webapp.support.CurrentUser;
import ar.edu.itba.paw.webapp.util.WebAuthUtils;

/** Spring Security login form; redirects authenticated users away from guest-only entry. */
@Controller
public final class LoginController {

    @GetMapping("/login")
    public String loginForm(
            final HttpServletRequest request,
            @CurrentUser final User currentUser,
            @RequestParam(value = "error", required = false) final String error) {
        if (WebAuthUtils.isSignedIn(currentUser)) {
            return "redirect:" + WebAuthUtils.guestOnlyPageRedirectTarget(request, "/login");
        }
        if ("emailNotValidated".equals(error)) {
            return "redirect:/verify-email?fromLogin=1";
        }
        return "auth/login";
    }
}
