package ar.edu.itba.paw.webapp.controller;

import java.io.IOException;
import java.util.List;
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

import ar.edu.itba.paw.dto.ImageUpload;
import ar.edu.itba.paw.exception.MessageKeys;
import ar.edu.itba.paw.exception.RydenException;
import ar.edu.itba.paw.exception.car.DuplicatePlateException;
import ar.edu.itba.paw.models.domain.Car;
import ar.edu.itba.paw.models.domain.CarBrand;
import ar.edu.itba.paw.models.domain.CarModel;
import ar.edu.itba.paw.models.domain.User;
import ar.edu.itba.paw.models.domain.UserDocumentType;
import ar.edu.itba.paw.models.util.CbuRules;
import ar.edu.itba.paw.services.CarBrandService;
import ar.edu.itba.paw.services.CarModelService;
import ar.edu.itba.paw.services.CarService;
import ar.edu.itba.paw.services.ImageService;
import ar.edu.itba.paw.services.UserService;
import ar.edu.itba.paw.services.policy.ProfileDocumentUploadPolicy;
import ar.edu.itba.paw.webapp.dto.PublishCarRetainedImage;
import ar.edu.itba.paw.webapp.form.PublishCarForm;
import ar.edu.itba.paw.webapp.support.CurrentUser;
import ar.edu.itba.paw.webapp.util.CarEnumOptions;
import ar.edu.itba.paw.webapp.util.LocaleMessages;
import ar.edu.itba.paw.webapp.util.PublishCarPictureSessionStash;
import ar.edu.itba.paw.webapp.util.WebAuthUtils;
import ar.edu.itba.paw.webapp.validation.ValidationGroups;
import ar.edu.itba.paw.webapp.validation.support.MultipartImageValidation;

/** Step 1 of the two-step publish flow: registers a car with pictures. */
@Controller
@RequestMapping("/publish-car")
public final class PublishCarFormController {

    private static final Logger LOG = LoggerFactory.getLogger(PublishCarFormController.class);

    private final CarService carService;
    private final CarBrandService carBrandService;
    private final CarModelService carModelService;
    private final LocaleMessages localeMessages;
    private final ImageService imageService;
    private final MultipartImageValidation multipartImageValidation;
    private final PublishCarPictureSessionStash pictureStash;
    private final CarEnumOptions carEnumOptions;
    private final UserService userService;
    private final ProfileDocumentUploadPolicy profileDocumentUploadPolicy;

