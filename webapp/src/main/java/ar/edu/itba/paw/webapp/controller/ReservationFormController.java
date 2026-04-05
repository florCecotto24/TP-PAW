package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.exception.MessageKeys;
import ar.edu.itba.paw.exception.reservation.ReservationException;
import ar.edu.itba.paw.models.Listing;
import ar.edu.itba.paw.models.Reservation;
import ar.edu.itba.paw.services.ListingService;
import ar.edu.itba.paw.services.ReservationService;
import ar.edu.itba.paw.webapp.form.ReservationForm;
import ar.edu.itba.paw.webapp.support.LocaleMessages;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import javax.validation.Valid;
import java.util.Optional;

@Controller
@RequestMapping("/reservation")
public class ReservationFormController {

    private final ListingService listingService;
    private final ReservationService reservationService;
    private final LocaleMessages localeMessages;

    @Autowired
    public ReservationFormController(
            final ListingService listingService,
            final ReservationService reservationService,
            final LocaleMessages localeMessages) {
        this.listingService = listingService;
        this.reservationService = reservationService;
        this.localeMessages = localeMessages;
    }

    @GetMapping("/new")
    public ModelAndView index(
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
        form.setDeliveryLocation(listing.getStartPoint());
        form.setFromDateTime(fromDateTime);
        form.setUntilDateTime(untilDateTime);

        final ModelAndView mav = new ModelAndView("reservationForm");
        mav.addObject("availabilityId", availabilityId);
        addReservationPricingToModel(mav, listingId, fromDateTime, untilDateTime, reservationTotal);
        return mav;
    }

    @PostMapping
    public ModelAndView formSubmit(
            @Valid @ModelAttribute("reservationForm") final ReservationForm form,
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
            addReservationPricingToModel(mav, listingId, form.getFromDateTime(), form.getUntilDateTime(), reservationTotal);
            return mav;
        }

        if (form.getCarName() == null || form.getCarName().isBlank()) {
            final ModelAndView mav = new ModelAndView("reservationForm");
            mav.addObject("reservationError", localeMessages.msg(MessageKeys.RESERVATION_FORM_CAR_NAME_REQUIRED));
            mav.addObject("availabilityId", availabilityId);
            addReservationPricingToModel(mav, listingId, form.getFromDateTime(), form.getUntilDateTime(), reservationTotal);
            return mav;
        }

        final Reservation reservation;
        try {
            reservation = reservationService.submitRiderReservation(
                    form.getEmail(),
                    form.getName(),
                    form.getSurname(),
                    listingId,
                    availabilityId,
                    form.getFromDateTime(),
                    form.getUntilDateTime());
        } catch (final ReservationException e) {
            final ModelAndView mav = new ModelAndView("reservationForm");
            mav.addObject("reservationError", localeMessages.msg(e));
            mav.addObject("availabilityId", availabilityId);
            addReservationPricingToModel(mav, listingId, form.getFromDateTime(), form.getUntilDateTime(), reservationTotal);
            return mav;
        }

        final ModelAndView mav = new ModelAndView("reservationConfirmation");
        mav.addObject("carName", form.getCarName());
        mav.addObject("name", form.getName());
        mav.addObject("surname", form.getSurname());
        mav.addObject("email", form.getEmail());
        mav.addObject("fromDateTime", form.getFromDateTime());
        mav.addObject("untilDateTime", form.getUntilDateTime());
        mav.addObject("deliveryLocation", listingOpt.get().getStartPoint());
        mav.addObject("reservationId", reservation.getId());
        mav.addObject("listingId", listingId);
        mav.addObject("availabilityId", availabilityId);
        return mav;
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
