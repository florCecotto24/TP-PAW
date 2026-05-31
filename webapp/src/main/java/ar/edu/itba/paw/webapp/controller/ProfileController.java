package ar.edu.itba.paw.webapp.controller;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import ar.edu.itba.paw.exception.RydenException;
import ar.edu.itba.paw.exception.user.InvalidCbuFormatException;
import ar.edu.itba.paw.models.domain.StoredFile;
import ar.edu.itba.paw.models.domain.User;
import ar.edu.itba.paw.models.domain.UserDocumentType;
import ar.edu.itba.paw.models.dto.profile.ProfileUpdateRequest;
import ar.edu.itba.paw.models.util.time.AppTimezone;
import ar.edu.itba.paw.services.ImageService;
import ar.edu.itba.paw.services.StoredFileService;
import ar.edu.itba.paw.services.UserService;
import ar.edu.itba.paw.services.policy.ProfileDocumentUploadPolicy;
import ar.edu.itba.paw.services.policy.UserValidationPolicy;
import ar.edu.itba.paw.webapp.form.CbuUpdateForm;
import ar.edu.itba.paw.webapp.form.ProfileDocumentUploadForm;
import ar.edu.itba.paw.webapp.form.ProfilePasswordChangeForm;
import ar.edu.itba.paw.webapp.form.ProfilePictureUploadForm;
import ar.edu.itba.paw.webapp.form.ProfileUpdateForm;
import ar.edu.itba.paw.webapp.security.auth.userdetails.RydenUserDetails;
import ar.edu.itba.paw.webapp.support.CurrentUser;
import ar.edu.itba.paw.webapp.support.facade.UserBookingDocumentsFacade;
import ar.edu.itba.paw.webapp.util.LocaleMessages;
import ar.edu.itba.paw.webapp.util.WebAuthUtils;
import ar.edu.itba.paw.webapp.validation.ValidationGroups;
import ar.edu.itba.paw.webapp.validation.support.MultipartImageValidation;

/** Signed-in profile: display name, locale, CBU, password, picture, and identity or license documents. */
@Controller
@RequestMapping("/profile")
public final class ProfileController {

    private static final Logger LOG = LoggerFactory.getLogger(ProfileController.class);

    private final UserService userService;
    private final LocaleMessages localeMessages;
    private final ImageService imageService;
    private final MultipartImageValidation multipartImageValidation;
    private final UserValidationPolicy userValidationPolicy;
    private final ProfileDocumentUploadPolicy profileDocumentUploadPolicy;
    private final StoredFileService storedFileService;
    private final SecurityContextRepository securityContextRepository;
    private final UserBookingDocumentsFacade userBookingDocumentsFacade;

    public ProfileController(
            final UserService userService,
            final LocaleMessages localeMessages,
            final ImageService imageService,
            final MultipartImageValidation multipartImageValidation,
            final UserValidationPolicy userValidationPolicy,
            final ProfileDocumentUploadPolicy profileDocumentUploadPolicy,
            final StoredFileService storedFileService,
            final SecurityContextRepository securityContextRepository,
            final UserBookingDocumentsFacade userBookingDocumentsFacade) {
        this.userService = userService;
        this.localeMessages = localeMessages;
        this.imageService = imageService;
        this.multipartImageValidation = multipartImageValidation;
        this.userValidationPolicy = userValidationPolicy;
        this.profileDocumentUploadPolicy = profileDocumentUploadPolicy;
        this.storedFileService = storedFileService;
        this.securityContextRepository = securityContextRepository;
        this.userBookingDocumentsFacade = userBookingDocumentsFacade;
    }

    @ModelAttribute("profilePhoneMaxLength")
    public int profilePhoneMaxLength() {
        return userValidationPolicy.getProfilePhoneMaxLength();
    }

    @ModelAttribute("profileAboutMaxLength")
    public int profileAboutMaxLength() {
        return userValidationPolicy.getProfileAboutMaxLength();
    }

    @ModelAttribute("profileDisplayNamePartMaxLength")
    public int profileDisplayNamePartMaxLength() {
        return userValidationPolicy.getDisplayNamePartMaxLength();
    }

    @ModelAttribute("registrationPasswordMinLength")
    public int registrationPasswordMinLength() {
        return userValidationPolicy.getRegistrationPasswordMinLength();
    }

    @ModelAttribute("registrationPasswordMaxLength")
    public int registrationPasswordMaxLength() {
        return userValidationPolicy.getRegistrationPasswordMaxLength();
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
        return LocalDate.now(AppTimezone.WALL_ZONE).toString();
    }

    @ModelAttribute("uploadMaxProfileDocumentBytes")
    public int uploadMaxProfileDocumentBytes() {
        return profileDocumentUploadPolicy.getMaxBytes();
    }

