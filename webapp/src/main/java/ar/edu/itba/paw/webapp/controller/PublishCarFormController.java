package ar.edu.itba.paw.webapp.controller;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import ar.edu.itba.paw.dto.GalleryMediaUpload;
import ar.edu.itba.paw.dto.PublishCarOutcome;
import ar.edu.itba.paw.dto.PublishCarRequest;
import ar.edu.itba.paw.exception.MessageKeys;
import ar.edu.itba.paw.exception.RydenException;
import ar.edu.itba.paw.exception.car.DuplicatePlateException;
import ar.edu.itba.paw.models.domain.CarBrand;
import ar.edu.itba.paw.models.domain.CarModel;
import ar.edu.itba.paw.models.domain.User;
import ar.edu.itba.paw.models.domain.UserDocumentType;
import ar.edu.itba.paw.models.util.rules.CbuRules;
import ar.edu.itba.paw.services.CarBrandService;
import ar.edu.itba.paw.services.CarModelService;
import ar.edu.itba.paw.services.CarPublishingService;
import ar.edu.itba.paw.services.ImageService;
import ar.edu.itba.paw.services.UserService;
import ar.edu.itba.paw.services.policy.CarGalleryUploadPolicy;
import ar.edu.itba.paw.services.policy.ProfileDocumentUploadPolicy;
import ar.edu.itba.paw.webapp.dto.PublishCarRetainedImage;
import ar.edu.itba.paw.webapp.dto.PublishCarRetainedInsurance;
import ar.edu.itba.paw.webapp.form.PublishCarForm;
import ar.edu.itba.paw.webapp.support.CurrentUser;
import ar.edu.itba.paw.webapp.support.facade.UserBookingDocumentsFacade;
import ar.edu.itba.paw.webapp.util.CarEnumOptions;
import ar.edu.itba.paw.webapp.util.LocaleMessages;
import ar.edu.itba.paw.webapp.util.PublishCarInsuranceSessionStash;
import ar.edu.itba.paw.webapp.util.PublishCarPictureSessionStash;
import ar.edu.itba.paw.webapp.util.WebAuthUtils;
import ar.edu.itba.paw.webapp.validation.PublishCarFormValidator;
import ar.edu.itba.paw.webapp.validation.ValidationGroups;
import ar.edu.itba.paw.webapp.validation.support.MultipartGalleryMediaValidation;

/** Step 1 of the two-step publish flow: registers a car with pictures. */
@Controller
@RequestMapping("/publish-car")
public final class PublishCarFormController {

    private static final Logger LOG = LoggerFactory.getLogger(PublishCarFormController.class);

    private final CarPublishingService carPublishingService;
    private final CarBrandService carBrandService;
    private final CarModelService carModelService;
    private final LocaleMessages localeMessages;
    private final ImageService imageService;
    private final MultipartGalleryMediaValidation multipartGalleryMediaValidation;
    private final CarGalleryUploadPolicy carGalleryUploadPolicy;
    private final PublishCarPictureSessionStash pictureStash;
    private final PublishCarInsuranceSessionStash insuranceStash;
    private final CarEnumOptions carEnumOptions;
    private final UserService userService;
    private final ProfileDocumentUploadPolicy profileDocumentUploadPolicy;
    private final PublishCarFormValidator publishCarFormValidator;
    private final UserBookingDocumentsFacade userBookingDocumentsFacade;

    public PublishCarFormController(
            final CarPublishingService carPublishingService,
            final CarBrandService carBrandService,
            final CarModelService carModelService,
            final LocaleMessages localeMessages,
            final ImageService imageService,
            final MultipartGalleryMediaValidation multipartGalleryMediaValidation,
            final CarGalleryUploadPolicy carGalleryUploadPolicy,
            final PublishCarPictureSessionStash pictureStash,
            final PublishCarInsuranceSessionStash insuranceStash,
            final CarEnumOptions carEnumOptions,
            final UserService userService,
            final ProfileDocumentUploadPolicy profileDocumentUploadPolicy,
            final PublishCarFormValidator publishCarFormValidator,
            final UserBookingDocumentsFacade userBookingDocumentsFacade) {
        this.carPublishingService = carPublishingService;
        this.carBrandService = carBrandService;
        this.carModelService = carModelService;
        this.localeMessages = localeMessages;
        this.imageService = imageService;
        this.multipartGalleryMediaValidation = multipartGalleryMediaValidation;
        this.carGalleryUploadPolicy = carGalleryUploadPolicy;
        this.pictureStash = pictureStash;
        this.insuranceStash = insuranceStash;
        this.carEnumOptions = carEnumOptions;
        this.userService = userService;
        this.profileDocumentUploadPolicy = profileDocumentUploadPolicy;
        this.publishCarFormValidator = publishCarFormValidator;
        this.userBookingDocumentsFacade = userBookingDocumentsFacade;
    }

