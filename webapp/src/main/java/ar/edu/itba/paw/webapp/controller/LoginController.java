package ar.edu.itba.paw.webapp.controller;

import javax.servlet.http.HttpServletRequest;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import ar.edu.itba.paw.webapp.util.WebAuthUtils;

@Controller
public class LoginController {

    @GetMapping("/login")
    public String loginForm(
            final HttpServletRequest request,
            final Authentication authentication,
            @RequestParam(value = "error", required = false) final String error) {
        if (authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)) {
            return "redirect:" + WebAuthUtils.guestOnlyPageRedirectTarget(request, "/login");
        }
        if ("emailNotValidated".equals(error)) {
            return "redirect:/verify-email?fromLogin=1";
        }
        return "login";
    }
}
