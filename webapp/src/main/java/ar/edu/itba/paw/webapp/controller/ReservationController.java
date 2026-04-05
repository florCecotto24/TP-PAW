package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.models.Listing;
import ar.edu.itba.paw.models.Reservation;
import ar.edu.itba.paw.services.ListingService;
import ar.edu.itba.paw.services.ReservationConflictException;
import ar.edu.itba.paw.services.RiderReservationException;
import ar.edu.itba.paw.services.RiderReservationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import java.util.Optional;

@Controller
public class ReservationController {

    private static final String MSG_CAR_NAME_REQUIRED = "Vehicle name is required.";

    private final RiderReservationService riderReservationService;
    private final ListingService listingService;

    @Autowired
    public ReservationController(
            final RiderReservationService riderReservationService,
            final ListingService listingService) {
        this.riderReservationService = riderReservationService;
        this.listingService = listingService;
    }

    @RequestMapping(value = "/reservation/new", method = RequestMethod.GET)
    public ModelAndView newReservation(@RequestParam(value = "listingId", required = false) final Long listingId,
                                       @RequestParam(value = "availabilityId", required = false) final Long availabilityId,
                                       @RequestParam(value = "carName", required = false) final String carName,
                                       @RequestParam(value = "fromDateTime", required = false) final String fromDateTime,
                                       @RequestParam(value = "untilDateTime", required = false) final String untilDateTime,
                                       @RequestParam(value = "reservationTotal", required = false) final String reservationTotal) {
        final String clientReservationTotal = riderReservationService
                .normalizeClientReservationTotal(reservationTotal)
                .orElse(null);
        final Optional<String> serverReservationTotal =
                riderReservationService.reservationTotalDisplay(listingId, fromDateTime, untilDateTime);

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
        final String clientReservationTotal = riderReservationService
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
                    MSG_CAR_NAME_REQUIRED);
        }

        final Reservation reservation;
        try {
            reservation = riderReservationService.submitRiderReservation(
                    email, name, surname, listingId, availabilityId, fromDateTime, untilDateTime);
        } catch (final RiderReservationException | ReservationConflictException e) {
            return reservationFormWithError(
                    listingId,
                    availabilityId,
                    carName,
                    fromDateTime,
                    untilDateTime,
                    clientReservationTotal,
                    e.getMessage());
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
                riderReservationService.reservationTotalDisplay(listingId, fromDateTime, untilDateTime).orElse(null));
        mav.addObject("clientReservationTotal", clientReservationTotal);
        mav.addObject("reservationError", errorMessage);
        return mav;
    }
}
