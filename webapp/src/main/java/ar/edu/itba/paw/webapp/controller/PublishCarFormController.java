package ar.edu.itba.paw.webapp.controller;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
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
import ar.edu.itba.paw.models.domain.AvailabilityPeriod;
import ar.edu.itba.paw.models.util.RiderPickupLeadTime;
import ar.edu.itba.paw.models.domain.User;
import ar.edu.itba.paw.services.ImageService;
import ar.edu.itba.paw.services.ListingService;
import ar.edu.itba.paw.services.LocationService;
import ar.edu.itba.paw.services.ReservationService;
import ar.edu.itba.paw.webapp.support.CurrentUser;
import ar.edu.itba.paw.webapp.dto.PublishCarRetainedImage;
import ar.edu.itba.paw.webapp.form.PublishCarForm;
import ar.edu.itba.paw.webapp.util.CarEnumOptions;
import ar.edu.itba.paw.webapp.util.LocaleMessages;
import ar.edu.itba.paw.webapp.util.PublishCarPictureSessionStash;
import ar.edu.itba.paw.webapp.util.WebAuthUtils;
import ar.edu.itba.paw.webapp.validation.ListingNeighborhoodFormValidator;
import ar.edu.itba.paw.webapp.validation.ValidationGroups;
import ar.edu.itba.paw.webapp.validation.support.MultipartImageValidation;

@Controller
@RequestMapping("/publish-car")
public class PublishCarFormController {

    private final ListingService listingService;
    private final LocaleMessages localeMessages;
    private final ImageService imageService;
    private final MultipartImageValidation multipartImageValidation;
    private final PublishCarPictureSessionStash pictureStash;
    private final CarEnumOptions carEnumOptions;
    private final LocationService locationService;
    private final ListingNeighborhoodFormValidator listingNeighborhoodFormValidator;
    private final ReservationService reservationService;

    @Autowired
    public PublishCarFormController(
            final ListingService listingService,
            final LocaleMessages localeMessages,
            final ImageService imageService,
            final MultipartImageValidation multipartImageValidation,
            final PublishCarPictureSessionStash pictureStash,
            final CarEnumOptions carEnumOptions,
            final LocationService locationService,
            final ListingNeighborhoodFormValidator listingNeighborhoodFormValidator,
            final ReservationService reservationService) {
        this.listingService = listingService;
        this.localeMessages = localeMessages;
        this.imageService = imageService;
        this.multipartImageValidation = multipartImageValidation;
        this.pictureStash = pictureStash;
        this.carEnumOptions = carEnumOptions;
        this.locationService = locationService;
        this.listingNeighborhoodFormValidator = listingNeighborhoodFormValidator;
        this.reservationService = reservationService;
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
        final PublishCarForm form = new PublishCarForm();
        final ModelAndView mav = publishCarFormView(currentUser, session, form);
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
        final int pickupLeadHours = reservationService.getConfiguredPickupLeadHours();
        final LocalDate minFrom = RiderPickupLeadTime.minListingAvailabilityFirstDayInclusive(
                lt, AvailabilityPeriod.WALL_ZONE, Instant.now(), pickupLeadHours);
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
        validatePublishAvailabilityRiderLead(form, errors);
        validatePublishAvailabilityTotalDays(form, errors);
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
            return LocalTime.of(10, 0);
        }
        try {
            return LocalTime.parse(checkInRaw.trim());
        } catch (final DateTimeParseException e) {
            return LocalTime.of(10, 0);
        }
    }

    private void validatePublishAvailabilityRiderLead(final PublishCarForm form, final BindingResult errors) {
        final LocalTime pickup = form.getCheckInTime() != null ? form.getCheckInTime() : LocalTime.of(10, 0);
        final int pickupLeadHours = reservationService.getConfiguredPickupLeadHours();
        final LocalDate minStart = RiderPickupLeadTime.minListingAvailabilityFirstDayInclusive(
                pickup, AvailabilityPeriod.WALL_ZONE, Instant.now(), pickupLeadHours);
        final List<PublishCarForm.AvailabilityRow> rows = form.getAvailabilityRows();
        for (int i = 0; i < rows.size(); i++) {
            final LocalDate from = rows.get(i).getFrom();
            if (from != null && from.isBefore(minStart)) {
                errors.rejectValue(
                        "availabilityRows[" + i + "].from",
                        "validation.availabilityRow.from.riderLeadTime",
                        new Object[] { minStart, pickupLeadHours },
                        localeMessages.msg(
                                "validation.availabilityRow.from.riderLeadTime",
                                minStart,
                                pickupLeadHours));
            }
        }
    }

    private void validatePublishAvailabilityTotalDays(final PublishCarForm form, final BindingResult errors) {
        final int maxDays = listingService.getConfiguredMaxAvailabilityTotalDays();
        long totalInclusive = 0L;
        final List<PublishCarForm.AvailabilityRow> rows = form.getAvailabilityRows();
        for (int i = 0; i < rows.size(); i++) {
            final LocalDate from = rows.get(i).getFrom();
            final LocalDate until = rows.get(i).getUntil();
            if (from == null || until == null) {
                continue;
            }
            if (until.isBefore(from)) {
                continue;
            }
            totalInclusive += ChronoUnit.DAYS.between(from, until) + 1;
        }
        if (totalInclusive > maxDays) {
            errors.reject(
                    "validation.availabilityRows.totalMaxDays",
                    new Object[] { maxDays },
                    localeMessages.msg("validation.availabilityRows.totalMaxDays", maxDays));
        }
    }

    private ModelAndView publishCarFormView(
            final User currentUser,
            final HttpSession session,
            final PublishCarForm publishFormForMinAvail) {
        final ModelAndView mav = new ModelAndView("publishCarForm");
        mav.addObject("activeTab", "publish-car");
        final User me = WebAuthUtils.requireUser(currentUser);
        mav.addObject("publisherEmail", me.getEmail());
        mav.addObject("publisherDisplayName", me.getForename() + " " + me.getSurname());
        final List<String> stashedTokens = pictureStash.getStashedTokens(session);
        mav.addObject("retainedPictureTokens", stashedTokens);
        mav.addObject("publishPicturesClientRequired", stashedTokens.isEmpty());
        mav.addObject("allNeighborhoods", locationService.findAllNeighborhoods());
        final LocalTime pickup = publishFormForMinAvail.getCheckInTime() != null
                ? publishFormForMinAvail.getCheckInTime()
                : LocalTime.of(10, 0);
        final int pickupLeadHours = reservationService.getConfiguredPickupLeadHours();
        final LocalDate publishMinAvailabilityFrom = RiderPickupLeadTime.minListingAvailabilityFirstDayInclusive(
                pickup, AvailabilityPeriod.WALL_ZONE, Instant.now(), pickupLeadHours);
        mav.addObject("publishMinAvailabilityFrom", publishMinAvailabilityFrom.toString());
        mav.addObject("pickupLeadHours", pickupLeadHours);
        mav.addObject("maxAvailabilityTotalDays", listingService.getConfiguredMaxAvailabilityTotalDays());
        return mav;
    }

}
