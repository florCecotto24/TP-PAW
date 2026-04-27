package ar.edu.itba.paw.webapp.controller;

import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import ar.edu.itba.paw.exception.RydenException;
import ar.edu.itba.paw.exception.user.EmailAlreadyExistsException;
import ar.edu.itba.paw.models.domain.User;
import ar.edu.itba.paw.services.policy.UserValidationPolicy;
import ar.edu.itba.paw.services.EmailVerificationService;
import ar.edu.itba.paw.services.UserService;
import ar.edu.itba.paw.webapp.support.CurrentUser;
import ar.edu.itba.paw.webapp.form.RegistrationAccountForm;
import ar.edu.itba.paw.webapp.validation.ValidationGroups;
import ar.edu.itba.paw.webapp.security.RegistrationSessionAttributes;
import ar.edu.itba.paw.webapp.security.SessionLoginService;
import ar.edu.itba.paw.webapp.util.LocaleMessages;
import ar.edu.itba.paw.webapp.util.WebAuthUtils;

@Controller
public class RegistrationController {

    private final UserService userService;
    private final EmailVerificationService emailVerificationService;
    private final LocaleMessages localeMessages;
    private final UserValidationPolicy userValidationPolicy;
    private final SessionLoginService sessionLoginService;
    private final RequestCache requestCache;

    @Autowired
    public RegistrationController(
            final UserService userService,
            final EmailVerificationService emailVerificationService,
            final LocaleMessages localeMessages,
            final UserValidationPolicy userValidationPolicy,
            final SessionLoginService sessionLoginService,
            final RequestCache requestCache) {
        this.userService = userService;
        this.emailVerificationService = emailVerificationService;
        this.localeMessages = localeMessages;
        this.userValidationPolicy = userValidationPolicy;
        this.sessionLoginService = sessionLoginService;
        this.requestCache = requestCache;
    }

    @ModelAttribute("registrationPasswordMinLength")
    public int registrationPasswordMinLength() {
        return userValidationPolicy.getRegistrationPasswordMinLength();
    }

    @ModelAttribute("registrationPasswordMaxLength")
    public int registrationPasswordMaxLength() {
        return userValidationPolicy.getRegistrationPasswordMaxLength();
    }

    @ModelAttribute("registrationDisplayNamePartMaxLength")
    public int registrationDisplayNamePartMaxLength() {
        return userValidationPolicy.getDisplayNamePartMaxLength();
    }

    @ModelAttribute("registrationEmailMaxLength")
    public int registrationEmailMaxLength() {
        return userValidationPolicy.getRegistrationEmailMaxLength();
    }

    @GetMapping("/register/password")
    public String legacyRegisterPasswordPath() {
        return "redirect:/register";
    }

    @GetMapping("/register")
    public String registerForm(
            final HttpServletRequest request,
            @CurrentUser final User currentUser,
            final Model model) {
        if (WebAuthUtils.isSignedIn(currentUser)) {
            return "redirect:" + WebAuthUtils.guestOnlyPageRedirectTarget(request, "/register");
        }
        final RegistrationAccountForm form = new RegistrationAccountForm();
        copyRegisterFlashToForm(model, form);
        model.addAttribute("registrationAccountForm", form);
        return "register";
    }

    private static void copyRegisterFlashToForm(final Model model, final RegistrationAccountForm form) {
        final Object forename = model.getAttribute("forename");
        if (forename instanceof String && StringUtils.hasText((String) forename)) {
            form.setForename(((String) forename).trim());
        }
        final Object surname = model.getAttribute("surname");
        if (surname instanceof String && StringUtils.hasText((String) surname)) {
            form.setSurname(((String) surname).trim());
        }
        final Object mail = model.getAttribute("email");
        if (mail instanceof String && StringUtils.hasText((String) mail)) {
            form.setEmail(((String) mail).trim());
        }
    }

    @PostMapping("/register")
    public String registerSubmit(
            final HttpServletRequest request,
            @CurrentUser final User currentUser,
            @Validated(ValidationGroups.OnRegistration.class) @ModelAttribute("registrationAccountForm")
            final RegistrationAccountForm registrationAccountForm,
            final BindingResult bindingResult,
            final RedirectAttributes redirectAttributes) {
        if (WebAuthUtils.isSignedIn(currentUser)) {
            return "redirect:" + WebAuthUtils.guestOnlyPageRedirectTarget(request, "/register");
        }
        if (bindingResult.hasErrors()) {
            return "register";
        }
        try {
            final User created = userService.registerUser(
                    registrationAccountForm.getEmail(),
                    registrationAccountForm.getForename(),
                    registrationAccountForm.getSurname(),
                    registrationAccountForm.getPassword(),
                    registrationAccountForm.getPasswordConfirm());
            emailVerificationService.issueFreshVerificationCode(
                    created.getId(), created.getEmail(), LocaleContextHolder.getLocale());
            redirectAttributes.addFlashAttribute("verifyEmailHint", created.getEmail());
            return "redirect:/verify-email";
        } catch (final EmailAlreadyExistsException e) {
            redirectAttributes.addFlashAttribute("registerEmailTaken", Boolean.TRUE);
            redirectAttributes.addFlashAttribute("forename", registrationAccountForm.getForename());
            redirectAttributes.addFlashAttribute("surname", registrationAccountForm.getSurname());
            redirectAttributes.addFlashAttribute("email", registrationAccountForm.getEmail());
            return "redirect:/register";
        } catch (final RydenException e) {
            bindingResult.reject("register.failed", localeMessages.msg(e));
            return "register";
        }
    }

