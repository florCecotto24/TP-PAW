package ar.edu.itba.paw.webapp.controller;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class LoginController {

    @GetMapping("/login")
    public String loginForm(
            final Authentication authentication,
            @RequestParam(value = "error", required = false) final String error) {
        if (authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)) {
            return "redirect:/";
        }
        if ("emailNotValidated".equals(error)) {
            return "redirect:/verify-email?fromLogin=1";
        }
        return "login";
    }
}
