package ar.edu.itba.paw.webapp.controller;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;
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
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import ar.edu.itba.paw.dto.ImageUpload;
import ar.edu.itba.paw.exception.MessageKeys;
import ar.edu.itba.paw.exception.RydenException;
import ar.edu.itba.paw.exception.listing.AvailabilityRiderLeadViolationException;
import ar.edu.itba.paw.exception.listing.DuplicatePlateException;
import ar.edu.itba.paw.exception.listing.ListingValidationException;
import ar.edu.itba.paw.models.domain.AvailabilityPeriod;
import ar.edu.itba.paw.models.domain.Listing;
import ar.edu.itba.paw.models.domain.User;
import ar.edu.itba.paw.services.ImageService;
import ar.edu.itba.paw.services.ListingService;
import ar.edu.itba.paw.services.LocationService;
import ar.edu.itba.paw.services.UserService;
import ar.edu.itba.paw.webapp.dto.PublishCarRetainedImage;
import ar.edu.itba.paw.webapp.form.PublishCarForm;
import ar.edu.itba.paw.webapp.support.CurrentUser;
import ar.edu.itba.paw.webapp.util.CarEnumOptions;
import ar.edu.itba.paw.webapp.util.LocaleMessages;
import ar.edu.itba.paw.webapp.util.PublishCarPictureSessionStash;
import ar.edu.itba.paw.webapp.util.WebAuthUtils;
import ar.edu.itba.paw.webapp.validation.ListingNeighborhoodFormValidator;
import ar.edu.itba.paw.webapp.validation.ValidationGroups;
import ar.edu.itba.paw.webapp.validation.support.MultipartImageValidation;

/** Owner wizard to publish a car, listing, neighborhood, availability, and gallery images. */
@Controller
@RequestMapping("/publish-car")
public final class PublishCarFormController {

    private static final Logger LOG = LoggerFactory.getLogger(PublishCarFormController.class);

    private final ListingService listingService;
    private final LocaleMessages localeMessages;
    private final ImageService imageService;
    private final MultipartImageValidation multipartImageValidation;
    private final PublishCarPictureSessionStash pictureStash;
    private final CarEnumOptions carEnumOptions;
    private final LocationService locationService;
    private final ListingNeighborhoodFormValidator listingNeighborhoodFormValidator;
    private final UserService userService;

    public PublishCarFormController(
            final ListingService listingService,
            final LocaleMessages localeMessages,
            final ImageService imageService,
            final MultipartImageValidation multipartImageValidation,
            final PublishCarPictureSessionStash pictureStash,
            final CarEnumOptions carEnumOptions,
            final LocationService locationService,
            final ListingNeighborhoodFormValidator listingNeighborhoodFormValidator,
            final UserService userService) {
        this.listingService = listingService;
        this.localeMessages = localeMessages;
        this.imageService = imageService;
        this.multipartImageValidation = multipartImageValidation;
        this.pictureStash = pictureStash;
        this.carEnumOptions = carEnumOptions;
        this.locationService = locationService;
        this.listingNeighborhoodFormValidator = listingNeighborhoodFormValidator;
        this.userService = userService;
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
        pictureStash.clear(session);
        // Load a fresh user row so we validate against the current CBU in the database
        final User freshUser = userService.getUserById(currentUser.getId())
                .orElse(currentUser);
        final PublishCarForm form = new PublishCarForm();
        final ModelAndView mav = publishCarFormView(freshUser, session, form);
        mav.addObject("publishCarForm", form);
        return mav;
    }

