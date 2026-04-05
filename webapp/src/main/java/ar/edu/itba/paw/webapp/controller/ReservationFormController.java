package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.models.AvailabilityPeriod;
import ar.edu.itba.paw.models.Listing;
import ar.edu.itba.paw.models.Reservation;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.services.ListingService;
import ar.edu.itba.paw.services.ReservationConflictException;
import ar.edu.itba.paw.services.ReservationService;
import ar.edu.itba.paw.services.UserService;
import ar.edu.itba.paw.webapp.form.ReservationForm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import javax.validation.Valid;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.Optional;

@Controller
@RequestMapping("/reservation")
public class ReservationFormController {

    private final ListingService listingService;
    private final ReservationService reservationService;
    private final UserService userService;

    @Autowired
    public ReservationFormController(
            final ListingService listingService,
            final ReservationService reservationService,
            final UserService userService) {
        this.listingService = listingService;
        this.reservationService = reservationService;
        this.userService = userService;
    }

    @GetMapping("/new")
    public ModelAndView index(@RequestParam(name = "listingId") final long listingId,
                              @RequestParam(value = "fromDateTime", required = false) final String fromDateTime,
                              @RequestParam(value = "untilDateTime", required = false) final String untilDateTime,
                              @ModelAttribute("reservationForm") final ReservationForm form) {
        final Optional<Listing> listingOpt = listingService.getListingById(listingId);
        if (listingOpt.isEmpty()) {
            return new ModelAndView("redirect:/search");
        }

        final Listing listing = listingOpt.get();
        form.setListingId(listingId);
        form.setCarName(listing.getTitle());
        form.setDeliveryLocation(listing.getStartPoint());
        form.setFromDateTime(fromDateTime);
        form.setUntilDateTime(untilDateTime);

        return new ModelAndView("reservationForm");
    }

    @PostMapping
    public ModelAndView formSubmit(@Valid @ModelAttribute("reservationForm") final ReservationForm form,
            final BindingResult errors) {

        if (form.getListingId() == null) {
            return redirectToSearch();
        }
        final long listingId = form.getListingId();

        final Optional<Listing> listingOpt = listingService.getListingById(listingId);
        if (listingOpt.isEmpty()) {
            return redirectToSearch();
        }

        final Listing listing = listingOpt.get();

        if (errors.hasErrors()) {
            return new ModelAndView("reservationForm");
        }

        final User rider = userService.findOrCreatePublisher(form.getEmail(), form.getName(), form.getSurname());
        final OffsetDateTime startDate;
        final OffsetDateTime endDate;
        try {
            startDate = parseWallDateTimeToUtc(form.getFromDateTime());
            endDate = parseWallDateTimeToUtc(form.getUntilDateTime());
        } catch (final DateTimeParseException e) {
            errors.reject("reservation.datetime.invalid", "Invalid date and time format. Please use the correct format.");
            System.out.println("Invalid date format: " + e.getMessage());
            return new ModelAndView("reservationForm");
        }

        try {
            reservationService.createReservation(
                    rider.getId(),
                    listing.getId(),
                    startDate,
                    endDate,
                    Reservation.Status.ACCEPTED,
                    form.getDeliveryLocation());
        } catch (final ReservationConflictException e) {
            errors.reject("reservation.conflict", e.getMessage());
            return new ModelAndView("reservationForm");
        }

        final ModelAndView mav = new ModelAndView("reservationConfirmation");
        mav.addObject("carName", form.getCarName());
        mav.addObject("name", form.getName());
        mav.addObject("surname", form.getSurname());
        mav.addObject("email", form.getEmail());
        mav.addObject("fromDateTime", startDate);
        mav.addObject("untilDateTime", endDate);
        mav.addObject("deliveryLocation", listing.getStartPoint());
        return mav;
    }

    private ModelAndView redirectToSearch() {
        return new ModelAndView(new RedirectView("/search", true));
    }

    private static OffsetDateTime parseWallDateTimeToUtc(final String value) {
        final LocalDateTime localDateTime = LocalDateTime.parse(value);
        return localDateTime.atZone(AvailabilityPeriod.WALL_ZONE).toInstant().atOffset(ZoneOffset.UTC);
    }
}