    public PublishCarFormController(
            final CarService carService,
            final CarBrandService carBrandService,
            final CarModelService carModelService,
            final LocaleMessages localeMessages,
            final ImageService imageService,
            final MultipartImageValidation multipartImageValidation,
            final PublishCarPictureSessionStash pictureStash,
            final CarEnumOptions carEnumOptions,
            final UserService userService,
            final ProfileDocumentUploadPolicy profileDocumentUploadPolicy) {
        this.carService = carService;
        this.carBrandService = carBrandService;
        this.carModelService = carModelService;
        this.localeMessages = localeMessages;
        this.imageService = imageService;
        this.multipartImageValidation = multipartImageValidation;
        this.pictureStash = pictureStash;
        this.carEnumOptions = carEnumOptions;
        this.userService = userService;
        this.profileDocumentUploadPolicy = profileDocumentUploadPolicy;
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

    @ModelAttribute("uploadMaxProfileDocumentMegabytes")
    public int uploadMaxProfileDocumentMegabytes() {
        return profileDocumentUploadPolicy.getMaxMegabytesRoundedUp();
    }

    @GetMapping
    public ModelAndView index(
            @CurrentUser final User currentUser,
            final HttpSession session) {
        final User me = WebAuthUtils.requireUser(currentUser);
        final User fresh = userService.getUserById(me.getId()).orElse(me);
        if (!hasPublishPrerequisites(fresh)) {
            return publishCarPrerequisitesView(fresh);
        }
        pictureStash.clear(session);
        final PublishCarForm form = new PublishCarForm();
        final ModelAndView mav = publishCarFormView(session);
        mav.addObject("publishCarForm", form);
        return mav;
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
        final MediaType mediaType = PublishCarPictureSessionStash.safeImageMediaType(img.contentType());
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
            @Validated(ValidationGroups.OnPublishCar.class) @ModelAttribute("publishCarForm") final PublishCarForm form,
            final BindingResult errors) {
        // Defensive backend check: prerequisites (CBU + identity) must be in place before publishing.
        final User me = WebAuthUtils.requireUser(currentUser);
        final User fresh = userService.getUserById(me.getId()).orElse(me);
        if (!hasPublishPrerequisites(fresh)) {
            return publishCarPrerequisitesView(fresh);
        }
        pictureStash.trySyncFromForm(form, session, errors);
        pictureStash.validatePicturePresence(form, session, errors);
        if (errors.hasErrors()) {
            return publishCarFormView(session);
        }

        final int fromForm = PublishCarPictureSessionStash.countNonEmptyMultipart(form.getPictures());
        if (fromForm > 0) {
            if (!multipartImageValidation.validateFilesAreImages(form.getPictures(), errors, "pictures")) {
                pictureStash.trySyncFromForm(form, session, errors);
                return publishCarFormView(session);
            }
            if (!multipartImageValidation.validateFilesWithinMaxSize(form.getPictures(), errors)) {
                pictureStash.trySyncFromForm(form, session, errors);
                return publishCarFormView(session);
            }
        }

        final List<ImageUpload> uploads;
        try {
            uploads = pictureStash.resolveUploads(form, session);
        } catch (final IOException e) {
            errors.reject(
                    MessageKeys.PUBLISH_IMAGES_READ,
                    localeMessages.msg(MessageKeys.PUBLISH_IMAGES_READ));
            pictureStash.trySyncFromForm(form, session, errors);
            return publishCarFormView(session);
        }

        if (!multipartImageValidation.validateImageUploadsAreImages(uploads, errors, "pictures")) {
            pictureStash.trySyncFromForm(form, session, errors);
            return publishCarFormView(session);
        }
        if (!multipartImageValidation.validateImageUploadsWithinMaxSize(uploads, errors)) {
            pictureStash.trySyncFromForm(form, session, errors);
            return publishCarFormView(session);
        }

        // Optional insurance file: when present, must fit the profile-document size limit.
        final MultipartFile insuranceFile = form.getInsuranceFile();
        final boolean hasInsurance = insuranceFile != null && !insuranceFile.isEmpty();
        if (hasInsurance && insuranceFile.getSize() > profileDocumentUploadPolicy.getMaxBytes()) {
            errors.rejectValue(
                    "insuranceFile",
                    MessageKeys.PUBLISH_FAILED,
                    new Object[]{profileDocumentUploadPolicy.getMaxMegabytesRoundedUp()},
                    localeMessages.msg(MessageKeys.PUBLISH_FAILED));
            pictureStash.trySyncFromForm(form, session, errors);
            return publishCarFormView(session);
        }

        try {
            final long ownerId = WebAuthUtils.requireUser(currentUser).getId();

            // Resolve brand/model strings (validated non-blank by JSR-303) to a catalog CarModel
            final CarBrand resolvedBrand = carBrandService.findOrCreateUnvalidated(form.getBrand())
                    .orElseThrow(() -> new IllegalStateException("Could not resolve brand: " + form.getBrand()));
            final CarModel resolvedModel = carModelService.findOrCreateUnvalidated(
                            resolvedBrand.getId(), form.getModel(), form.getType())
                    .orElseThrow(() -> new IllegalStateException("Could not resolve model: " + form.getModel()));

            final byte[] insuranceBytes = hasInsurance ? insuranceFile.getBytes() : null;
            final String insuranceName = hasInsurance ? insuranceFile.getOriginalFilename() : null;
            final String insuranceType = hasInsurance ? insuranceFile.getContentType() : null;
            final Car car = carService.publishCar(
                    ownerId,
                    form.getPlate(),
                    resolvedModel.getId(),
                    form.getType(),
                    form.getPowertrain(),
                    form.getTransmission(),
                    form.getDescription(),
                    uploads,
                    insuranceName,
                    insuranceType,
                    insuranceBytes);

            pictureStash.clear(session);

            final ModelAndView mav = new ModelAndView("publishCarConfirmation");
            mav.addObject("car", car);
            return mav;
        } catch (final DuplicatePlateException e) {
            errors.rejectValue(
                    "plate",
                    e.getMessageCode(),
                    e.getMessageArgs(),
                    localeMessages.msg(e));
            pictureStash.trySyncFromForm(form, session, errors);
            return publishCarFormView(session);
        } catch (final Exception e) {
            errors.reject(
                    MessageKeys.PUBLISH_FAILED,
                    localeMessages.msg(MessageKeys.PUBLISH_FAILED));
            pictureStash.trySyncFromForm(form, session, errors);
            return publishCarFormView(session);
        }
    }

    private ModelAndView publishCarFormView(final HttpSession session) {
        final ModelAndView mav = new ModelAndView("publishCarForm");
        mav.addObject("activeTab", "publish-car");
        final List<String> stashedTokens = pictureStash.getStashedTokens(session);
        mav.addObject("retainedPictureTokens", stashedTokens);
        mav.addObject("publishPicturesClientRequired", stashedTokens.isEmpty());
        return mav;
    }

    /**
     * AJAX endpoint used by the prerequisites screen modal to save the CBU without a full reload.
     */
    @PostMapping(value = "/quick-cbu", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<Void> quickCbu(
            @CurrentUser final User currentUser,
            @RequestParam("cbu") final String cbuRaw) {
        WebAuthUtils.requireUser(currentUser);
        final String trimmed = cbuRaw == null ? "" : cbuRaw.trim();
        if (!userService.isValidCbuFormat(cbuRaw)) {
            return ResponseEntity.badRequest().build();
        }
        userService.updateCbu(currentUser.getId(), trimmed);
        return ResponseEntity.noContent().build();
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
        if (identityFile == null || identityFile.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        final User fresh = userService.getUserById(me.getId()).orElse(me);
        if (fresh.getIdentityFileId().isPresent()) {
            // Idempotency: already uploaded; treat as success.
            return ResponseEntity.noContent().build();
        }
        try {
            userService.uploadValidatedProfileDocument(
                    me.getId(),
                    UserDocumentType.IDENTITY,
                    identityFile.getOriginalFilename(),
                    identityFile.getContentType(),
                    identityFile.getBytes());
            return ResponseEntity.noContent().build();
        } catch (final RydenException e) {
            final HttpHeaders headers = new HttpHeaders();
            headers.add("X-Ryden-Error", localeMessages.msg(e));
            return new ResponseEntity<>(null, headers, HttpStatus.BAD_REQUEST);
        } catch (final IOException e) {
            LOG.atWarn().setMessage("Could not read uploaded identity document for userId={}")
                    .addArgument(me.getId())
                    .setCause(e)
                    .log();
            return ResponseEntity.badRequest().build();
        }
    }

    private boolean hasPublishPrerequisites(final User user) {
        return userService.hasValidCbu(user) && user.getIdentityFileId().isPresent();
    }

    private ModelAndView publishCarPrerequisitesView(final User user) {
        final ModelAndView mav = new ModelAndView("publishCarPrerequisites");
        mav.addObject("activeTab", "publish-car");
        mav.addObject("publisherHasCbu", userService.hasValidCbu(user));
        mav.addObject("publisherHasIdentity", user.getIdentityFileId().isPresent());
        mav.addObject("cbuRequiredDigits", CbuRules.REQUIRED_DIGIT_LENGTH);
        return mav;
    }
}
