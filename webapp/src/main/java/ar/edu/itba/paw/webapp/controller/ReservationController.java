package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.services.ListingService;
import ar.edu.itba.paw.services.ReservationService;
import ar.edu.itba.paw.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;

@Controller
public class ReservationController {

    private static final ZoneOffset RESERVATION_ZONE = ZoneOffset.UTC;

    private final ReservationService reservationService;
    private final UserService userService;
    private final ListingService listingService;

    @Autowired
    public ReservationController(
            final ReservationService reservationService,
            final UserService userService,
            final ListingService listingService) {
        this.reservationService = reservationService;
        this.userService = userService;
        this.listingService = listingService;
    }

    @RequestMapping(value = "/reservation/new", method = RequestMethod.GET)
    public ModelAndView newReservation(
            @RequestParam(value = "listingId", defaultValue = "1") final long listingId,
            @RequestParam(value = "carName", required = false) final String carName,
            @RequestParam(value = "fromDate", required = false) final String fromDate,
            @RequestParam(value = "untilDate", required = false) final String untilDate,
            @RequestParam(value = "deliveryLocation", required = false) final String deliveryLocation) {
        final ModelAndView mav = new ModelAndView("reservationForm");
        mav.addObject("listingId", listingId);
        mav.addObject("carName", carName == null || carName.isBlank() ? "Mercedes-Benz E-Class 300" : carName);
        mav.addObject("fromDate", fromDate);
        mav.addObject("untilDate", untilDate);
        mav.addObject("deliveryLocation", deliveryLocation);
        return mav;
    }

    @RequestMapping(value = "/reservation", method = RequestMethod.POST)
    public ModelAndView createReservation(
            @RequestParam("email") final String email,
            @RequestParam("name") final String name,
            @RequestParam("surname") final String surname,
            @RequestParam(value = "listingId", defaultValue = "1") final long listingId,
            @RequestParam(value = "carName", required = false) final String carName,
            @RequestParam(value = "fromDate", required = false) final String fromDate,
            @RequestParam(value = "untilDate", required = false) final String untilDate,
            @RequestParam(value = "deliveryLocation", required = false) final String deliveryLocation) {
        final String resolvedCarName = carName == null || carName.isBlank() ? "Mercedes-Benz E-Class 300" : carName;
        final String delivery = deliveryLocation == null ? "" : deliveryLocation.trim();

        if (listingService.getListingById(listingId).isEmpty()) {
            return formWithError(
                    listingId,
                    resolvedCarName,
                    fromDate,
                    untilDate,
                    deliveryLocation,
                    "No existe la publicación indicada (listing " + listingId + ").");
        }
        if (fromDate == null || fromDate.isBlank() || untilDate == null || untilDate.isBlank()) {
            return formWithError(
                    listingId,
                    resolvedCarName,
                    fromDate,
                    untilDate,
                    deliveryLocation,
                    "Indicá fecha de inicio y fecha de fin.");
        }

        final OffsetDateTime start;
        final OffsetDateTime end;
        try {
            final LocalDate from = LocalDate.parse(fromDate.trim());
            final LocalDate until = LocalDate.parse(untilDate.trim());
            if (until.isBefore(from)) {
                return formWithError(
                        listingId,
                        resolvedCarName,
                        fromDate,
                        untilDate,
                        deliveryLocation,
                        "La fecha de fin no puede ser anterior a la de inicio.");
            }
            start = from.atTime(LocalTime.MIDNIGHT).atOffset(RESERVATION_ZONE);
            end = until.atTime(LocalTime.of(23, 59, 59)).atOffset(RESERVATION_ZONE);
        } catch (final DateTimeParseException e) {
            return formWithError(
                    listingId,
                    resolvedCarName,
                    fromDate,
                    untilDate,
                    deliveryLocation,
                    "Las fechas no tienen un formato válido (usá el selector de fechas).");
        }

        final String riderName = (name == null ? "" : name.trim()) + " " + (surname == null ? "" : surname.trim());
        final User rider = userService.getOrCreateUser(email.trim(), riderName.trim());

        reservationService.createReservation(rider.getId(), listingId, start, end, delivery);

        final ModelAndView mav = new ModelAndView("reservationConfirmation");
        mav.addObject("email", email.trim());
        mav.addObject("name", name == null ? "" : name.trim());
        mav.addObject("surname", surname == null ? "" : surname.trim());
        mav.addObject("carName", resolvedCarName);
        mav.addObject("fromDate", fromDate);
        mav.addObject("untilDate", untilDate);
        mav.addObject("deliveryLocation", delivery);
        return mav;
    }

    private ModelAndView formWithError(
            final long listingId,
            final String carName,
            final String fromDate,
            final String untilDate,
            final String deliveryLocation,
            final String errorMessage) {
        final ModelAndView mav = new ModelAndView("reservationForm");
        mav.addObject("listingId", listingId);
        mav.addObject("carName", carName);
        mav.addObject("fromDate", fromDate);
        mav.addObject("untilDate", untilDate);
        mav.addObject("deliveryLocation", deliveryLocation);
        mav.addObject("errorMessage", errorMessage);
        return mav;
    }
}