    @ModelAttribute("uploadMaxProfileDocumentMegabytes")
    public int uploadMaxProfileDocumentMegabytes() {
        return profileDocumentUploadPolicy.getMaxMegabytesRoundedUp();
    }

    @ModelAttribute
    public void addUserBasics(
            @CurrentUser final User currentUser,
            final Model model) {
        if (currentUser == null) {
            return;
        }
        model.addAttribute("userEmail", currentUser.getEmail());
        model.addAttribute("userForename", currentUser.getForename());
        model.addAttribute("userSurname", currentUser.getSurname());
        userService.getUserById(currentUser.getId()).ifPresent(u -> {
            u.getProfilePictureId().ifPresent(id -> model.addAttribute("profilePictureImageId", id));
            u.getMemberSince().ifPresent(ms -> {
                final String display = ms.format(
                        DateTimeFormatter.ofPattern("LLLL uuuu").withLocale(LocaleContextHolder.getLocale()));
                model.addAttribute("profileMemberSinceDisplay", display);
            });
            model.addAttribute("licenseValidated", u.isLicenseValidated());
            model.addAttribute("identityValidated", u.isIdentityValidated());
            u.getLicenseFileId()
                    .flatMap(storedFileService::findById)
                    .ifPresent(sf -> model.addAttribute("licenseFileName", sf.getFileName()));
            u.getIdentityFileId()
                    .flatMap(storedFileService::findById)
                    .ifPresent(sf -> model.addAttribute("identityFileName", sf.getFileName()));
        });
    }

    @GetMapping
    public String profileGet(
            @CurrentUser final User currentUser,
            @ModelAttribute("profileForm") final ProfileUpdateForm profileForm,
            final Model model) {
        final User me = WebAuthUtils.requireUser(currentUser);
        populateFormFromUser(me.getId(), profileForm);
        final LocalDate bd = profileForm.getBirthDate();
        if (bd != null) {
            final Locale locale = LocaleContextHolder.getLocale();
            final String pattern = locale.getLanguage().equals("es") ? "dd/MM/yyyy" : "MM/dd/yyyy";
            model.addAttribute("profileBirthDateDisplay",
                    bd.format(DateTimeFormatter.ofPattern(pattern)));
        } else {
            model.addAttribute("profileBirthDateDisplay", localeMessages.msg("common.notSpecified"));
        }
        return "profile/profile";
    }

    @PostMapping
    public String profilePost(
            @CurrentUser final User currentUser,
            @Validated(ValidationGroups.OnProfileUpdate.class) @ModelAttribute("profileForm") final ProfileUpdateForm profileForm,
            final BindingResult bindingResult,
            final HttpServletRequest request,
            final HttpServletResponse response,
            final RedirectAttributes redirectAttributes) {
        final User me = WebAuthUtils.requireUser(currentUser);
        if (bindingResult.hasErrors()) {
            return "profile/profile";
        }
        try {
            userService.updateProfile(me.getId(), ProfileUpdateRequest.builder()
                    .forename(profileForm.getForename())
                    .surname(profileForm.getSurname())
                    .phoneNumberRaw(profileForm.getPhoneNumber())
                    .birthDate(profileForm.getBirthDate())
                    .aboutRaw(profileForm.getAbout())
                    .cbuRaw(profileForm.getCbu())
                    .build());
        } catch (final RydenException e) {
            if (e instanceof InvalidCbuFormatException ic) {
                bindingResult.rejectValue("cbu", ic.getMessageCode(), ic.getMessageArgs(), null);
                return "profile/profile";
            }
            bindingResult.reject("profile.update.failed", localeMessages.msg(e));
            return "profile/profile";
        }
        refreshPrincipalDisplayName(profileForm.getForename().trim(), profileForm.getSurname().trim(), request, response);
        redirectAttributes.addFlashAttribute("profileSaved", Boolean.TRUE);
        return "redirect:/profile";
    }

