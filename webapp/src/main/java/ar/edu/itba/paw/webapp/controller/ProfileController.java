package ar.edu.itba.paw.webapp.controller;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import ar.edu.itba.paw.exception.RydenException;
import ar.edu.itba.paw.models.AvailabilityPeriod;
import ar.edu.itba.paw.models.UserValidationPolicy;
import ar.edu.itba.paw.services.ImageService;
import ar.edu.itba.paw.services.UserService;
import ar.edu.itba.paw.webapp.form.ProfilePasswordChangeForm;
import ar.edu.itba.paw.webapp.form.ProfileUpdateForm;
import ar.edu.itba.paw.webapp.security.RydenUserDetails;
import ar.edu.itba.paw.webapp.util.LocaleMessages;
import ar.edu.itba.paw.webapp.util.MultipartImageValidation;
import ar.edu.itba.paw.webapp.util.WebAuthUtils;

@Controller
@RequestMapping("/profile")
public class ProfileController {

    private final UserService userService;
    private final LocaleMessages localeMessages;
    private final ImageService imageService;
    private final MultipartImageValidation multipartImageValidation;
    private final UserValidationPolicy userValidationPolicy;
    private final SecurityContextRepository securityContextRepository;

    @Autowired
    public ProfileController(
            final UserService userService,
            final LocaleMessages localeMessages,
            final ImageService imageService,
            final MultipartImageValidation multipartImageValidation,
            final UserValidationPolicy userValidationPolicy,
            final SecurityContextRepository securityContextRepository) {
        this.userService = userService;
        this.localeMessages = localeMessages;
        this.imageService = imageService;
        this.multipartImageValidation = multipartImageValidation;
        this.userValidationPolicy = userValidationPolicy;
        this.securityContextRepository = securityContextRepository;
    }

    @ModelAttribute("profilePhoneMaxLength")
    public int profilePhoneMaxLength() {
        return userValidationPolicy.getProfilePhoneMaxLength();
    }

    @ModelAttribute("uploadMaxImageBytes")
    public long uploadMaxImageBytes() {
        return imageService.getMaxImageBytes();
    }

    @ModelAttribute("uploadMaxImageMegabytes")
    public long uploadMaxImageMegabytes() {
        return imageService.getMaxImageMegabytesRoundedUp();
    }

    @ModelAttribute("profileBirthDateMax")
    public String profileBirthDateMax() {
        return LocalDate.now(AvailabilityPeriod.WALL_ZONE).toString();
    }

    @ModelAttribute
    public void addUserBasics(final Authentication authentication, final Model model) {
        if (authentication == null || !(authentication.getPrincipal() instanceof RydenUserDetails)) {
            return;
        }
        final RydenUserDetails details = (RydenUserDetails) authentication.getPrincipal();
        model.addAttribute("userEmail", details.getUsername());
        model.addAttribute("userForename", details.getForename());
        model.addAttribute("userSurname", details.getSurname());
        userService.getUserById(details.getUserId()).ifPresent(u -> u.getProfilePictureId()
                .ifPresent(id -> model.addAttribute("profilePictureImageId", id)));
    }

    @GetMapping
    public String profileGet(
            final Authentication authentication,
            @ModelAttribute("profileForm") final ProfileUpdateForm profileForm) {
        final RydenUserDetails details = WebAuthUtils.requireCurrentUser(authentication);
        populateFormFromUser(details.getUserId(), profileForm);
        return "profile";
    }

    @PostMapping
    public String profilePost(
            final Authentication authentication,
            @Valid @ModelAttribute("profileForm") final ProfileUpdateForm profileForm,
            final BindingResult bindingResult,
            final HttpServletRequest request,
            final HttpServletResponse response,
            final RedirectAttributes redirectAttributes) {
        final RydenUserDetails details = WebAuthUtils.requireCurrentUser(authentication);
        final LocalDate birthParsed = parseAndValidateBirthDate(profileForm.getBirthDate(), bindingResult);
        if (bindingResult.hasErrors()) {
            return "profile";
        }
        try {
            userService.updateDisplayName(details.getUserId(), profileForm.getForename(), profileForm.getSurname());
            userService.updatePhoneNumber(details.getUserId(), profileForm.getPhoneNumber());
            userService.updateBirthDate(details.getUserId(), birthParsed);
        } catch (final RydenException e) {
            bindingResult.reject("profile.update.failed", localeMessages.msg(e));
            return "profile";
        }
        refreshPrincipalDisplayName(authentication, profileForm.getForename().trim(), profileForm.getSurname().trim(), request, response);
        redirectAttributes.addFlashAttribute("profileSaved", Boolean.TRUE);
        return "redirect:/profile";
    }

