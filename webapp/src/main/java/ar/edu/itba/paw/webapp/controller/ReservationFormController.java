package ar.edu.itba.paw.webapp.controller;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Optional;

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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import ar.edu.itba.paw.exception.MessageKeys;
import ar.edu.itba.paw.exception.RydenException;
import ar.edu.itba.paw.exception.reservation.ReservationException;
import ar.edu.itba.paw.models.domain.Car;
import ar.edu.itba.paw.models.domain.ListingAvailability;
import ar.edu.itba.paw.models.domain.Reservation;
import ar.edu.itba.paw.models.domain.User;
import ar.edu.itba.paw.models.domain.UserDocumentType;
import ar.edu.itba.paw.models.util.ArsMoneyFormat;
import ar.edu.itba.paw.models.util.WallDateTimeParsing;
import ar.edu.itba.paw.services.CarService;
import ar.edu.itba.paw.services.ImageService;
import ar.edu.itba.paw.services.ListingAvailabilityService;
import ar.edu.itba.paw.services.ReservationService;
import ar.edu.itba.paw.services.UserService;
import ar.edu.itba.paw.services.policy.ProfileDocumentUploadPolicy;
import ar.edu.itba.paw.services.util.ListingAddressFormatter;
import ar.edu.itba.paw.webapp.form.ReservationForm;
import ar.edu.itba.paw.webapp.support.CurrentUser;
import ar.edu.itba.paw.webapp.util.LocaleMessages;
import ar.edu.itba.paw.webapp.util.WallDateTimeUiFormatter;
import ar.edu.itba.paw.webapp.util.WebAuthUtils;
import ar.edu.itba.paw.webapp.validation.ValidationGroups;