    @PostMapping("/picture")
    public String uploadProfilePicture(
            @CurrentUser final User currentUser,
            @Validated @ModelAttribute final ProfilePictureUploadForm profilePictureUploadForm,
            final BindingResult bindingResult,
            final RedirectAttributes redirectAttributes) {
        final User me = WebAuthUtils.requireUser(currentUser);
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("profilePictureErrorCode", "profile.picture.required");
            return "redirect:/profile";
        }
        final MultipartFile file = profilePictureUploadForm.getProfilePicture();
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
                    me.getId(),
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
            @CurrentUser final User currentUser,
            final RedirectAttributes redirectAttributes) {
        final User me = WebAuthUtils.requireUser(currentUser);
        try {
            userService.clearProfilePicture(me.getId());
        } catch (final RydenException e) {
            redirectAttributes.addFlashAttribute("profilePictureErrorMessage", localeMessages.msg(e));
            return "redirect:/profile";
        }
        redirectAttributes.addFlashAttribute("profilePictureDeleted", Boolean.TRUE);
        return "redirect:/profile";
    }

    @PostMapping("/document")
    public String uploadProfileDocument(
            @CurrentUser final User currentUser,
            @Validated @ModelAttribute final ProfileDocumentUploadForm profileDocumentUploadForm,
            final BindingResult bindingResult,
            final RedirectAttributes redirectAttributes) {
        final User me = WebAuthUtils.requireUser(currentUser);
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("profileDocumentError", localeMessages.msg("profile.document.invalid"));
            return "redirect:/profile";
        }
        final var outcome = userBookingDocumentsFacade.attemptUpload(
                me.getId(),
                profileDocumentUploadForm.getDocumentType(),
                profileDocumentUploadForm.getDocumentFile(),
                false);
        applyProfileDocumentUploadFlash(outcome, redirectAttributes);
        return "redirect:/profile";
    }

    @PostMapping("/documents")
    public String uploadProfileDocuments(
            @CurrentUser final User currentUser,
            @RequestParam(name = "licenseFile", required = false) final MultipartFile licenseFile,
            @RequestParam(name = "identityFile", required = false) final MultipartFile identityFile,
            final RedirectAttributes redirectAttributes) {
        final User me = WebAuthUtils.requireUser(currentUser);
        final var pair = userBookingDocumentsFacade.attemptBookingPair(me.getId(), licenseFile, identityFile, false);
        if (pair.nothingAttempted()) {
            redirectAttributes.addFlashAttribute("profileDocumentError", localeMessages.msg("profile.document.invalid"));
            return "redirect:/profile";
        }
        final var firstError = pair.getFirstHardError();
        if (firstError.isPresent()) {
            applyProfileDocumentUploadFlash(firstError.get(), redirectAttributes);
            return "redirect:/profile";
        }
        redirectAttributes.addFlashAttribute("profileDocumentSaved", Boolean.TRUE);
        return "redirect:/profile";
    }

    private void applyProfileDocumentUploadFlash(
            final UserBookingDocumentsFacade.UploadOutcome outcome,
            final RedirectAttributes redirectAttributes) {
        switch (outcome.getStatus()) {
            case OK, SKIPPED_ALREADY_PRESENT ->
                    redirectAttributes.addFlashAttribute("profileDocumentSaved", Boolean.TRUE);
            case BUSINESS_ERROR ->
                    redirectAttributes.addFlashAttribute("profileDocumentError",
                            outcome.getLocalizedBusinessMessage().orElseGet(
                                    () -> localeMessages.msg("profile.document.invalid")));
            case READ_ERROR ->
                    redirectAttributes.addFlashAttribute("profileDocumentError",
                            localeMessages.msg("profile.document.readFailed"));
            case SKIPPED_EMPTY ->
                    redirectAttributes.addFlashAttribute("profileDocumentError",
                            localeMessages.msg("profile.document.invalid"));
        }
    }

    @PostMapping("/document/delete")
    public String deleteProfileDocument(
            @CurrentUser final User currentUser,
            @RequestParam(name = "documentType", required = false) final UserDocumentType documentType,
            final RedirectAttributes redirectAttributes) {
        final User me = WebAuthUtils.requireUser(currentUser);
        if (documentType == null) {
            redirectAttributes.addFlashAttribute("profileDocumentError", localeMessages.msg("profile.document.invalid"));
            return "redirect:/profile";
        }
        try {
            userService.clearProfileDocument(me.getId(), documentType);
            redirectAttributes.addFlashAttribute("profileDocumentDeleted", Boolean.TRUE);
        } catch (final RydenException e) {
            redirectAttributes.addFlashAttribute("profileDocumentError", localeMessages.msg(e));
        }
        return "redirect:/profile";
    }

    @GetMapping("/document")
    public ResponseEntity<byte[]> downloadProfileDocument(
            @CurrentUser final User currentUser,
            @RequestParam(name = "documentType", required = false) final UserDocumentType documentType) {
        final User me = WebAuthUtils.requireUser(currentUser);
        if (documentType == null) {
            return ResponseEntity.notFound().build();
        }
        final var storedOpt = findProfileDocument(me.getId(), documentType);
        if (storedOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        final StoredFile sf = storedOpt.get();
        MediaType contentType = MediaType.APPLICATION_OCTET_STREAM;
        try {
            if (sf.getContentType() != null && !sf.getContentType().isBlank()) {
                contentType = MediaType.parseMediaType(sf.getContentType());
            }
        } catch (final IllegalArgumentException e) {
            LOG.atDebug()
                    .setMessage("Invalid stored Content-Type for profile document userId={} [{}]")
                    .addArgument(me.getId())
                    .addArgument(sf.getContentType())
                    .setCause(e)
                    .log();
            contentType = MediaType.APPLICATION_OCTET_STREAM;
        }
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(contentType);
        headers.setContentLength(sf.getData().length);
        return new ResponseEntity<>(sf.getData(), headers, HttpStatus.OK);
    }

    @GetMapping("/document/view")
    public String viewProfileDocument(
            @CurrentUser final User currentUser,
            @RequestParam(name = "documentType", required = false) final UserDocumentType documentType,
            final Model model) {
        final User me = WebAuthUtils.requireUser(currentUser);
        if (documentType == null) {
            return "redirect:/profile";
        }
        final var storedOpt = findProfileDocument(me.getId(), documentType);
        if (storedOpt.isEmpty()) {
            return "redirect:/profile";
        }
        model.addAttribute("documentFileName", storedOpt.get().getFileName());
        model.addAttribute("documentType", documentType.name());
        return "profile/profile-document-view";
    }

    @GetMapping("/password")
    public String passwordFormGet(
            @CurrentUser final User currentUser,
            final Model model) {
        WebAuthUtils.requireUser(currentUser);
        model.addAttribute("profilePasswordForm", new ProfilePasswordChangeForm());
        return "profile/profile-password";
    }

    @PostMapping("/password")
    public String passwordFormPost(
            @CurrentUser final User currentUser,
            @Validated(ValidationGroups.OnProfilePassword.class) @ModelAttribute("profilePasswordForm")
            final ProfilePasswordChangeForm profilePasswordForm,
            final BindingResult bindingResult,
            final RedirectAttributes redirectAttributes) {
        final User me = WebAuthUtils.requireUser(currentUser);
        if (bindingResult.hasErrors()) {
            return "profile/profile-password";
        }
        try {
            userService.changePassword(
                    me.getId(),
                    profilePasswordForm.getCurrentPassword(),
                    profilePasswordForm.getPassword(),
                    profilePasswordForm.getPasswordConfirm());
        } catch (final RydenException e) {
            bindingResult.reject("profile.password.failed", localeMessages.msg(e));
            return "profile/profile-password";
        }
        redirectAttributes.addFlashAttribute("profilePasswordSaved", Boolean.TRUE);
        return "redirect:/profile";
    }

    /**
     * Single AJAX endpoint to persist the user's CBU without a full page reload. Used by the
     * "missing CBU" modals in the publish-car prerequisites page and the create-listing form;
     * previously each flow duplicated this handler. Format validation is handled declaratively by
     * {@link ar.edu.itba.paw.webapp.validation.constraint.ValidCbu} on {@link CbuUpdateForm#getCbu()}.
     */
    @PostMapping(value = "/cbu", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<Void> updateCbuAjax(
            @CurrentUser final User currentUser,
            @Validated @ModelAttribute final CbuUpdateForm cbuUpdateForm,
            final BindingResult bindingResult) {
        WebAuthUtils.requireUser(currentUser);
        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest().build();
        }
        userService.updateCbu(currentUser.getId(), cbuUpdateForm.getCbu().trim());
        return ResponseEntity.noContent().build();
    }

    private void populateFormFromUser(final long userId, final ProfileUpdateForm form) {
        userService.getUserById(userId).ifPresent(u -> {
            form.setForename(u.getForename());
            form.setSurname(u.getSurname());
            u.getPhoneNumber().ifPresent(form::setPhoneNumber);
            u.getBirthDate().ifPresent(form::setBirthDate);
            u.getAbout().ifPresent(form::setAbout);
            u.getCbu().ifPresent(form::setCbu);
        });
    }

    private void refreshPrincipalDisplayName(
            final String forename,
            final String surname,
            final HttpServletRequest request,
            final HttpServletResponse response) {
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof RydenUserDetails old)) {
            return;
        }
        final RydenUserDetails updated = new RydenUserDetails(
                old.getUserId(),
                old.getUsername(),
                forename,
                surname,
                old.getPassword(),
                old.getAuthorities(),
                old.getRoleAssignedBy().orElse(null));
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

    private java.util.Optional<StoredFile> findProfileDocument(final long userId, final UserDocumentType documentType) {
        final var userOpt = userService.getUserById(userId);
        if (userOpt.isEmpty()) {
            return java.util.Optional.empty();
        }
        final var user = userOpt.get();
        final var fileId = switch (documentType) {
            case LICENSE -> user.getLicenseFileId();
            case IDENTITY -> user.getIdentityFileId();
        };
        return fileId.flatMap(storedFileService::findById);
    }

}