    /**
     * First day of availability valid for the indicated check-in time (24 h rule for reservations).
     */
    @GetMapping(value = "/availability-min-from", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String, String> availabilityMinFrom(
            @RequestParam(name = "checkIn", required = false) final String checkInRaw) {
        final LocalTime lt = parseCheckInParam(checkInRaw);
        final LocalDate minFrom =
                listingService.getPublicationMinAvailabilityFirstWallDay(lt, Instant.now());
        return Map.of("minFrom", minFrom.toString());
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
        if (errors.hasErrors()) {
            return publishCarFormView(currentUser, session, form);
        }

        listingNeighborhoodFormValidator.validate(form, errors);
        if (!errors.hasErrors()) {
            try {
                listingService.validatePublicationAvailabilityRiderLead(
                        form.toAvailabilityPeriods(), form.getCheckInTime(), Instant.now());
            } catch (final AvailabilityRiderLeadViolationException e) {
                errors.rejectValue(
                        "availabilityRows[" + e.getAvailabilityRowIndex() + "].from",
                        e.getMessageCode(),
                        e.getMessageArgs(),
                        localeMessages.msg(e));
            }
        }
        if (!errors.hasErrors()) {
            try {
                listingService.validatePublicationAvailabilityAgainstWallCalendar(form.toAvailabilityPeriods());
            } catch (final ListingValidationException e) {
                errors.reject(
                        e.getMessageCode(),
                        e.getMessageArgs(),
                        localeMessages.msg(e));
            }
        }
        if (errors.hasErrors()) {
            return publishCarFormView(currentUser, session, form);
        }

        pictureStash.validatePicturePresence(form, session, errors);
        if (errors.hasErrors()) {
            pictureStash.trySyncFromForm(form, session, errors);
            return publishCarFormView(currentUser, session, form);
        }

        final int fromForm = PublishCarPictureSessionStash.countNonEmptyMultipart(form.getPictures());
        if (fromForm > 0) {
            if (!multipartImageValidation.validateFilesAreImages(form.getPictures(), errors, "pictures")) {
                pictureStash.trySyncFromForm(form, session, errors);
                return publishCarFormView(currentUser, session, form);
            }
            if (!multipartImageValidation.validateFilesWithinMaxSize(form.getPictures(), errors)) {
                pictureStash.trySyncFromForm(form, session, errors);
                return publishCarFormView(currentUser, session, form);
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
            return publishCarFormView(currentUser, session, form);
        }

        if (!multipartImageValidation.validateImageUploadsAreImages(uploads, errors, "pictures")) {
            pictureStash.trySyncFromForm(form, session, errors);
            return publishCarFormView(currentUser, session, form);
        }
        if (!multipartImageValidation.validateImageUploadsWithinMaxSize(uploads, errors)) {
            pictureStash.trySyncFromForm(form, session, errors);
            return publishCarFormView(currentUser, session, form);
        }

        try {
            final List<AvailabilityPeriod> periods = form.toAvailabilityPeriods();
            final long ownerId = WebAuthUtils.requireUser(currentUser).getId();
            final var result = listingService.publish(
                    ownerId,
                    form.getPlate(),
                    form.getBrand(),
                    form.getModel(),
                    form.getType(),
                    form.getPowertrain(),
                    form.getTransmission(),
                    form.getPricePerDay(),
                    form.getStartPointStreet(),
                    form.getStartPointNumber(),
                    form.getDescription(),
                    form.getCheckInTime(),
                    form.getCheckOutTime(),
                    periods,
                    uploads,
                    form.getNeighborhoodId());

            pictureStash.clear(session);

            final ModelAndView mav = new ModelAndView("publishCarConfirmation");
            mav.addObject("car", result.getCar());
            mav.addObject("listing", result.getListing());
            mav.addObject("publisher", result.getPublisher());
            return mav;
        } catch (final DuplicatePlateException e) {
            errors.rejectValue(
                    "plate",
                    e.getMessageCode(),
                    e.getMessageArgs(),
                    localeMessages.msg(e));
            pictureStash.trySyncFromForm(form, session, errors);
            return publishCarFormView(currentUser, session, form);
        } catch (final RydenException e) {
            final String detail = localeMessages.msg(e);
            errors.reject(
                    MessageKeys.PUBLISH_FAILED,
                    localeMessages.msg(MessageKeys.PUBLISH_FAILED, detail));
            pictureStash.trySyncFromForm(form, session, errors);
            return publishCarFormView(currentUser, session, form);
        }
    }

    private static LocalTime parseCheckInParam(final String checkInRaw) {
        if (checkInRaw == null || checkInRaw.isBlank()) {
            return Listing.DEFAULT_CHECK_IN_TIME;
        }
        try {
            return LocalTime.parse(checkInRaw.trim());
        } catch (final DateTimeParseException e) {
            LOG.atDebug()
                    .setMessage("Unparseable checkIn query param [{}] ; using default")
                    .addArgument(checkInRaw)
                    .setCause(e)
                    .log();
            return Listing.DEFAULT_CHECK_IN_TIME;
        }
    }

    /**
     * Saves CBU from the publish flow modal (same validation as profile); responds without a full page reload.
     */
    @PostMapping(value = "/quick-cbu", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<Void> publishQuickCbu(
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

    private ModelAndView publishCarFormView(
            final User currentUser,
            final HttpSession session,
            final PublishCarForm publishFormForMinAvail) {
        final ModelAndView mav = new ModelAndView("publishCarForm");
        mav.addObject("activeTab", "publish-car");
        final User me = WebAuthUtils.requireUser(currentUser);
        final User publisherRow = userService.getUserById(me.getId()).orElse(me);
        final boolean userHasCbu = userService.hasValidCbu(publisherRow);
        mav.addObject("userHasCbu", userHasCbu);
        mav.addObject("publisherEmail", publisherRow.getEmail());
        mav.addObject("publisherDisplayName", publisherRow.getForename() + " " + publisherRow.getSurname());
        final List<String> stashedTokens = pictureStash.getStashedTokens(session);
        mav.addObject("retainedPictureTokens", stashedTokens);
        mav.addObject("publishPicturesClientRequired", stashedTokens.isEmpty());
        mav.addObject("allNeighborhoods", locationService.findAllNeighborhoods());
        final LocalDate publishMinAvailabilityFrom =
                listingService.getPublicationMinAvailabilityFirstWallDay(
                        publishFormForMinAvail.getCheckInTime(), Instant.now());
        final int pickupLeadHours = listingService.getConfiguredPickupLeadHours();
        mav.addObject("publishMinAvailabilityFrom", publishMinAvailabilityFrom.toString());
        mav.addObject("pickupLeadHours", pickupLeadHours);
        final int forwardDays = listingService.getConfiguredMaxAvailabilityForwardWallDays();
        mav.addObject("maxAvailabilityForwardWallDays", forwardDays);
        final LocalDate wallToday = LocalDate.now(AvailabilityPeriod.WALL_ZONE);
        mav.addObject("publishMaxAvailabilityWallInclusive", wallToday.plusDays(forwardDays).toString());
        return mav;
    }

}