    @GetMapping("/verify-email")
    public String verifyForm(
            @CurrentUser final User currentUser,
            @RequestParam(value = "email", required = false) final String email,
            @RequestParam(value = "fromLogin", required = false) final String fromLogin,
            final HttpServletRequest request,
            final Model model) {
        if (WebAuthUtils.isSignedIn(currentUser)) {
            return "redirect:" + WebAuthUtils.guestOnlyPageRedirectTarget(request, "/verify-email");
        }
        final boolean fromLoginFlow = StringUtils.hasText(fromLogin)
                && ("1".equals(fromLogin) || "true".equalsIgnoreCase(fromLogin));
        if (fromLoginFlow) {
            model.addAttribute("verifyFromLogin", Boolean.TRUE);
            final HttpSession session = request.getSession(false);
            if (session != null) {
                final Object pending = session.getAttribute(RegistrationSessionAttributes.PENDING_VERIFY_EMAIL);
                if (pending instanceof String && StringUtils.hasText((String) pending)) {
                    final String pendingEmail = ((String) pending).trim();
                    model.addAttribute("verifyEmailHint", pendingEmail);
                    userService.findByEmail(pendingEmail).ifPresent(u -> emailVerificationService.ensurePendingVerificationCode(
                            u.getId(), u.getEmail(), LocaleContextHolder.getLocale()));
                }
            }
        } else if (StringUtils.hasText(email)) {
            model.addAttribute("verifyEmailHint", email.trim());
        }
        return "verify-email";
    }

    @PostMapping("/verify-email/resend")
    public String verifyResend(
            final HttpServletRequest request,
            @CurrentUser final User currentUser,
            @RequestParam("email") final String email,
            final RedirectAttributes redirectAttributes) {
        if (WebAuthUtils.isSignedIn(currentUser)) {
            return "redirect:" + WebAuthUtils.guestOnlyPageRedirectTarget(request, "/verify-email");
        }
        if (!StringUtils.hasText(email)) {
            redirectAttributes.addFlashAttribute("verifyFieldError", Boolean.TRUE);
            return "redirect:/verify-email";
        }
        final Optional<User> userOpt = userService.findByEmail(email.trim());
        if (userOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("verifyErrorMessage", localeMessages.msg("verifyEmail.resendUnknown"));
            redirectAttributes.addFlashAttribute("verifyEmail", email.trim());
            return "redirect:/verify-email";
        }
        final User user = userOpt.get();
        try {
            emailVerificationService.resendVerificationCode(user.getId(), user.getEmail(), LocaleContextHolder.getLocale());
            redirectAttributes.addFlashAttribute("verifyResent", Boolean.TRUE);
        } catch (final RydenException e) {
            redirectAttributes.addFlashAttribute("verifyErrorMessage", localeMessages.msg(e));
        }
        redirectAttributes.addFlashAttribute("verifyEmail", email.trim());
        return "redirect:/verify-email";
    }

    @PostMapping("/verify-email")
    public String verifySubmit(
            @RequestParam("email") final String email,
            @RequestParam("code") final String code,
            final HttpServletRequest request,
            final HttpServletResponse response,
            final RedirectAttributes redirectAttributes) {
        if (!StringUtils.hasText(email) || !StringUtils.hasText(code)) {
            redirectAttributes.addFlashAttribute("verifyFieldError", Boolean.TRUE);
            redirectAttributes.addFlashAttribute("verifyEmail", email != null ? email : "");
            return "redirect:/verify-email";
        }
        try {
            final long userId = emailVerificationService.verifyEmailAndConsumeCode(email, code.trim());
            final HttpSession session = request.getSession(false);
            if (session != null) {
                session.removeAttribute(RegistrationSessionAttributes.PENDING_VERIFY_EMAIL);
            }
            sessionLoginService.signInUserAfterEmailVerification(request, response, userId);
            return resolvePostAuthRedirect(request, response);
        } catch (final RydenException e) {
            redirectAttributes.addFlashAttribute("verifyErrorMessage", localeMessages.msg(e));
            redirectAttributes.addFlashAttribute("verifyEmail", email.trim());
            return "redirect:/verify-email";
        }
    }

    private String resolvePostAuthRedirect(final HttpServletRequest request, final HttpServletResponse response) {
        final SavedRequest saved = this.requestCache.getRequest(request, response);
        if (saved != null) {
            final String target = saved.getRedirectUrl();
            this.requestCache.removeRequest(request, response);
            if (target != null && !target.isBlank()) {
                return "redirect:" + target;
            }
        }
        return "redirect:/";
    }

}
