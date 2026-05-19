package ar.edu.itba.paw.webapp.controller;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import org.springframework.web.servlet.ModelAndView;

import ar.edu.itba.paw.dto.ImageUpload;
import ar.edu.itba.paw.exception.MessageKeys;
import ar.edu.itba.paw.exception.listing.DuplicatePlateException;
import ar.edu.itba.paw.models.domain.Car;
import ar.edu.itba.paw.models.domain.User;
import ar.edu.itba.paw.services.CarService;
import ar.edu.itba.paw.services.ImageService;
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
    private final LocaleMessages localeMessages;
    private final ImageService imageService;
    private final MultipartImageValidation multipartImageValidation;
    private final PublishCarPictureSessionStash pictureStash;
    private final CarEnumOptions carEnumOptions;

    public PublishCarFormController(
            final CarService carService,
            final LocaleMessages localeMessages,
            final ImageService imageService,
            final MultipartImageValidation multipartImageValidation,
            final PublishCarPictureSessionStash pictureStash,
            final CarEnumOptions carEnumOptions) {
        this.carService = carService;
        this.localeMessages = localeMessages;
        this.imageService = imageService;
        this.multipartImageValidation = multipartImageValidation;
        this.pictureStash = pictureStash;
        this.carEnumOptions = carEnumOptions;
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

    @GetMapping
    public ModelAndView index(
            @CurrentUser final User currentUser,
            final HttpSession session) {
        WebAuthUtils.requireUser(currentUser);
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

        try {
            final long ownerId = WebAuthUtils.requireUser(currentUser).getId();
            final Car car = carService.publishCar(
                    ownerId,
                    form.getPlate(),
                    form.getBrand(),
                    form.getModel(),
                    form.getType(),
                    form.getPowertrain(),
                    form.getTransmission(),
                    uploads);

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
}
