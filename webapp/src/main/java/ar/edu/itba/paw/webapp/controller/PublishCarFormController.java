package ar.edu.itba.paw.webapp.controller;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpSession;
import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import ar.edu.itba.paw.dto.ImageUpload;
import ar.edu.itba.paw.exception.MessageKeys;
import ar.edu.itba.paw.exception.RydenException;
import ar.edu.itba.paw.models.AvailabilityPeriod;
import ar.edu.itba.paw.services.ImageService;
import ar.edu.itba.paw.services.ListingService;
import ar.edu.itba.paw.webapp.dto.PublishCarRetainedImage;
import ar.edu.itba.paw.webapp.form.PublishCarForm;
import ar.edu.itba.paw.webapp.util.CarEnumOptions;
import ar.edu.itba.paw.webapp.util.LocaleMessages;
import ar.edu.itba.paw.webapp.util.MultipartImageValidation;
import ar.edu.itba.paw.webapp.util.PublishCarPictureSessionStash;
import ar.edu.itba.paw.webapp.util.WebAuthUtils;

@Controller
@RequestMapping("/publish-car")
public class PublishCarFormController {

    private final ListingService listingService;
    private final LocaleMessages localeMessages;
    private final ImageService imageService;
    private final MultipartImageValidation multipartImageValidation;
    private final PublishCarPictureSessionStash pictureStash;
    private final CarEnumOptions carEnumOptions;

    @Autowired
    public PublishCarFormController(
            final ListingService listingService,
            final LocaleMessages localeMessages,
            final ImageService imageService,
            final MultipartImageValidation multipartImageValidation,
            final PublishCarPictureSessionStash pictureStash,
            final CarEnumOptions carEnumOptions) {
        this.listingService = listingService;
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

    @ModelAttribute("publishCarForm")
    public PublishCarForm publishCarForm() {
        return new PublishCarForm();
    }

    @GetMapping
    public ModelAndView index(final Authentication authentication, final HttpSession session) {
        pictureStash.clear(session);
        return publishCarFormView(authentication, session);
    }

    @GetMapping("/retained-picture/{index:\\d+}")
    public ResponseEntity<byte[]> retainedPicture(
            final Authentication authentication,
            final HttpSession session,
            @PathVariable final int index) {
        WebAuthUtils.requireCurrentUser(authentication);
        final PublishCarRetainedImage img = pictureStash.getOrNull(session, index);
        if (img == null) {
            return ResponseEntity.notFound().build();
        }
        final MediaType mediaType = PublishCarPictureSessionStash.safeImageMediaType(img.contentType());
        return ResponseEntity.ok().contentType(mediaType).body(img.data());
    }

    @PostMapping
    public ModelAndView formSubmit(
            final Authentication authentication,
            @Valid @ModelAttribute("publishCarForm") final PublishCarForm form,
            final BindingResult errors,
            final HttpSession session) {
        if (errors.hasErrors()) {
            pictureStash.trySyncFromForm(form, session, errors);
            return publishCarFormView(authentication, session);
        }

        pictureStash.validatePicturePresence(form, session, errors);
        if (errors.hasErrors()) {
            pictureStash.trySyncFromForm(form, session, errors);
            return publishCarFormView(authentication, session);
        }

        final int fromForm = PublishCarPictureSessionStash.countNonEmptyMultipart(form.getPictures());
        if (fromForm > 0) {
            if (!multipartImageValidation.validateFilesAreImages(form.getPictures(), errors, "pictures")) {
                pictureStash.trySyncFromForm(form, session, errors);
                return publishCarFormView(authentication, session);
            }
            if (!multipartImageValidation.validateFilesWithinMaxSize(form.getPictures(), errors)) {
                pictureStash.trySyncFromForm(form, session, errors);
                return publishCarFormView(authentication, session);
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
            return publishCarFormView(authentication, session);
        }

        if (!multipartImageValidation.validateImageUploadsAreImages(uploads, errors, "pictures")) {
            pictureStash.trySyncFromForm(form, session, errors);
            return publishCarFormView(authentication, session);
        }
        if (!multipartImageValidation.validateImageUploadsWithinMaxSize(uploads, errors)) {
            pictureStash.trySyncFromForm(form, session, errors);
            return publishCarFormView(authentication, session);
        }

        try {
            final List<AvailabilityPeriod> periods = form.toAvailabilityPeriods();
            final long ownerId = WebAuthUtils.requireCurrentUser(authentication).getUserId();
            final var result = listingService.publish(
                    ownerId,
                    form.getPlate(),
                    form.getBrand(),
                    form.getModel(),
                    form.getType(),
                    form.getPowertrain(),
                    form.getTransmission(),
                    form.getPricePerDay(),
                    form.getStartPoint(),
                    form.getDescription(),
                    form.getCheckInTime(),
                    form.getCheckOutTime(),
                    periods,
                    uploads);

            pictureStash.clear(session);

            final ModelAndView mav = new ModelAndView("publishCarConfirmation");
            mav.addObject("car", result.getCar());
            mav.addObject("listing", result.getListing());
            mav.addObject("publisher", result.getPublisher());
            return mav;
        } catch (final RydenException e) {
            final String detail = localeMessages.msg(e);
            errors.reject(
                    MessageKeys.PUBLISH_FAILED,
                    localeMessages.msg(MessageKeys.PUBLISH_FAILED, detail));
            pictureStash.trySyncFromForm(form, session, errors);
            return publishCarFormView(authentication, session);
        }
    }

    private ModelAndView publishCarFormView(final Authentication authentication, final HttpSession session) {
        final ModelAndView mav = new ModelAndView("publishCarForm");
        mav.addObject("activeTab", "publish-car");
        final var me = WebAuthUtils.requireCurrentUser(authentication);
        mav.addObject("publisherEmail", me.getUsername());
        mav.addObject("publisherDisplayName", me.getForename() + " " + me.getSurname());
        mav.addObject("retainedPicturesCount", pictureStash.stashSize(session));
        return mav;
    }
}
