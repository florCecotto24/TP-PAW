package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.dto.ImageUpload;
import ar.edu.itba.paw.exception.MessageKeys;
import ar.edu.itba.paw.exception.RydenException;
import ar.edu.itba.paw.models.AvailabilityPeriod;
import ar.edu.itba.paw.services.ImageService;
import ar.edu.itba.paw.services.ListingService;
import ar.edu.itba.paw.webapp.form.PublishCarForm;
import ar.edu.itba.paw.webapp.util.CarEnumOptions;
import ar.edu.itba.paw.webapp.util.LocaleMessages;
import ar.edu.itba.paw.webapp.util.MultipartImageValidation;
import ar.edu.itba.paw.webapp.util.WebAuthUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import javax.validation.Valid;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/publish-car")
public class PublishCarFormController {

    @ModelAttribute("carTypeOptions")
    public Map<String, String> carTypeOptions() {
        return CarEnumOptions.carTypeSelectOptions();
    }

    @ModelAttribute("powertrainOptions")
    public Map<String, String> powertrainOptions() {
        return CarEnumOptions.powertrainSelectOptions();
    }

    @ModelAttribute("transmissionOptions")
    public Map<String, String> transmissionOptions() {
        return CarEnumOptions.transmissionSelectOptions();
    }

    private final ListingService listingService;
    private final LocaleMessages localeMessages;
    private final ImageService imageService;
    private final MultipartImageValidation multipartImageValidation;

    @Autowired
    public PublishCarFormController(
            final ListingService listingService,
            final LocaleMessages localeMessages,
            final ImageService imageService,
            final MultipartImageValidation multipartImageValidation) {
        this.listingService = listingService;
        this.localeMessages = localeMessages;
        this.imageService = imageService;
        this.multipartImageValidation = multipartImageValidation;
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
            final Authentication authentication,
            @ModelAttribute("publishCarForm") final PublishCarForm form) {
        final ModelAndView mav = new ModelAndView("publishCarForm");
        mav.addObject("activeTab", "publish-car");
        final var me = WebAuthUtils.requireCurrentUser(authentication);
        mav.addObject("publisherEmail", me.getUsername());
        mav.addObject("publisherDisplayName", me.getForename() + " " + me.getSurname());
        return mav;
    }

    @PostMapping
    public ModelAndView formSubmit(
            final Authentication authentication,
            @Valid @ModelAttribute("publishCarForm") final PublishCarForm form,
            final BindingResult errors) {
        if (errors.hasErrors()) {
            return index(authentication, form);
        }

        if (!multipartImageValidation.validateFilesAreImages(form.getPictures(), errors, "pictures")) {
            return index(authentication, form);
        }

        if (!multipartImageValidation.validateFilesWithinMaxSize(form.getPictures(), errors)) {
            return index(authentication, form);
        }

        final List<ImageUpload> uploads;
        try {
            uploads = toImageUploads(form.getPictures());
        } catch (final IOException e) {
            errors.reject(
                    MessageKeys.PUBLISH_IMAGES_READ,
                    localeMessages.msg(MessageKeys.PUBLISH_IMAGES_READ));
            return index(authentication, form);
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
            return index(authentication, form);
        }
    }

    private static List<ImageUpload> toImageUploads(final MultipartFile[] pictures) throws IOException {
        final List<ImageUpload> out = new ArrayList<>();
        if (pictures == null) {
            return out;
        }
        for (final MultipartFile picture : pictures) {
            if (picture == null || picture.isEmpty()) {
                continue;
            }
            out.add(new ImageUpload(
                    picture.getOriginalFilename(),
                    picture.getContentType(),
                    picture.getBytes()));
        }
        return out;
    }
}
