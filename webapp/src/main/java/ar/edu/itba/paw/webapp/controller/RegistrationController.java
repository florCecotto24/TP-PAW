package ar.edu.itba.paw.webapp.controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import ar.edu.itba.paw.exception.RydenException;
import ar.edu.itba.paw.exception.user.EmailAlreadyExistsException;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.models.UserValidationPolicy;
import ar.edu.itba.paw.services.EmailVerificationService;
import ar.edu.itba.paw.services.UserService;
import ar.edu.itba.paw.webapp.form.RegistrationPasswordForm;
import ar.edu.itba.paw.webapp.security.RegistrationSessionAttributes;
import ar.edu.itba.paw.webapp.util.LocaleMessages;

@Controller
public class RegistrationController {

    private final UserService userService;
    private final EmailVerificationService emailVerificationService;
    private final LocaleMessages localeMessages;
    private final UserValidationPolicy userValidationPolicy;

    @Autowired
    public RegistrationController(
            final UserService userService,
            final EmailVerificationService emailVerificationService,
            final LocaleMessages localeMessages,
            final UserValidationPolicy userValidationPolicy) {
        this.userService = userService;
        this.emailVerificationService = emailVerificationService;
        this.localeMessages = localeMessages;
        this.userValidationPolicy = userValidationPolicy;
    }

    @ModelAttribute("registrationPasswordMinLength")
    public int registrationPasswordMinLength() {
        return userValidationPolicy.getRegistrationPasswordMinLength();
    }

    @GetMapping("/register")
    public String registerForm(final Authentication authentication) {
        if (isSignedIn(authentication)) {
            return "redirect:/";
        }
        return "register";
    }

    @PostMapping("/register")
    public String registerSubmit(
            @RequestParam("forename") final String forename,
            @RequestParam("surname") final String surname,
            @RequestParam("email") final String email,
            final RedirectAttributes redirectAttributes) {
        if (!StringUtils.hasText(forename) || !StringUtils.hasText(surname) || !StringUtils.hasText(email)) {
            redirectAttributes.addFlashAttribute("registerFieldError", Boolean.TRUE);
            redirectAttributes.addFlashAttribute("forename", forename != null ? forename : "");
            redirectAttributes.addFlashAttribute("surname", surname != null ? surname : "");
            redirectAttributes.addFlashAttribute("email", email != null ? email : "");
            return "redirect:/register";
        }
        try {
            final User created = userService.createUser(email, forename.trim(), surname.trim());
            emailVerificationService.issueNewCode(created.getId(), created.getEmail(), LocaleContextHolder.getLocale());
            redirectAttributes.addFlashAttribute("verifyEmailHint", created.getEmail());
            return "redirect:/verify-email";
        } catch (final EmailAlreadyExistsException e) {
            redirectAttributes.addFlashAttribute("registerEmailTaken", Boolean.TRUE);
            redirectAttributes.addFlashAttribute("forename", forename.trim());
            redirectAttributes.addFlashAttribute("surname", surname.trim());
            redirectAttributes.addFlashAttribute("email", email.trim());
            return "redirect:/register";
        }
    }

    @GetMapping("/verify-email")
    public String verifyForm(
            final Authentication authentication,
            @RequestParam(value = "email", required = false) final String email,
            @RequestParam(value = "fromLogin", required = false) final String fromLogin,
            final HttpServletRequest request,
            final Model model) {
        if (isSignedIn(authentication)) {
            return "redirect:/";
        }
        final boolean fromLoginFlow = StringUtils.hasText(fromLogin)
                && ("1".equals(fromLogin) || "true".equalsIgnoreCase(fromLogin));
        if (fromLoginFlow) {
            model.addAttribute("verifyFromLogin", Boolean.TRUE);
            final HttpSession session = request.getSession(false);
            if (session != null) {
                final Object pending = session.getAttribute(RegistrationSessionAttributes.PENDING_VERIFY_EMAIL);
                if (pending instanceof String && StringUtils.hasText((String) pending)) {
                    model.addAttribute("verifyEmailHint", ((String) pending).trim());
                    session.removeAttribute(RegistrationSessionAttributes.PENDING_VERIFY_EMAIL);
                }
            }
        } else if (StringUtils.hasText(email)) {
            model.addAttribute("verifyEmailHint", email.trim());
        }
        return "verify-email";
    }

    @PostMapping("/verify-email")
    public String verifySubmit(
            @RequestParam("email") final String email,
            @RequestParam("code") final String code,
            final HttpServletRequest request,
            final RedirectAttributes redirectAttributes) {
        if (!StringUtils.hasText(email) || !StringUtils.hasText(code)) {
            redirectAttributes.addFlashAttribute("verifyFieldError", Boolean.TRUE);
            redirectAttributes.addFlashAttribute("verifyEmail", email != null ? email : "");
            return "redirect:/verify-email";
        }
        try {
            final long userId = emailVerificationService.verifyEmailAndConsumeCode(email, code.trim());
            final HttpSession session = request.getSession(true);
            session.setAttribute(RegistrationSessionAttributes.PENDING_PASSWORD_USER_ID, userId);
            return "redirect:/register/password";
        } catch (final RydenException e) {
            redirectAttributes.addFlashAttribute("verifyErrorMessage", localeMessages.msg(e));
            redirectAttributes.addFlashAttribute("verifyEmail", email.trim());
            return "redirect:/verify-email";
        }
    }

    @GetMapping("/register/password")
    public String passwordForm(
            final HttpSession session,
            final Authentication authentication,
            @ModelAttribute("registrationPasswordForm") final RegistrationPasswordForm registrationPasswordForm) {
        if (isSignedIn(authentication)) {
            return "redirect:/";
        }
        if (session.getAttribute(RegistrationSessionAttributes.PENDING_PASSWORD_USER_ID) == null) {
            return "redirect:/verify-email";
        }
        return "register-password";
    }

    private static boolean isSignedIn(final Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }

    @PostMapping("/register/password")
    public String passwordSubmit(
            @Valid @ModelAttribute("registrationPasswordForm") final RegistrationPasswordForm registrationPasswordForm,
            final BindingResult bindingResult,
            final HttpSession session,
            final RedirectAttributes redirectAttributes) {
        final Object rawId = session.getAttribute(RegistrationSessionAttributes.PENDING_PASSWORD_USER_ID);
        if (rawId == null) {
            return "redirect:/verify-email";
        }
        if (bindingResult.hasErrors()) {
            return "register-password";
        }
        final long userId = ((Number) rawId).longValue();
        try {
            userService.setRegistrationPassword(
                    userId,
                    registrationPasswordForm.getPassword(),
                    registrationPasswordForm.getPasswordConfirm());
            session.removeAttribute(RegistrationSessionAttributes.PENDING_PASSWORD_USER_ID);
            return "redirect:/login?registrationComplete=1";
        } catch (final RydenException e) {
            redirectAttributes.addFlashAttribute("passwordErrorMessage", localeMessages.msg(e));
            return "redirect:/register/password";
        }
    }
}
