package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.exception.MessageKeys;
import ar.edu.itba.paw.exception.reservation.ReservationException;
import ar.edu.itba.paw.models.domain.Listing;
import ar.edu.itba.paw.models.domain.Reservation;
import ar.edu.itba.paw.models.domain.User;
import ar.edu.itba.paw.services.ImageService;
import ar.edu.itba.paw.services.ListingService;
import ar.edu.itba.paw.services.ReservationService;
import ar.edu.itba.paw.webapp.support.CurrentUser;
import ar.edu.itba.paw.webapp.form.ReservationForm;
import ar.edu.itba.paw.webapp.util.LocaleMessages;
import ar.edu.itba.paw.webapp.util.WallDateTimeUiFormatter;
import ar.edu.itba.paw.webapp.util.WebAuthUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import java.util.Optional;

import ar.edu.itba.paw.webapp.validation.ValidationGroups;

@Controller
@RequestMapping("/reservation")
public class ReservationFormController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReservationFormController.class);


    private final ListingService listingService;
    private final ReservationService reservationService;
    private final ImageService imageService;
    private final LocaleMessages localeMessages;
    private final WallDateTimeUiFormatter wallDateTimeUiFormatter;

    @Autowired
    public ReservationFormController(
            final ListingService listingService,
            final ReservationService reservationService,
            final ImageService imageService,
            final LocaleMessages localeMessages,
            final WallDateTimeUiFormatter wallDateTimeUiFormatter) {
        this.listingService = listingService;
        this.reservationService = reservationService;
        this.imageService = imageService;
        this.localeMessages = localeMessages;
        this.wallDateTimeUiFormatter = wallDateTimeUiFormatter;
    }

    @GetMapping("/new")
    public ModelAndView index(
            @CurrentUser final User currentUser,
            @RequestParam(name = "listingId") final long listingId,
            @RequestParam(value = "availabilityId", required = false) final Long availabilityId,
            @RequestParam(value = "carName", required = false) final String carName,
            @RequestParam(value = "fromDateTime", required = false) final String fromDateTime,
            @RequestParam(value = "untilDateTime", required = false) final String untilDateTime,
            @RequestParam(value = "reservationTotal", required = false) final String reservationTotal,
            @ModelAttribute("reservationForm") final ReservationForm form) {
        final Optional<Listing> listingOpt = listingService.getListingById(listingId);
        if (listingOpt.isEmpty()) {
            return redirectToSearch();
        }
        final Listing listing = listingOpt.get();
        form.setListingId(listingId);
        form.setCarName(carName != null && !carName.isBlank() ? carName : listing.getTitle());
        String loc = listingService.formatPublicPickupLocation(listing);
        if (loc == null || loc.isBlank()) {
            loc = listing.getStartPointStreet();
        }
        form.setDeliveryLocation(loc);
        form.setFromDateTime(fromDateTime);
        form.setUntilDateTime(untilDateTime);

        final ModelAndView mav = new ModelAndView("reservationForm");
        mav.addObject("availabilityId", availabilityId);
        final User rider = WebAuthUtils.requireUser(currentUser);
        mav.addObject("riderForename", rider.getForename());
        mav.addObject("riderSurname", rider.getSurname());
        mav.addObject("riderEmail", rider.getEmail());
        addReservationPricingToModel(mav, listingId, fromDateTime, untilDateTime, reservationTotal);
        wallDateTimeUiFormatter.addReservationFormDateDisplays(mav, form);
        addReservationPolicyHours(mav);
        return mav;
    }

    @PostMapping
    public ModelAndView formSubmit(
            @CurrentUser final User currentUser,
            @Validated(ValidationGroups.OnReservationSubmit.class) @ModelAttribute("reservationForm") final ReservationForm form,
            final BindingResult errors,
            @RequestParam(value = "availabilityId", required = false) final Long availabilityId,
            @RequestParam(value = "reservationTotal", required = false) final String reservationTotal) {

        if (form.getListingId() == null) {
            return redirectToSearch();
        }
        final long listingId = form.getListingId();

        final Optional<Listing> listingOpt = listingService.getListingById(listingId);
        if (listingOpt.isEmpty()) {
            return redirectToSearch();
        }

        if (errors.hasErrors()) {
            final ModelAndView mav = new ModelAndView("reservationForm");
            mav.addObject("availabilityId", availabilityId);
            final User riderErr = WebAuthUtils.requireUser(currentUser);
            mav.addObject("riderForename", riderErr.getForename());
            mav.addObject("riderSurname", riderErr.getSurname());
            mav.addObject("riderEmail", riderErr.getEmail());
            addReservationPricingToModel(mav, listingId, form.getFromDateTime(), form.getUntilDateTime(), reservationTotal);
            wallDateTimeUiFormatter.addReservationFormDateDisplays(mav, form);
            addReservationPolicyHours(mav);
            return mav;
        }

        if (form.getCarName() == null || form.getCarName().isBlank()) {
            final ModelAndView mav = new ModelAndView("reservationForm");
            mav.addObject("reservationError", localeMessages.msg(MessageKeys.RESERVATION_FORM_CAR_NAME_REQUIRED));
            mav.addObject("availabilityId", availabilityId);
            final User riderCar = WebAuthUtils.requireUser(currentUser);
            mav.addObject("riderForename", riderCar.getForename());
            mav.addObject("riderSurname", riderCar.getSurname());
            mav.addObject("riderEmail", riderCar.getEmail());
            addReservationPricingToModel(mav, listingId, form.getFromDateTime(), form.getUntilDateTime(), reservationTotal);
            wallDateTimeUiFormatter.addReservationFormDateDisplays(mav, form);
            addReservationPolicyHours(mav);
            return mav;
        }

        final Reservation reservation;
        try {
            final long riderId = WebAuthUtils.requireUser(currentUser).getId();

            LOGGER.atInfo().log("Submitting reservation for riderId={}, listingId={}, availabilityId={}, " +
                            "fromDateTime={}, untilDateTime={}",
                    riderId, listingId, availabilityId, form.getFromDateTime(), form.getUntilDateTime());

            reservation = reservationService.submitRiderReservation(
                    riderId,
                    listingId,
                    availabilityId,
                    form.getFromDateTime(),
                    form.getUntilDateTime());
        } catch (final ReservationException e) {
            final ModelAndView mav = new ModelAndView("reservationForm");
            mav.addObject("reservationError", localeMessages.msg(e));
            mav.addObject("availabilityId", availabilityId);
            final User riderEx = WebAuthUtils.requireUser(currentUser);
            mav.addObject("riderForename", riderEx.getForename());
            mav.addObject("riderSurname", riderEx.getSurname());
            mav.addObject("riderEmail", riderEx.getEmail());
            addReservationPricingToModel(mav, listingId, form.getFromDateTime(), form.getUntilDateTime(), reservationTotal);
            wallDateTimeUiFormatter.addReservationFormDateDisplays(mav, form);
            addReservationPolicyHours(mav);
            return mav;
        }

        final User riderDone = WebAuthUtils.requireUser(currentUser);
        final ModelAndView mav = new ModelAndView("reservationConfirmation");
        mav.addObject("carName", form.getCarName());
        mav.addObject("name", riderDone.getForename());
        mav.addObject("surname", riderDone.getSurname());
        mav.addObject("email", riderDone.getEmail());
        mav.addObject("fromDateTime", form.getFromDateTime());
        mav.addObject("untilDateTime", form.getUntilDateTime());
        final String confirmLoc = listingService.formatRiderReservationHandoverSummary(listingOpt.get(), reservation);
        mav.addObject("deliveryLocation", confirmLoc == null || confirmLoc.isBlank() ? "" : confirmLoc);
        mav.addObject("reservationId", reservation.getId());
        mav.addObject("listingId", listingId);
        mav.addObject("availabilityId", availabilityId);
        wallDateTimeUiFormatter.addReservationFormDateDisplays(mav, form);
        addReservationPolicyHours(mav);
        mav.addObject("uploadMaxImageBytes", imageService.getMaxImageBytes());
        mav.addObject("uploadMaxImageMegabytes", imageService.getMaxImageMegabytesRoundedUp());
        return mav;
    }

    private void addReservationPolicyHours(final ModelAndView mav) {
        mav.addObject("paymentProofUploadDeadlineHours", reservationService.getConfiguredPaymentProofDeadlineHours());
        mav.addObject("maxReservationBillableDays", reservationService.getConfiguredMaxReservationBillableDays());
    }

    private void addReservationPricingToModel(
            final ModelAndView mav,
            final long listingId,
            final String fromDateTime,
            final String untilDateTime,
            final String reservationTotal) {
        mav.addObject(
                "clientReservationTotal",
                reservationService.normalizeClientReservationTotal(reservationTotal).orElse(null));
        mav.addObject(
                "reservationTotal",
                reservationService.reservationTotalDisplay(listingId, fromDateTime, untilDateTime).orElse(null));
    }

    private ModelAndView redirectToSearch() {
        return new ModelAndView(new RedirectView("/search", true));
    }
}
