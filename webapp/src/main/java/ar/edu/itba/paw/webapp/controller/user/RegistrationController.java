package ar.edu.itba.paw.webapp.controller.user;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import ar.edu.itba.paw.exception.RydenException;
import ar.edu.itba.paw.exception.user.EmailAlreadyExistsException;
import ar.edu.itba.paw.models.domain.user.User;
import ar.edu.itba.paw.policy.UserValidationPolicy;
import ar.edu.itba.paw.services.user.UserService;
import ar.edu.itba.paw.webapp.form.user.RegistrationAccountForm;
import ar.edu.itba.paw.webapp.form.user.VerifyEmailForm;
import ar.edu.itba.paw.webapp.security.auth.SessionLoginService;
import ar.edu.itba.paw.webapp.security.http.RegistrationSessionAttributes;
import ar.edu.itba.paw.webapp.support.CurrentUser;
import ar.edu.itba.paw.webapp.util.LocaleMessages;
import ar.edu.itba.paw.webapp.util.WebAuthUtils;
import ar.edu.itba.paw.webapp.validation.ValidationGroups;

/** Registration wizard, optional post-login redirect from saved request, and account confirmation entry. */
@Controller
public final class RegistrationController {

    private final UserService userService;
    private final LocaleMessages localeMessages;
    private final UserValidationPolicy userValidationPolicy;
    private final SessionLoginService sessionLoginService;
    private final RequestCache requestCache;

    public RegistrationController(
            final UserService userService,
            final LocaleMessages localeMessages,
            final UserValidationPolicy userValidationPolicy,
            final SessionLoginService sessionLoginService,
            final RequestCache requestCache) {
        this.userService = userService;
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
        return "auth/register";
    }

    private static void copyRegisterFlashToForm(final Model model, final RegistrationAccountForm form) {
        final Object forename = model.getAttribute("forename");
        if (forename instanceof String forenameString && StringUtils.hasText(forenameString)) {
            form.setForename(forenameString.trim());
        }
        final Object surname = model.getAttribute("surname");
        if (surname instanceof String surnameString && StringUtils.hasText(surnameString)) {
            form.setSurname(surnameString.trim());
        }
        final Object mail = model.getAttribute("email");
        if (mail instanceof String mailString && StringUtils.hasText(mailString)) {
            form.setEmail(mailString.trim());
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
            return "auth/register";
        }
        try {
            final User created = userService.registerUserRequiringAccountConfirmation(
                    registrationAccountForm.getEmail(),
                    registrationAccountForm.getForename(),
                    registrationAccountForm.getSurname(),
                    registrationAccountForm.getPassword(),
                    registrationAccountForm.getPasswordConfirm(),
                    LocaleContextHolder.getLocale());
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
            return "auth/register";
        }
    }

    @GetMapping("/verify-email")
    public String verifyForm(
            @CurrentUser final User currentUser,
            @RequestParam(value = "email", required = false) final String email,
            @RequestParam(value = "fromLogin", required = false) final Boolean fromLogin,
            final HttpServletRequest request,
            final Model model) {
        if (WebAuthUtils.isSignedIn(currentUser)) {
            return "redirect:" + WebAuthUtils.guestOnlyPageRedirectTarget(request, "/verify-email");
        }
        // Spring's StringToBooleanConverter already accepts "true"/"false"/"1"/"0"/"on"/"off"
        // (case-insensitive), which covers the legacy query-param tokens that landed here.
        if (Boolean.TRUE.equals(fromLogin)) {
            model.addAttribute("verifyFromLogin", Boolean.TRUE);
            final HttpSession session = request.getSession(false);
            if (session != null) {
                final Object pending = session.getAttribute(RegistrationSessionAttributes.PENDING_VERIFY_EMAIL);
                if (pending instanceof String pendingString && StringUtils.hasText(pendingString)) {
                    final String pendingEmail = pendingString.trim();
                    model.addAttribute("verifyEmailHint", pendingEmail);
                    userService.ensureAccountConfirmationPrerequisites(
                            pendingEmail, LocaleContextHolder.getLocale());
                }
            }
        } else if (StringUtils.hasText(email)) {
            model.addAttribute("verifyEmailHint", email.trim());
        }
        final VerifyEmailForm verifyEmailForm = new VerifyEmailForm();
        final Object flashEmail = model.asMap().get("verifyEmail");
        if (flashEmail instanceof String flashEmailString && StringUtils.hasText(flashEmailString)) {
            verifyEmailForm.setEmail(flashEmailString);
        } else {
            final Object hintEmail = model.asMap().get("verifyEmailHint");
            if (hintEmail instanceof String hintEmailString && StringUtils.hasText(hintEmailString)) {
                verifyEmailForm.setEmail(hintEmailString);
            }
        }
        model.addAttribute("verifyEmailForm", verifyEmailForm);
        return "auth/verify-email";
    }

    @PostMapping("/verify-email/resend")
    public String verifyResend(
            final HttpServletRequest request,
            @CurrentUser final User currentUser,
            @Validated(ValidationGroups.OnResendVerification.class)
            @ModelAttribute("verifyEmailForm") final VerifyEmailForm verifyEmailForm,
            final BindingResult bindingResult,
            final RedirectAttributes redirectAttributes) {
        if (WebAuthUtils.isSignedIn(currentUser)) {
            return "redirect:" + WebAuthUtils.guestOnlyPageRedirectTarget(request, "/verify-email");
        }
        if (bindingResult.hasErrors()) {
            return "auth/verify-email";
        }
        final String email = verifyEmailForm.getEmail();
        try {
            if (!userService.requestAccountConfirmationResend(email, LocaleContextHolder.getLocale())) {
                redirectAttributes.addFlashAttribute("verifyErrorMessage", localeMessages.msg("verifyEmail.resendUnknown"));
                redirectAttributes.addFlashAttribute("verifyEmail", email);
                return "redirect:/verify-email";
            }
            redirectAttributes.addFlashAttribute("verifyResent", Boolean.TRUE);
        } catch (final RydenException e) {
            redirectAttributes.addFlashAttribute("verifyErrorMessage", localeMessages.msg(e));
        }
        redirectAttributes.addFlashAttribute("verifyEmail", email);
        return "redirect:/verify-email";
    }

    @PostMapping("/verify-email")
    public String verifySubmit(
            @Validated(ValidationGroups.OnVerifyEmail.class)
            @ModelAttribute("verifyEmailForm") final VerifyEmailForm verifyEmailForm,
            final BindingResult bindingResult,
            final HttpServletRequest request,
            final HttpServletResponse response,
            final RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "auth/verify-email";
        }
        final String email = verifyEmailForm.getEmail();
        final String code = verifyEmailForm.getCode();
        try {
            final long userId = userService.completeAccountConfirmation(email, code);
            final HttpSession session = request.getSession(false);
            if (session != null) {
                session.removeAttribute(RegistrationSessionAttributes.PENDING_VERIFY_EMAIL);
            }
            sessionLoginService.signInUserAfterEmailVerification(request, response, userId);
            return resolvePostAuthRedirect(request, response);
        } catch (final RydenException e) {
            redirectAttributes.addFlashAttribute("verifyErrorMessage", localeMessages.msg(e));
            redirectAttributes.addFlashAttribute("verifyEmail", email);
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