    @ModelAttribute("allBrands")
    public List<CarBrand> allBrands() {
        return carBrandService.findAllOrdered();
    }

    @ModelAttribute("allModels")
    public List<CarModel> allModels() {
        return carModelService.findAllOrderedGroupedByBrand();
    }

    @ModelAttribute("carTypeOptions")
    public Map<String, String> carTypeOptions() {
        return carEnumOptions.carTypeSelectOptions();
    }

    @ModelAttribute("powertrainOptions")
    public Map<String, String> powertrainOptions() {
        return carEnumOptions.powertrainSelectOptions();
    }

    @ModelAttribute("transmissionOptions")
    public Map<String, String> transmissionOptions() {
        return carEnumOptions.transmissionSelectOptions();
    }

    @ModelAttribute("uploadMaxImageBytes")
    public long uploadMaxImageBytes() {
        return imageService.getMaxImageBytes();
    }

    @ModelAttribute("uploadMaxImageMegabytes")
    public long uploadMaxImageMegabytes() {
        return imageService.getMaxImageMegabytesRoundedUp();
    }

    @ModelAttribute("uploadMaxGalleryVideoBytes")
    public long uploadMaxGalleryVideoBytes() {
        return carGalleryUploadPolicy.getMaxVideoBytes();
    }

    @ModelAttribute("uploadMaxGalleryVideoMegabytes")
    public long uploadMaxGalleryVideoMegabytes() {
        return carGalleryUploadPolicy.getMaxVideoMegabytesRoundedUp();
    }

    @ModelAttribute("uploadMaxProfileDocumentMegabytes")
    public int uploadMaxProfileDocumentMegabytes() {
        return profileDocumentUploadPolicy.getMaxMegabytesRoundedUp();
    }

    @ModelAttribute("carYearMax")
    public int carYearMax() {
        return java.time.Year.now().getValue();
    }

    @GetMapping
    public ModelAndView index(
            @CurrentUser final User currentUser,
            final HttpSession session) {
        final User me = WebAuthUtils.requireUser(currentUser);
        final User fresh = userService.getUserById(me.getId()).orElse(me);
        // Defense-in-depth (matches the service-layer guard in CarServiceImpl#publishCar): blocked owners
        // cannot publish a new car, so we don't even show them the form. They are redirected to /my-cars
        // where the global navbar banner explains why and points them at the refund-proof upload.
        if (fresh.isBlocked()) {
            return new ModelAndView("redirect:/my-cars");
        }
        if (!hasPublishPrerequisites(fresh)) {
            return publishCarPrerequisitesView(fresh);
        }
        pictureStash.clear(session);
        insuranceStash.clear(session);
        final PublishCarForm form = new PublishCarForm();
        final ModelAndView mav = publishCarFormView(session);
        mav.addObject("publishCarForm", form);
        return mav;
    }

    @GetMapping("/retained-insurance")
    public ResponseEntity<byte[]> retainedInsurance(
            @CurrentUser final User currentUser,
            final HttpSession session) {
        WebAuthUtils.requireUser(currentUser);
        final PublishCarRetainedInsurance meta = insuranceStash.getOrNull(session);
        if (meta == null) {
            return ResponseEntity.notFound().build();
        }
        final Optional<byte[]> bytes;
        try {
            bytes = insuranceStash.readRetainedBytes(session);
        } catch (final IOException e) {
            LOG.atDebug()
                    .setMessage("Could not read retained publish-car insurance bytes for session=[{}]")
                    .addArgument(session.getId())
                    .setCause(e)
                    .log();
            return ResponseEntity.notFound().build();
        }
        if (bytes.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        final MediaType mediaType;
        try {
            mediaType = meta.contentType().isBlank()
                    ? MediaType.APPLICATION_OCTET_STREAM
                    : MediaType.parseMediaType(meta.contentType());
        } catch (final IllegalArgumentException e) {
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_OCTET_STREAM).body(bytes.get());
        }
        return ResponseEntity.ok().contentType(mediaType).body(bytes.get());
    }

