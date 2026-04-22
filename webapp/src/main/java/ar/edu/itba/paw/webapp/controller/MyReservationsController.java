package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.models.ListingDetail;
import ar.edu.itba.paw.models.Page;
import ar.edu.itba.paw.models.Reservation;
import ar.edu.itba.paw.models.ReservationCard;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.models.WallDateTimeDisplayFormat;
import ar.edu.itba.paw.services.ListingService;
import ar.edu.itba.paw.services.ReservationService;
import ar.edu.itba.paw.webapp.dto.ReservationCardView;
import ar.edu.itba.paw.webapp.util.WebAuthUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
public class MyReservationsController {

    private static final int PAGE_SIZE = 8;

    private final ReservationService reservationService;
    private final ListingService listingService;

    @Autowired
    public MyReservationsController(
            final ReservationService reservationService,
            final ListingService listingService) {
        this.reservationService = reservationService;
        this.listingService = listingService;
    }

    @GetMapping("/my-reservations")
    public ModelAndView myReservations(
            @ModelAttribute(name = LoggedUserAdvice.CURRENT_USER_MODEL_KEY, binding = false) final User currentUser,
            @RequestParam(defaultValue = "0") int riderPage,
            @RequestParam(defaultValue = "0") int ownerPage) {
        final User me = WebAuthUtils.requireUser(currentUser);
        riderPage = Math.max(0, riderPage);
        ownerPage = Math.max(0, ownerPage);

        // Rider reservations (reservations made BY the user)
        final Page<ReservationCard> riderResultPage = reservationService.getRiderReservationCards(me.getId(), riderPage, PAGE_SIZE);
        final Locale locale = LocaleContextHolder.getLocale();
        final List<ReservationCardView> riderReservations = riderResultPage.getContent().stream()
                .map(card -> toReservationCardView(card, locale))
                .collect(Collectors.toList());

        // Owner reservations (reservations made ON the user's cars)
        final Page<ReservationCard> ownerResultPage = reservationService.getOwnerReservationCards(me.getId(), ownerPage, PAGE_SIZE);
        final List<ReservationCardView> ownerReservations = ownerResultPage.getContent().stream()
                .map(card -> toReservationCardView(card, locale))
                .collect(Collectors.toList());

        final ModelAndView mav = new ModelAndView("myReservations");
        mav.addObject("riderReservations", riderReservations);
        mav.addObject("riderReservationsPage", riderResultPage);
        mav.addObject("ownerReservations", ownerReservations);
        mav.addObject("ownerReservationsPage", ownerResultPage);
        mav.addObject("activeTab", "my-reservations");
        return mav;
    }

    @GetMapping("/my-reservations/{reservationId}")
    public ModelAndView reservationDetail(
            @ModelAttribute(name = LoggedUserAdvice.CURRENT_USER_MODEL_KEY, binding = false) final User currentUser,
            @PathVariable("reservationId") final long reservationId,
            @RequestParam(defaultValue = "rider") final String role) {
        final User me = WebAuthUtils.requireUser(currentUser);
        final Optional<Reservation> reservationOpt = "owner".equals(role)
                ? reservationService.getOwnerReservationById(me.getId(), reservationId)
                : reservationService.getRiderReservationById(me.getId(), reservationId);
        
        if (reservationOpt.isEmpty()) {
            return new ModelAndView(new RedirectView("/my-reservations", true));
        }

        final Reservation reservation = reservationOpt.get();
        final Optional<ListingDetail> listingDetailOpt = listingService.getListingDetailById(reservation.getListingId());
        if (listingDetailOpt.isEmpty()) {
            return new ModelAndView(new RedirectView("/my-reservations", true));
        }

        final ListingDetail listingDetail = listingDetailOpt.get();
        final Locale locale = LocaleContextHolder.getLocale();
        final String pickupDisplay = WallDateTimeDisplayFormat.formatUtcAsWallLocalNoSeconds(reservation.getStartDate(), locale);
        final String returnDisplay = WallDateTimeDisplayFormat.formatUtcAsWallLocalNoSeconds(reservation.getEndDate(), locale);
        final String totalPrice = reservation.getTotalPrice().toString();
        final long carImageId = listingDetail.getPictures().isEmpty() ? 0L : listingDetail.getPictures().get(0).getImageId();

        final ModelAndView mav = new ModelAndView("myReservationDetail");
        mav.addObject("reservation", reservation);
        mav.addObject("listing", listingDetail.getListing());
        mav.addObject("car", listingDetail.getCar());
        mav.addObject("owner", listingDetail.getOwner());
        mav.addObject("pickupDateTime", pickupDisplay);
        mav.addObject("returnDateTime", returnDisplay);
        mav.addObject("statusKey", reservation.getStatus().name().toLowerCase());
        mav.addObject("totalPrice", totalPrice);
        mav.addObject("carImageId", carImageId);
        mav.addObject("activeTab", "my-reservations");
        mav.addObject("reservationRole", role);
        return mav;
    }

    private ReservationCardView toReservationCardView(final ReservationCard card, final Locale locale) {
        final String pickupDisplay = WallDateTimeDisplayFormat.formatUtcAsWallLocalNoSeconds(card.getStartDate(), locale);
        final String returnDisplay = WallDateTimeDisplayFormat.formatUtcAsWallLocalNoSeconds(card.getEndDate(), locale);
        final String totalPrice = reservationService
                .calculateTotal(card.getListingId(), card.getStartDate(), card.getEndDate())
                .map(MyReservationsController::formatMoney)
                .orElse("-");
        return new ReservationCardView(
                card.getReservationId(),
                card.getListingId(),
                card.getImageId(),
                card.getBrand(),
                card.getModel(),
                pickupDisplay,
                returnDisplay,
                card.getStatus().name().toLowerCase(),
                totalPrice);
    }

    private static String formatMoney(final BigDecimal amount) {
        return amount.stripTrailingZeros().toPlainString();
    }

    @PostMapping("/my-reservations/{reservationId}/cancel")
    public ModelAndView cancelReservation(
            @ModelAttribute(name = LoggedUserAdvice.CURRENT_USER_MODEL_KEY, binding = false) final User currentUser,
            @PathVariable("reservationId") final long reservationId,
            @RequestParam(defaultValue = "rider") final String role) {
        final User me = WebAuthUtils.requireUser(currentUser);
        final Optional<Reservation> reservationOpt = "owner".equals(role)
                ? reservationService.getOwnerReservationById(me.getId(), reservationId)
                : reservationService.getRiderReservationById(me.getId(), reservationId);
        
        if (reservationOpt.isPresent()) {
            reservationService.cancelReservation(reservationId);
        }
        
        final ModelAndView mav = new ModelAndView("reservationCancelled");
        mav.addObject("reservationRole", role);
        return mav;
    }
}