/** Rider flow to create a reservation for a car (GET form + POST submit). */
@Controller
@RequestMapping("/reservation")
public final class ReservationFormController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReservationFormController.class);

    private final CarService carService;
    private final ListingAvailabilityService listingAvailabilityService;
    private final ReservationService reservationService;
    private final ImageService imageService;
    private final LocaleMessages localeMessages;
    private final WallDateTimeUiFormatter wallDateTimeUiFormatter;
    private final UserService userService;
    private final ProfileDocumentUploadPolicy profileDocumentUploadPolicy;
    private final ListingAddressFormatter listingAddressFormatter;

    public ReservationFormController(
            final CarService carService,
            final ListingAvailabilityService listingAvailabilityService,
            final ReservationService reservationService,
            final ImageService imageService,
            final LocaleMessages localeMessages,
            final WallDateTimeUiFormatter wallDateTimeUiFormatter,
            final UserService userService,
            final ProfileDocumentUploadPolicy profileDocumentUploadPolicy,
            final ListingAddressFormatter listingAddressFormatter) {
        this.carService = carService;
        this.listingAvailabilityService = listingAvailabilityService;
        this.reservationService = reservationService;
        this.imageService = imageService;
        this.localeMessages = localeMessages;
        this.wallDateTimeUiFormatter = wallDateTimeUiFormatter;
        this.userService = userService;
        this.profileDocumentUploadPolicy = profileDocumentUploadPolicy;
        this.listingAddressFormatter = listingAddressFormatter;
    }

    @GetMapping("/new")
    public ModelAndView index(
            @CurrentUser final User currentUser,
            @RequestParam(name = "carId") final long carId,
            @RequestParam(value = "availabilityId", required = false) final Long availabilityId,
            @RequestParam(value = "carName", required = false) final String carName,
            @RequestParam(value = "fromDateTime", required = false) final String fromDateTime,
            @RequestParam(value = "untilDateTime", required = false) final String untilDateTime,
            @RequestParam(value = "reservationTotal", required = false) final String reservationTotal,
            @ModelAttribute("reservationForm") final ReservationForm form) {
        final Optional<Car> carOpt = carService.getCarById(carId);
        if (carOpt.isEmpty()) {
            return redirectToSearch();
        }
        final Car car = carOpt.get();
        form.setCarId(carId);
        form.setCarName(carName != null && !carName.isBlank() ? carName : car.getBrand() + " " + car.getModel());
        final String loc = resolveCarHandoverLocation(carId, fromDateTime);
        form.setDeliveryLocation(loc);
        form.setFromDateTime(fromDateTime);
        form.setUntilDateTime(untilDateTime);

        final ModelAndView mav = new ModelAndView("reservation/reservationForm");
        mav.addObject("availabilityId", availabilityId);
        final User rider = WebAuthUtils.requireUser(currentUser);
        mav.addObject("riderForename", rider.getForename());
        mav.addObject("riderSurname", rider.getSurname());
        mav.addObject("riderEmail", rider.getEmail());
        addReservationPricingToModel(mav, carId, fromDateTime, untilDateTime, reservationTotal);
        wallDateTimeUiFormatter.addReservationFormDateDisplays(mav, form);
        addReservationPolicyHours(mav);
        addReservationFormRiderDocs(mav, rider);
        return mav;
    }

    /**
     * Saves identity/license from the reservation modal (same validation as profile); responds without a full page reload.
     */
    @PostMapping(value = "/booking-documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> bookingDocuments(
            @CurrentUser final User currentUser,
            @RequestParam(name = "licenseFile", required = false) final MultipartFile licenseFile,
            @RequestParam(name = "identityFile", required = false) final MultipartFile identityFile) {
        final User me = WebAuthUtils.requireUser(currentUser);
        final long userId = me.getId();
        try {
            boolean uploadedSomething = false;
            User fresh = userService.getUserById(userId).orElse(me);
            if (!isMissingOrEmpty(licenseFile) && fresh.getLicenseFileId().isEmpty()) {
                uploadBookingDocument(userId, UserDocumentType.LICENSE, licenseFile);
                uploadedSomething = true;
                fresh = userService.getUserById(userId).orElse(fresh);
            }
            if (!isMissingOrEmpty(identityFile) && fresh.getIdentityFileId().isEmpty()) {
                uploadBookingDocument(userId, UserDocumentType.IDENTITY, identityFile);
                uploadedSomething = true;
                fresh = userService.getUserById(userId).orElse(fresh);
            }
            final User after = userService.getUserById(userId).orElse(fresh);
            if (userService.hasUploadedLicenseAndIdentity(after)) {
                return ResponseEntity.noContent().build();
            }
            if (!uploadedSomething) {
                return ResponseEntity.badRequest().build();
            }
            final HttpHeaders headers = new HttpHeaders();
            headers.add("X-Ryden-Needs-License", after.getLicenseFileId().isEmpty() ? "true" : "false");
            headers.add("X-Ryden-Needs-Identity", after.getIdentityFileId().isEmpty() ? "true" : "false");
            return new ResponseEntity<>(null, headers, HttpStatus.ACCEPTED);
        } catch (final RydenException e) {
            return ResponseEntity.badRequest().build();
        } catch (final IOException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping
    public ModelAndView formSubmit(
            @CurrentUser final User currentUser,
            @Validated(ValidationGroups.OnReservationSubmit.class) @ModelAttribute("reservationForm") final ReservationForm form,
            final BindingResult errors,
            @RequestParam(value = "availabilityId", required = false) final Long availabilityId,
            @RequestParam(value = "reservationTotal", required = false) final String reservationTotal) {

        if (form.getCarId() == null) {
            return redirectToSearch();
        }
        final long carId = form.getCarId();

        final Optional<Car> carOpt = carService.getCarById(carId);
        if (carOpt.isEmpty()) {
            return redirectToSearch();
        }

        if (errors.hasErrors()) {
            final ModelAndView mav = new ModelAndView("reservation/reservationForm");
            mav.addObject("availabilityId", availabilityId);
            final User riderErr = WebAuthUtils.requireUser(currentUser);
            mav.addObject("riderForename", riderErr.getForename());
            mav.addObject("riderSurname", riderErr.getSurname());
            mav.addObject("riderEmail", riderErr.getEmail());
            addReservationPricingToModel(mav, carId, form.getFromDateTime(), form.getUntilDateTime(), reservationTotal);
            wallDateTimeUiFormatter.addReservationFormDateDisplays(mav, form);
            addReservationPolicyHours(mav);
            addReservationFormRiderDocs(mav, riderErr);
            return mav;
        }

        if (form.getCarName() == null || form.getCarName().isBlank()) {
            final ModelAndView mav = new ModelAndView("reservation/reservationForm");
            mav.addObject("reservationError", localeMessages.msg(MessageKeys.RESERVATION_FORM_CAR_NAME_REQUIRED));
            mav.addObject("availabilityId", availabilityId);
            final User riderCar = WebAuthUtils.requireUser(currentUser);
            mav.addObject("riderForename", riderCar.getForename());
            mav.addObject("riderSurname", riderCar.getSurname());
            mav.addObject("riderEmail", riderCar.getEmail());
            addReservationPricingToModel(mav, carId, form.getFromDateTime(), form.getUntilDateTime(), reservationTotal);
            wallDateTimeUiFormatter.addReservationFormDateDisplays(mav, form);
            addReservationPolicyHours(mav);
            addReservationFormRiderDocs(mav, riderCar);
            return mav;
        }

        final Reservation reservation;
        try {
            final long riderId = WebAuthUtils.requireUser(currentUser).getId();

            LOGGER.atInfo()
                    .addArgument(riderId)
                    .addArgument(carId)
                    .addArgument(availabilityId)
                    .addArgument(form.getFromDateTime())
                    .addArgument(form.getUntilDateTime())
                    .log("Submitting reservation for riderId={}, carId={}, availabilityId={}, fromDateTime={}, untilDateTime={}");

            reservation = reservationService.submitRiderReservationByCar(
                    riderId,
                    carId,
                    availabilityId,
                    form.getFromDateTime(),
                    form.getUntilDateTime());
        } catch (final ReservationException e) {
            final ModelAndView mav = new ModelAndView("reservation/reservationForm");
            mav.addObject("reservationError", localeMessages.msg(e));
            mav.addObject("availabilityId", availabilityId);
            final User riderEx = WebAuthUtils.requireUser(currentUser);
            mav.addObject("riderForename", riderEx.getForename());
            mav.addObject("riderSurname", riderEx.getSurname());
            mav.addObject("riderEmail", riderEx.getEmail());
            addReservationPricingToModel(mav, carId, form.getFromDateTime(), form.getUntilDateTime(), reservationTotal);
            wallDateTimeUiFormatter.addReservationFormDateDisplays(mav, form);
            addReservationPolicyHours(mav);
            addReservationFormRiderDocs(mav, riderEx);
            return mav;
        }

        final User riderDone = WebAuthUtils.requireUser(currentUser);
        final ModelAndView mav = new ModelAndView("reservation/reservationConfirmation");
        mav.addObject("carName", form.getCarName());
        mav.addObject("name", riderDone.getForename());
        mav.addObject("surname", riderDone.getSurname());
        mav.addObject("email", riderDone.getEmail());
        mav.addObject("fromDateTime", form.getFromDateTime());
        mav.addObject("untilDateTime", form.getUntilDateTime());
        final String confirmLoc = resolveCarHandoverLocation(carId, form.getFromDateTime());
        mav.addObject("deliveryLocation", confirmLoc == null || confirmLoc.isBlank() ? "" : confirmLoc);
        mav.addObject("reservationId", reservation.getId());
        mav.addObject("carId", carId);
        mav.addObject("availabilityId", availabilityId);
        mav.addObject("reservationTotal", ArsMoneyFormat.format(reservation.getTotalPrice()));
        mav.addObject("ownerCbu", userService.findOwnerCbuForCar(carId).orElse(""));
        wallDateTimeUiFormatter.addReservationFormDateDisplays(mav, form);
        addReservationPolicyHours(mav);
        mav.addObject("uploadMaxImageBytes", imageService.getMaxImageBytes());
        mav.addObject("uploadMaxImageMegabytes", imageService.getMaxImageMegabytesRoundedUp());
        return mav;
    }

    /**
     * Pickup/return address shown on the reservation summary, before any payment has been uploaded.
     * Uses the rider's chosen pickup date (not "today") to pick the effective availability row, and
     * falls back to the most recent row when the rider has not picked a valid date yet or the chosen
     * date is outside any published window. The address is rendered without the door number — this
     * matches {@link ListingAddressFormatter}'s sensitive-number policy (see
     * {@code formatPickupForReservationView}): the exact number is only revealed once the rider has
     * uploaded the payment receipt.
     */
    private String resolveCarHandoverLocation(final long carId, final String fromDateTime) {
        final LocalDate pickupDay = WallDateTimeParsing.parseWallLocalDateTimeToWallDate(fromDateTime);
        Optional<ListingAvailability> av = (pickupDay == null)
                ? Optional.empty()
                : listingAvailabilityService.findEffectiveForDayByCar(carId, pickupDay);
        if (av.isEmpty()) {
            av = listingAvailabilityService.findMostRecentByCarId(carId);
        }
        return av.map(listingAddressFormatter::formatPublicPickupLocation).orElse("");
    }

    private void addReservationPolicyHours(final ModelAndView mav) {
        mav.addObject("paymentProofUploadDeadlineHours", reservationService.getConfiguredPaymentProofDeadlineHours());
        mav.addObject("maxReservationBillableDays", reservationService.getConfiguredMaxReservationBillableDays());
    }

    private void addReservationPricingToModel(
            final ModelAndView mav,
            final long carId,
            final String fromDateTime,
            final String untilDateTime,
            final String reservationTotal) {
        mav.addObject(
                "clientReservationTotal",
                reservationService.normalizeClientReservationTotal(reservationTotal).orElse(null));
        mav.addObject(
                "reservationTotal",
                reservationService.reservationTotalDisplayByCar(carId, fromDateTime, untilDateTime).orElse(null));
    }

    private ModelAndView redirectToSearch() {
        return new ModelAndView(new RedirectView("/search", true));
    }

    private void addReservationFormRiderDocs(final ModelAndView mav, final User riderPrincipal) {
        final User riderRow = userService.getUserById(riderPrincipal.getId()).orElse(riderPrincipal);
        mav.addObject("riderHasBookingDocuments", userService.hasUploadedLicenseAndIdentity(riderRow));
        mav.addObject("riderMissingLicenseDocument", riderRow.getLicenseFileId().isEmpty());
        mav.addObject("riderMissingIdentityDocument", riderRow.getIdentityFileId().isEmpty());
        mav.addObject("uploadMaxProfileDocumentMegabytes", profileDocumentUploadPolicy.getMaxMegabytesRoundedUp());
    }

    private void uploadBookingDocument(
            final long userId, final UserDocumentType documentType, final MultipartFile file) throws IOException {
        userService.uploadValidatedProfileDocument(
                userId, documentType, file.getOriginalFilename(), file.getContentType(), file.getBytes());
    }

    private static boolean isMissingOrEmpty(final MultipartFile file) {
        return file == null || file.isEmpty();
    }
}