    @PostMapping("/picture")
    public String uploadProfilePicture(
            final Authentication authentication,
            @RequestParam("profilePicture") final MultipartFile file,
            final RedirectAttributes redirectAttributes) {
        final RydenUserDetails details = WebAuthUtils.requireCurrentUser(authentication);
        if (file == null || file.isEmpty()) {
            redirectAttributes.addFlashAttribute("profilePictureErrorCode", "profile.picture.required");
            return "redirect:/profile";
        }
        final var imageIssue = multipartImageValidation.validateNonEmptyFile(file);
        if (imageIssue.isPresent()) {
            final var issue = imageIssue.get();
            if (issue.getResolvedMessage() != null) {
                redirectAttributes.addFlashAttribute("profilePictureErrorMessage", issue.getResolvedMessage());
            }
            if (issue.getMessageCode() != null) {
                redirectAttributes.addFlashAttribute("profilePictureErrorCode", issue.getMessageCode());
            }
            return "redirect:/profile";
        }
        try {
            userService.updateProfilePicture(
                    details.getUserId(),
                    file.getOriginalFilename(),
                    file.getContentType(),
                    file.getBytes());
        } catch (final RydenException e) {
            redirectAttributes.addFlashAttribute("profilePictureErrorMessage", localeMessages.msg(e));
            return "redirect:/profile";
        } catch (final IOException e) {
            redirectAttributes.addFlashAttribute("profilePictureErrorCode", "profile.picture.readFailed");
            return "redirect:/profile";
        }
        redirectAttributes.addFlashAttribute("profilePictureSaved", Boolean.TRUE);
        return "redirect:/profile";
    }

    @PostMapping("/picture/delete")
    public String deleteProfilePicture(
            final Authentication authentication,
            final RedirectAttributes redirectAttributes) {
        final RydenUserDetails details = WebAuthUtils.requireCurrentUser(authentication);
        try {
            userService.clearProfilePicture(details.getUserId());
        } catch (final RydenException e) {
            redirectAttributes.addFlashAttribute("profilePictureErrorMessage", localeMessages.msg(e));
            return "redirect:/profile";
        }
        redirectAttributes.addFlashAttribute("profilePictureDeleted", Boolean.TRUE);
        return "redirect:/profile";
    }

    @GetMapping("/password")
    public String passwordFormGet(final Authentication authentication, final Model model) {
        WebAuthUtils.requireCurrentUser(authentication);
        model.addAttribute("profilePasswordForm", new ProfilePasswordChangeForm());
        return "profile-password";
    }

    @PostMapping("/password")
    public String passwordFormPost(
            final Authentication authentication,
            @Valid @ModelAttribute("profilePasswordForm") final ProfilePasswordChangeForm profilePasswordForm,
            final BindingResult bindingResult,
            final RedirectAttributes redirectAttributes) {
        WebAuthUtils.requireCurrentUser(authentication);
        if (bindingResult.hasErrors()) {
            return "profile-password";
        }
        final RydenUserDetails details = WebAuthUtils.requireCurrentUser(authentication);
        try {
            userService.changePassword(
                    details.getUserId(),
                    profilePasswordForm.getCurrentPassword(),
                    profilePasswordForm.getPassword(),
                    profilePasswordForm.getPasswordConfirm());
        } catch (final RydenException e) {
            bindingResult.reject("profile.password.failed", localeMessages.msg(e));
            return "profile-password";
        }
        redirectAttributes.addFlashAttribute("profilePasswordSaved", Boolean.TRUE);
        return "redirect:/profile";
    }

    private void populateFormFromUser(final long userId, final ProfileUpdateForm form) {
        userService.getUserById(userId).ifPresent(u -> {
            form.setForename(u.getForename());
            form.setSurname(u.getSurname());
            u.getPhoneNumber().ifPresent(form::setPhoneNumber);
            u.getBirthDate().ifPresent(bd -> form.setBirthDate(bd.toString()));
        });
    }

    private void refreshPrincipalDisplayName(
            final Authentication authentication,
            final String forename,
            final String surname,
            final HttpServletRequest request,
            final HttpServletResponse response) {
        if (!(authentication.getPrincipal() instanceof RydenUserDetails)) {
            return;
        }
        final RydenUserDetails old = (RydenUserDetails) authentication.getPrincipal();
        final RydenUserDetails updated = new RydenUserDetails(
                old.getUserId(),
                old.getUsername(),
                forename,
                surname,
                old.getPassword(),
                old.getAuthorities());
        final UsernamePasswordAuthenticationToken newAuth = new UsernamePasswordAuthenticationToken(
                updated,
                authentication.getCredentials(),
                updated.getAuthorities());
        newAuth.setDetails(authentication.getDetails());
        final SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(newAuth);
        SecurityContextHolder.setContext(context);
        this.securityContextRepository.saveContext(context, request, response);
    }

    private static LocalDate parseAndValidateBirthDate(final String raw, final BindingResult errors) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        final LocalDate parsed;
        try {
            parsed = LocalDate.parse(raw.trim());
        } catch (final DateTimeParseException e) {
            errors.rejectValue("birthDate", "profile.birthDate.invalid");
            return null;
        }
        final LocalDate today = LocalDate.now(AvailabilityPeriod.WALL_ZONE);
        if (parsed.isAfter(today)) {
            errors.rejectValue("birthDate", "profile.birthDate.future");
            return null;
        }
        return parsed;
    }
}
