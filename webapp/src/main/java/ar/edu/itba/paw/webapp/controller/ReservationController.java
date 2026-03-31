package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.models.AvailabilityPeriod;
import ar.edu.itba.paw.models.ListingAvailability;
import ar.edu.itba.paw.models.Reservation;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.services.ListingService;
import ar.edu.itba.paw.services.ReservationConflictException;
import ar.edu.itba.paw.services.ReservationService;
import ar.edu.itba.paw.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;

@Controller
public class ReservationController {

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
    public ModelAndView newReservation(@RequestParam(value = "listingId", required = false) final Long listingId,
                                       @RequestParam(value = "availabilityId", required = false) final Long availabilityId,
                                       @RequestParam(value = "carName", required = false) final String carName,
                                       @RequestParam(value = "fromDateTime", required = false) final String fromDateTime,
                                       @RequestParam(value = "untilDateTime", required = false) final String untilDateTime,
                                       @RequestParam(value = "deliveryLocation", required = false) final String deliveryLocation) {
        final ModelAndView mav = new ModelAndView("reservationForm");
        mav.addObject("listingId", listingId);
        mav.addObject("availabilityId", availabilityId);
        mav.addObject("carName", carName == null || carName.isBlank() ? "Mercedes-Benz E-Class 300" : carName);
        mav.addObject("fromDateTime", fromDateTime);
        mav.addObject("untilDateTime", untilDateTime);
        mav.addObject("deliveryLocation", deliveryLocation);
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
                                          @RequestParam(value = "deliveryLocation", required = false) final String deliveryLocation) {
        if (listingId == null || listingService.getListingById(listingId).isEmpty()) {
            return reservationFormWithError(
                    listingId,
                    availabilityId,
                    carName,
                    fromDateTime,
                    untilDateTime,
                    deliveryLocation,
                    "We could not find the listing you are trying to reserve.");
        }

        if (isBlank(fromDateTime) || isBlank(untilDateTime)) {
            return reservationFormWithError(
                    listingId,
                    availabilityId,
                    carName,
                    fromDateTime,
                    untilDateTime,
                    deliveryLocation,
                    "Select pickup and return date/time before confirming.");
        }

        final OffsetDateTime startDate;
        final OffsetDateTime endDate;
        try {
            startDate = parseWallDateTimeToUtc(fromDateTime);
            endDate = parseWallDateTimeToUtc(untilDateTime);
        } catch (final DateTimeParseException e) {
            return reservationFormWithError(
                    listingId,
                    availabilityId,
                    carName,
                    fromDateTime,
                    untilDateTime,
                    deliveryLocation,
                    "Invalid date/time format. Please choose the dates again.");
        }

        if (!endDate.isAfter(startDate)) {
            return reservationFormWithError(
                    listingId,
                    availabilityId,
                    carName,
                    fromDateTime,
                    untilDateTime,
                    deliveryLocation,
                    "Return date/time must be after pickup date/time.");
        }

        if (!fitsAvailabilityWindow(listingId, availabilityId, startDate, endDate)) {
            return reservationFormWithError(
                    listingId,
                    availabilityId,
                    carName,
                    fromDateTime,
                    untilDateTime,
                    deliveryLocation,
                    "Selected pickup/return is outside the listing availability.");
        }

        final User rider = userService.findOrCreatePublisher(email, name, surname);
        final Reservation reservation;
        try {
            reservation = reservationService.createReservation(
                    rider.getId(),
                    listingId,
                    startDate,
                    endDate,
                    Reservation.Status.ACCEPTED,
                    deliveryLocation);
        } catch (final ReservationConflictException e) {
            return reservationFormWithError(
                    listingId,
                    availabilityId,
                    carName,
                    fromDateTime,
                    untilDateTime,
                    deliveryLocation,
                    e.getMessage());
        }

        final ModelAndView mav = new ModelAndView("reservationConfirmation");
        mav.addObject("reservationId", reservation.getId());
        mav.addObject("email", email);
        mav.addObject("name", name);
        mav.addObject("surname", surname);
        mav.addObject("listingId", listingId);
        mav.addObject("availabilityId", availabilityId);
        mav.addObject("carName", carName == null || carName.isBlank() ? "Mercedes-Benz E-Class 300" : carName);
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
            final String deliveryLocation,
            final String errorMessage) {
        final ModelAndView mav = new ModelAndView("reservationForm");
        mav.addObject("listingId", listingId);
        mav.addObject("availabilityId", availabilityId);
        mav.addObject("carName", carName == null || carName.isBlank() ? "Mercedes-Benz E-Class 300" : carName);
        mav.addObject("fromDateTime", fromDateTime);
        mav.addObject("untilDateTime", untilDateTime);
        mav.addObject("deliveryLocation", deliveryLocation);
        mav.addObject("reservationError", errorMessage);
        return mav;
    }

    private boolean fitsAvailabilityWindow(
            final long listingId,
            final Long availabilityId,
            final OffsetDateTime startDate,
            final OffsetDateTime endDate) {
        final List<ListingAvailability> availabilities = listingService.findAvailabilityByListingId(listingId);
        if (availabilities.isEmpty()) {
            return false;
        }

        if (availabilityId != null) {
            final Optional<ListingAvailability> selected = availabilities.stream()
                    .filter(a -> a.getId() == availabilityId)
                    .findFirst();
            return selected.isPresent() && isInsideWindow(selected.get(), startDate, endDate);
        }

        return availabilities.stream().anyMatch(a -> isInsideWindow(a, startDate, endDate));
    }

    private static boolean isInsideWindow(
            final ListingAvailability availability,
            final OffsetDateTime startDate,
            final OffsetDateTime endDate) {
        return !startDate.isBefore(availability.getStartDate()) && !endDate.isAfter(availability.getEndDate());
    }

    private static OffsetDateTime parseWallDateTimeToUtc(final String value) {
        final LocalDateTime localDateTime = LocalDateTime.parse(value);
        return localDateTime.atZone(AvailabilityPeriod.WALL_ZONE).toInstant().atOffset(ZoneOffset.UTC);
    }

    private static boolean isBlank(final String value) {
        return value == null || value.trim().isEmpty();
    }
}