    @PostMapping("/retained-insurance/remove")
    public ResponseEntity<Void> removeRetainedInsurance(
            @CurrentUser final User currentUser,
            final HttpSession session) {
        WebAuthUtils.requireUser(currentUser);
        if (!insuranceStash.isStashed(session)) {
            return ResponseEntity.notFound().build();
        }
        insuranceStash.clear(session);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/retained-picture/{token}")
    public ResponseEntity<byte[]> retainedPicture(
            @CurrentUser final User currentUser,
            final HttpSession session,
            @PathVariable final String token) {
        WebAuthUtils.requireUser(currentUser);
        final PublishCarRetainedImage img = pictureStash.getOrNull(session, token);
        if (img == null) {
            return ResponseEntity.notFound().build();
        }
        final Optional<byte[]> bytes;
        try {
            bytes = pictureStash.readRetainedBytes(session, token);
        } catch (final IOException e) {
            LOG.atDebug()
                    .setMessage("Could not read retained publish-car picture bytes token=[{}]")
                    .addArgument(token)
                    .setCause(e)
                    .log();
            return ResponseEntity.notFound().build();
        }
        if (bytes.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        final MediaType mediaType = PublishCarPictureSessionStash.safeGalleryMediaType(img.contentType());
        return ResponseEntity.ok().contentType(mediaType).body(bytes.get());
    }

    @PostMapping("/retained-picture/{token}/remove")
    public ResponseEntity<Void> removeRetainedPicture(
            @CurrentUser final User currentUser,
            final HttpSession session,
            @PathVariable final String token) {
        WebAuthUtils.requireUser(currentUser);
        if (pictureStash.removeByToken(session, token)) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping
    public ModelAndView publish(
            @CurrentUser final User currentUser,
            final HttpSession session,
            final Locale locale,
            @Validated(ValidationGroups.OnPublishCar.class) @ModelAttribute("publishCarForm") final PublishCarForm form,
            final BindingResult errors) {
        // Defensive backend check: prerequisites (CBU + identity) must be in place before publishing.
        final User me = WebAuthUtils.requireUser(currentUser);
        final User fresh = userService.getUserById(me.getId()).orElse(me);
        if (!hasPublishPrerequisites(fresh)) {
            return publishCarPrerequisitesView(fresh);
        }
        publishCarFormValidator.validate(form, errors);
        syncPicturesFromPublishForm(form, session, errors);
        pictureStash.validatePicturePresence(form, session, errors);
        if (errors.hasErrors()) {
            // Only stash a fresh insurance file when it itself is valid (e.g. another field failed);
            // an over-sized upload must NOT replace the previously stashed file.
            if (!errors.hasFieldErrors("insuranceFile")) {
                insuranceStash.trySyncFromForm(form, session);
            }
            return publishCarFormView(session, form);
        }

        final int fromForm = PublishCarPictureSessionStash.countNonEmptyMultipart(form.getPictures());
        if (fromForm > 0) {
            if (!multipartGalleryMediaValidation.validateFilesAreGalleryMedia(form.getPictures(), errors, "pictures")) {
                syncPicturesFromPublishForm(form, session, errors);
                insuranceStash.trySyncFromForm(form, session);
                return publishCarFormView(session, form);
            }
            if (!multipartGalleryMediaValidation.validateFilesWithinMaxSize(form.getPictures(), errors)) {
                syncPicturesFromPublishForm(form, session, errors);
                insuranceStash.trySyncFromForm(form, session);
                return publishCarFormView(session, form);
            }
        }

        final List<GalleryMediaUpload> uploads;
        try {
            uploads = pictureStash.resolveUploads(form, session);
        } catch (final IOException e) {
            errors.reject(
                    MessageKeys.PUBLISH_IMAGES_READ,
                    localeMessages.msg(MessageKeys.PUBLISH_IMAGES_READ));
            syncPicturesFromPublishForm(form, session, errors);
            insuranceStash.trySyncFromForm(form, session);
            return publishCarFormView(session, form);
        }

        if (!multipartGalleryMediaValidation.validateGalleryMediaUploadsAreAllowed(uploads, errors, "pictures")) {
            syncPicturesFromPublishForm(form, session, errors);
            insuranceStash.trySyncFromForm(form, session);
            return publishCarFormView(session, form);
        }
        if (!multipartGalleryMediaValidation.validateGalleryMediaUploadsWithinMaxSize(uploads, errors)) {
            syncPicturesFromPublishForm(form, session, errors);
            insuranceStash.trySyncFromForm(form, session);
            return publishCarFormView(session, form);
        }

        // Optional insurance file: presence is optional, but when supplied the size constraint is
        // already enforced by @MaxFileSize on PublishCarForm.insuranceFile (see ValidCbu / NotEmptyFile
        // siblings under webapp.validation.constraint). We only need to fall back to the previously
        // stashed file when the form does not bring a fresh one, and to persist a fresh upload so it
        // survives subsequent retries on the same session.
        final MultipartFile insuranceFile = form.getInsuranceFile();
        final boolean hasFreshInsurance = insuranceFile != null && !insuranceFile.isEmpty();
        if (hasFreshInsurance) {
            insuranceStash.trySyncFromForm(form, session);
        }

        try {
            final long ownerId = WebAuthUtils.requireUser(currentUser).getId();

            // Resolve insurance bytes here (web concern: fresh upload vs session stash). Brand /
            // model resolution + the admin-vs-owner publishing branch live in CarPublishingService.
            final byte[] insuranceBytes;
            final String insuranceName;
            final String insuranceType;
            if (hasFreshInsurance) {
                insuranceBytes = insuranceFile.getBytes();
                insuranceName = insuranceFile.getOriginalFilename();
                insuranceType = insuranceFile.getContentType();
            } else {
                final PublishCarRetainedInsurance retained = insuranceStash.getOrNull(session);
                final Optional<byte[]> stashed = retained == null
                        ? Optional.empty()
                        : insuranceStash.readRetainedBytes(session);
                if (retained != null && stashed.isPresent()) {
                    insuranceBytes = stashed.get();
                    insuranceName = retained.filename();
                    insuranceType = retained.contentType();
                } else {
                    insuranceBytes = null;
                    insuranceName = null;
                    insuranceType = null;
                }
            }

            final PublishCarOutcome outcome = carPublishingService.publishCar(ownerId,
                    PublishCarRequest.builder()
                            .brand(form.getBrand())
                            .model(form.getModel())
                            .type(form.getType())
                            .plate(form.getPlate())
                            .year(form.getYear())
                            .powertrain(form.getPowertrain())
                            .transmission(form.getTransmission())
                            .description(form.getDescription())
                            .galleryUploads(uploads)
                            .insurance(insuranceName, insuranceType, insuranceBytes)
                            .build(),
                    locale);

            pictureStash.clear(session);
            insuranceStash.clear(session);

            if (outcome.getKind() == PublishCarOutcome.Kind.PENDING_VALIDATION) {
                final ModelAndView pendingMav = new ModelAndView("car/publishCarPending");
                pendingMav.addObject("createdCarId", outcome.getCar().getId());
                outcome.getPendingBrandName().ifPresent(name -> pendingMav.addObject("pendingBrand", name));
                outcome.getPendingModelName().ifPresent(name -> pendingMav.addObject("pendingModel", name));
                return pendingMav;
            }
            final ModelAndView mav = new ModelAndView("car/publishCarConfirmation");
            mav.addObject("car", outcome.getCar());
            if (outcome.isNewCatalogEntry()) {
                mav.addObject("newCatalogEntry", true);
            }
            return mav;
        } catch (final DuplicatePlateException e) {
            errors.rejectValue(
                    "plate",
                    e.getMessageCode(),
                    e.getMessageArgs(),
                    localeMessages.msg(e));
            syncPicturesFromPublishForm(form, session, errors);
            return publishCarFormView(session, form);
        } catch (final RydenException e) {
            // Typed domain failures (e.g. ImageValidationException) get repainted on the form with
            // their specific i18n message so the user sees what to fix without losing the stash.
            // Unexpected RuntimeExceptions are intentionally NOT caught: they bubble up to
            // RydenExceptionHandler / UnhandledExceptionHandler (see webapp.exception package).
            LOG.atDebug()
                    .setMessage("Publish car rejected for ownerId={} plate={} code={}")
                    .addArgument(currentUser != null ? currentUser.getId() : null)
                    .addArgument(form.getPlate())
                    .addArgument(e.getMessageCode())
                    .setCause(e)
                    .log();
            errors.reject(e.getMessageCode(), e.getMessageArgs(), localeMessages.msg(e));
            syncPicturesFromPublishForm(form, session, errors);
            return publishCarFormView(session, form);
        } catch (final IOException e) {
            // Reading the freshly uploaded insurance file or its stashed copy failed.
            // Repaint the form with the same generic "upload read" message used elsewhere
            // (see the pictureStash.resolveUploads catch above).
            errors.reject(
                    MessageKeys.PUBLISH_IMAGES_READ,
                    localeMessages.msg(MessageKeys.PUBLISH_IMAGES_READ));
            syncPicturesFromPublishForm(form, session, errors);
            insuranceStash.trySyncFromForm(form, session);
            return publishCarFormView(session, form);
        }
    }

    private void syncPicturesFromPublishForm(
            final PublishCarForm form,
            final HttpSession session,
            final BindingResult errors) {
        pictureStash.trySyncFromForm(form, session, errors);
        if (PublishCarPictureSessionStash.countNonEmptyMultipart(form.getPictures()) == 0) {
            pictureStash.applyCoverFromForm(form, session);
        }
    }

    private ModelAndView publishCarFormView(final HttpSession session) {
        return publishCarFormView(session, null);
    }

    private ModelAndView publishCarFormView(final HttpSession session, final PublishCarForm form) {
        final ModelAndView mav = new ModelAndView("car/publishCarForm");
        mav.addObject("activeTab", "publish-car");
        final List<String> stashedTokens = pictureStash.getStashedTokens(session);
        mav.addObject("retainedPictureTokens", stashedTokens);
        mav.addObject("retainedPictures", pictureStash.getStashedRetainedImages(session));
        mav.addObject("publishPicturesClientRequired", stashedTokens.isEmpty());
        final int coverIndex = pictureStash.getCoverPictureIndex(session);
        mav.addObject("coverPictureIndex", coverIndex);
        if (form != null) {
            form.setCoverPictureIndex(coverIndex);
        }
        final PublishCarRetainedInsurance retainedInsurance = insuranceStash.getOrNull(session);
        if (retainedInsurance != null) {
            mav.addObject("retainedInsuranceFilename", retainedInsurance.filename());
        }
        return mav;
    }

    /**
     * AJAX endpoint used by the prerequisites screen modal to save the identity document
     * without a full reload (same validation as the profile flow).
     */
    @PostMapping(value = "/quick-identity", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> quickIdentity(
            @CurrentUser final User currentUser,
            @RequestParam(name = "identityFile", required = false) final MultipartFile identityFile) {
        final User me = WebAuthUtils.requireUser(currentUser);
        final var outcome = userBookingDocumentsFacade.attemptUpload(
                me.getId(), UserDocumentType.IDENTITY, identityFile, true);
        switch (outcome.getStatus()) {
            case OK:
            case SKIPPED_ALREADY_PRESENT:
                return ResponseEntity.noContent().build();
            case BUSINESS_ERROR:
                final HttpHeaders headers = new HttpHeaders();
                outcome.getLocalizedBusinessMessage().ifPresent(msg -> headers.add("X-Ryden-Error", msg));
                return new ResponseEntity<>(null, headers, HttpStatus.BAD_REQUEST);
            case READ_ERROR:
                outcome.getReadException().ifPresent(ex -> LOG.atWarn()
                        .setMessage("Could not read uploaded identity document for userId={}")
                        .addArgument(me.getId())
                        .setCause(ex)
                        .log());
                return ResponseEntity.badRequest().build();
            case SKIPPED_EMPTY:
            default:
                return ResponseEntity.badRequest().build();
        }
    }

    private boolean hasPublishPrerequisites(final User user) {
        return userService.hasValidCbu(user) && user.getIdentityFileId().isPresent();
    }

    private ModelAndView publishCarPrerequisitesView(final User user) {
        final ModelAndView mav = new ModelAndView("car/publishCarPrerequisites");
        mav.addObject("activeTab", "publish-car");
        mav.addObject("publisherHasCbu", userService.hasValidCbu(user));
        mav.addObject("publisherHasIdentity", user.getIdentityFileId().isPresent());
        mav.addObject("cbuRequiredDigits", CbuRules.REQUIRED_DIGIT_LENGTH);
        return mav;
    }
}
