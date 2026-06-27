package ar.edu.itba.paw.webapp.deprecated.mvc.controller.user;

import javax.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import ar.edu.itba.paw.models.domain.user.User;
import ar.edu.itba.paw.webapp.deprecated.mvc.support.CurrentUser;
import ar.edu.itba.paw.webapp.util.WebAuthUtils;

/** Spring Security login form; redirects authenticated users away from guest-only entry. */
@Controller
public final class LoginController {

    @GetMapping("/login")
    public String loginForm(
            final HttpServletRequest request,
            @CurrentUser final User currentUser,
            @RequestParam(value = "error", required = false) final String error,
            final Model model) {
        if (WebAuthUtils.isSignedIn(currentUser)) {
            return "redirect:" + WebAuthUtils.guestOnlyPageRedirectTarget(request, "/login");
        }
        if ("emailNotValidated".equals(error)) {
            return "redirect:/verify-email?fromLogin=1";
        }
        model.addAttribute("activeTab", "login");
        return "auth/login";
    }
}
