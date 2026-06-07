package ar.edu.itba.paw.webapp.controller.user;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import ar.edu.itba.paw.exception.RydenException;
import ar.edu.itba.paw.models.domain.user.User;
import ar.edu.itba.paw.policy.UserValidationPolicy;
import ar.edu.itba.paw.services.user.PasswordResetService;
import ar.edu.itba.paw.webapp.form.user.ForgotPasswordRequestForm;
import ar.edu.itba.paw.webapp.form.user.ForgotPasswordResetForm;
import ar.edu.itba.paw.webapp.security.auth.SessionLoginService;
import ar.edu.itba.paw.webapp.security.http.ForgotPasswordSessionAttributes;
import ar.edu.itba.paw.webapp.support.CurrentUser;
import ar.edu.itba.paw.webapp.util.LocaleMessages;
import ar.edu.itba.paw.webapp.util.WebAuthUtils;
import ar.edu.itba.paw.webapp.validation.ValidationGroups;

/** Password reset: request code and complete reset with session-scoped email context. */
@Controller
@RequestMapping("/forgot-password")
public final class ForgotPasswordController {

    private final PasswordResetService passwordResetService;
    private final LocaleMessages localeMessages;
    private final UserValidationPolicy userValidationPolicy;
    private final SessionLoginService sessionLoginService;

    public ForgotPasswordController(
            final PasswordResetService passwordResetService,
            final LocaleMessages localeMessages,
            final UserValidationPolicy userValidationPolicy,
            final SessionLoginService sessionLoginService) {
        this.passwordResetService = passwordResetService;
        this.localeMessages = localeMessages;
        this.userValidationPolicy = userValidationPolicy;
        this.sessionLoginService = sessionLoginService;
    }

    @ModelAttribute("registrationPasswordMinLength")
    public int registrationPasswordMinLength() {
        return userValidationPolicy.getRegistrationPasswordMinLength();
    }

    @ModelAttribute("registrationPasswordMaxLength")
    public int registrationPasswordMaxLength() {
        return userValidationPolicy.getRegistrationPasswordMaxLength();
    }

    @ModelAttribute("registrationEmailMaxLength")
    public int registrationEmailMaxLength() {
        return userValidationPolicy.getRegistrationEmailMaxLength();
    }

    @GetMapping
    public String forgotForm(
            final HttpServletRequest request,
            @CurrentUser final User currentUser,
            final Model model) {
        if (WebAuthUtils.isSignedIn(currentUser)) {
            return "redirect:" + WebAuthUtils.guestOnlyPageRedirectTarget(request, "/forgot-password");
        }
        final ForgotPasswordRequestForm form = new ForgotPasswordRequestForm();
        final Object flashEmail = model.asMap().get("forgotEmail");
        if (flashEmail instanceof String flashEmailString) {
            form.setEmail(flashEmailString);
        }
        model.addAttribute("forgotPasswordRequestForm", form);
        return "auth/forgot-password";
    }

    @PostMapping
    public String forgotSubmit(
            final HttpServletRequest request,
            @CurrentUser final User currentUser,
            @Validated(ValidationGroups.OnForgotPasswordRequest.class)
            @ModelAttribute("forgotPasswordRequestForm") final ForgotPasswordRequestForm form,
            final BindingResult bindingResult,
            final HttpSession session,
            final RedirectAttributes redirectAttributes) {
        if (WebAuthUtils.isSignedIn(currentUser)) {
            return "redirect:" + WebAuthUtils.guestOnlyPageRedirectTarget(request, "/forgot-password");
        }
        if (bindingResult.hasErrors()) {
            return "auth/forgot-password";
        }
        final String email = form.getEmail();
        try {
            final boolean userFound = passwordResetService.initiatePasswordReset(email, LocaleContextHolder.getLocale());
            if (!userFound) {
                redirectAttributes.addFlashAttribute("forgotGenericHint", Boolean.TRUE);
                return "redirect:/forgot-password";
            }
        } catch (final RydenException e) {
            redirectAttributes.addFlashAttribute("forgotErrorMessage", localeMessages.msg(e));
            redirectAttributes.addFlashAttribute("forgotEmail", email);
            return "redirect:/forgot-password";
        }
        session.setAttribute(ForgotPasswordSessionAttributes.PENDING_RESET_EMAIL, email);
        redirectAttributes.addFlashAttribute("forgotCodeSent", Boolean.TRUE);
        return "redirect:/forgot-password/reset";
    }

    @GetMapping("/reset")
    public String resetForm(
            final HttpServletRequest request,
            @CurrentUser final User currentUser,
            final HttpSession session,
            final Model model,
            final RedirectAttributes redirectAttributes) {
        if (WebAuthUtils.isSignedIn(currentUser)) {
            return "redirect:" + WebAuthUtils.guestOnlyPageRedirectTarget(request, "/forgot-password");
        }
        if (session.getAttribute(ForgotPasswordSessionAttributes.PENDING_RESET_EMAIL) == null) {
            redirectAttributes.addFlashAttribute("forgotSessionExpired", Boolean.TRUE);
            return "redirect:/forgot-password";
        }
        model.addAttribute("forgotPasswordResetForm", new ForgotPasswordResetForm());
        return "auth/forgot-password-reset";
    }

    @PostMapping("/reset")
    public String resetSubmit(
            final HttpServletRequest request,
            final HttpServletResponse response,
            @CurrentUser final User currentUser,
            final HttpSession session,
            @Validated(ValidationGroups.OnForgotPasswordReset.class) @ModelAttribute("forgotPasswordResetForm")
            final ForgotPasswordResetForm form,
            final BindingResult bindingResult,
            final RedirectAttributes redirectAttributes) {
        if (WebAuthUtils.isSignedIn(currentUser)) {
            return "redirect:" + WebAuthUtils.guestOnlyPageRedirectTarget(request, "/forgot-password");
        }
        final Object rawEmail = session.getAttribute(ForgotPasswordSessionAttributes.PENDING_RESET_EMAIL);
        if (!(rawEmail instanceof String rawEmailString) || !StringUtils.hasText(rawEmailString)) {
            redirectAttributes.addFlashAttribute("forgotSessionExpired", Boolean.TRUE);
            return "redirect:/forgot-password";
        }
        if (bindingResult.hasErrors()) {
            return "auth/forgot-password-reset";
        }
        final String email = rawEmailString.trim();
        try {
            passwordResetService.completePasswordReset(email, form.getCode(), form.getPassword(), form.getPasswordConfirm());
        } catch (final RydenException e) {
            redirectAttributes.addFlashAttribute("forgotResetErrorMessage", localeMessages.msg(e));
            return "redirect:/forgot-password/reset";
        }
        session.removeAttribute(ForgotPasswordSessionAttributes.PENDING_RESET_EMAIL);
        sessionLoginService.signInUserAfterPasswordReset(request, response, email);
        return "redirect:/";
    }

}
