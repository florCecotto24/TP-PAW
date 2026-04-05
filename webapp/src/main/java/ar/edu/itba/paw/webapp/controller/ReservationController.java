package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.exception.MessageKeys;
import ar.edu.itba.paw.exception.reservation.ReservationException;
import ar.edu.itba.paw.models.Listing;
import ar.edu.itba.paw.models.Reservation;
import ar.edu.itba.paw.services.ListingService;
import ar.edu.itba.paw.services.ReservationService;
import ar.edu.itba.paw.webapp.support.LocaleMessages;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import java.util.Optional;

@Controller
public class ReservationController {

    private final ReservationService reservationService;
    private final ListingService listingService;
    private final LocaleMessages localeMessages;

    @Autowired
    public ReservationController(
            final ReservationService reservationService,
            final ListingService listingService,
            final LocaleMessages localeMessages) {
        this.reservationService = reservationService;
        this.listingService = listingService;
        this.localeMessages = localeMessages;
    }

    @RequestMapping(value = "/reservation/new", method = RequestMethod.GET)
    public ModelAndView newReservation(@RequestParam(value = "listingId", required = false) final Long listingId,
                                       @RequestParam(value = "availabilityId", required = false) final Long availabilityId,
                                       @RequestParam(value = "carName", required = false) final String carName,
                                       @RequestParam(value = "fromDateTime", required = false) final String fromDateTime,
                                       @RequestParam(value = "untilDateTime", required = false) final String untilDateTime,
                                       @RequestParam(value = "reservationTotal", required = false) final String reservationTotal) {
        final String clientReservationTotal = reservationService
                .normalizeClientReservationTotal(reservationTotal)
                .orElse(null);
        final Optional<String> serverReservationTotal =
                reservationService.reservationTotalDisplay(listingId, fromDateTime, untilDateTime);

        final Optional<Listing> listingOpt = listingId == null ? Optional.empty() : listingService.getListingById(listingId);
        final ModelAndView mav = new ModelAndView("reservationForm");
        mav.addObject("listingId", listingId);
        mav.addObject("availabilityId", availabilityId);
        mav.addObject("carName", carName);
        mav.addObject("fromDateTime", fromDateTime);
        mav.addObject("untilDateTime", untilDateTime);
        mav.addObject("deliveryLocation", listingOpt.map(Listing::getStartPoint).orElse(null));
        mav.addObject("reservationTotal", serverReservationTotal.orElse(null));
        mav.addObject("clientReservationTotal", clientReservationTotal);
        return mav;
    }

    @RequestMapping(value = "/reservation", method = RequestMethod.POST)
    public ModelAndView createReservation(@RequestParam("email") final String email,
                                          @RequestParam("name") final String name,
                                          @RequestParam("surname") final String surname,
                                          @RequestParam(value = "listingId", required = false) final Long listingId,
                                          @RequestParam(value = "availabilityId", required = false) final Long availabilityId,
                                          @RequestParam(value = "carName", required = false) final String carName,
                                          @RequestParam(value = "fromDateTime", required = false) final String fromDateTime,
                                          @RequestParam(value = "untilDateTime", required = false) final String untilDateTime,
                                          @RequestParam(value = "reservationTotal", required = false) final String reservationTotal) {
        final String clientReservationTotal = reservationService
                .normalizeClientReservationTotal(reservationTotal)
                .orElse(null);

        final Optional<Listing> listingOpt = listingId == null ? Optional.empty() : listingService.getListingById(listingId);
        final String deliveryLocation = listingOpt.map(Listing::getStartPoint).orElse(null);

        if (carName == null || carName.isBlank()) {
            return reservationFormWithError(
                    listingId,
                    availabilityId,
                    carName,
                    fromDateTime,
                    untilDateTime,
                    clientReservationTotal,
                    localeMessages.msg(MessageKeys.RESERVATION_FORM_CAR_NAME_REQUIRED));
        }

        final Reservation reservation;
        try {
            reservation = reservationService.submitRiderReservation(
                    email, name, surname, listingId, availabilityId, fromDateTime, untilDateTime);
        } catch (final ReservationException e) {
            return reservationFormWithError(
                    listingId,
                    availabilityId,
                    carName,
                    fromDateTime,
                    untilDateTime,
                    clientReservationTotal,
                    localeMessages.msg(e));
        }

        final ModelAndView mav = new ModelAndView("reservationConfirmation");
        mav.addObject("reservationId", reservation.getId());
        mav.addObject("email", email);
        mav.addObject("name", name);
        mav.addObject("surname", surname);
        mav.addObject("listingId", listingId);
        mav.addObject("availabilityId", availabilityId);
        mav.addObject("carName", carName);
        mav.addObject("fromDateTime", fromDateTime);
        mav.addObject("untilDateTime", untilDateTime);
        mav.addObject("deliveryLocation", deliveryLocation);
        return mav;
    }

    private ModelAndView reservationFormWithError(
            final Long listingId,
            final Long availabilityId,
            final String carName,
            final String fromDateTime,
            final String untilDateTime,
            final String clientReservationTotal,
            final String errorMessage) {
        final Optional<Listing> listingOpt = listingId == null ? Optional.empty() : listingService.getListingById(listingId);
        final ModelAndView mav = new ModelAndView("reservationForm");
        mav.addObject("listingId", listingId);
        mav.addObject("availabilityId", availabilityId);
        mav.addObject("carName", carName);
        mav.addObject("fromDateTime", fromDateTime);
        mav.addObject("untilDateTime", untilDateTime);
        mav.addObject("deliveryLocation", listingOpt.map(Listing::getStartPoint).orElse(null));
        mav.addObject(
                "reservationTotal",
                reservationService.reservationTotalDisplay(listingId, fromDateTime, untilDateTime).orElse(null));
        mav.addObject("clientReservationTotal", clientReservationTotal);
        mav.addObject("reservationError", errorMessage);
        return mav;
    }
}
