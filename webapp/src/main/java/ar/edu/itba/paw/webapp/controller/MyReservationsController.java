package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.models.Listing;
import ar.edu.itba.paw.models.ListingDetail;
import ar.edu.itba.paw.models.Page;
import ar.edu.itba.paw.models.Reservation;
import ar.edu.itba.paw.models.ReservationCard;
import ar.edu.itba.paw.models.StoredFile;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.models.WallDateTimeDisplayFormat;
import ar.edu.itba.paw.exception.MessageKeys;
import ar.edu.itba.paw.exception.RydenException;
import ar.edu.itba.paw.services.ImageService;
import ar.edu.itba.paw.services.ListingService;
import ar.edu.itba.paw.services.ReservationService;
import ar.edu.itba.paw.webapp.dto.ReservationCardView;
import ar.edu.itba.paw.webapp.util.LocaleMessages;
import ar.edu.itba.paw.webapp.util.WebAuthUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
public class MyReservationsController {

    private static final int PAGE_SIZE = 8;
    private static final String TAB_RIDER = "rider";
    private static final String TAB_OWNER = "owner";
    private static final Set<String> RESERVATION_STATUS_WHITELIST =
            Set.of("pending", "accepted", "started", "cancelled", "finished");

    private final ReservationService reservationService;
    private final ListingService listingService;
    private final ImageService imageService;
    private final LocaleMessages localeMessages;

    @Autowired
    public MyReservationsController(
            final ReservationService reservationService,
            final ListingService listingService,
            final ImageService imageService,
            final LocaleMessages localeMessages) {
        this.reservationService = reservationService;
        this.listingService = listingService;
        this.imageService = imageService;
        this.localeMessages = localeMessages;
    }

    @GetMapping("/my-reservations")
    public ModelAndView myReservations(
            @ModelAttribute(name = LoggedUserAdvice.CURRENT_USER_MODEL_KEY, binding = false) final User currentUser,
            @RequestParam(defaultValue = TAB_RIDER) final String tab,
            @RequestParam(defaultValue = "0") int riderPage,
            @RequestParam(defaultValue = "0") int ownerPage,
            @RequestParam(required = false) final String riderStatus,
            @RequestParam(required = false) final String ownerStatus,
            final HttpServletRequest request) {
        final User me = WebAuthUtils.requireUser(currentUser);
        final String selectedTab = normalizeReservationsTab(tab);
        riderPage = Math.max(0, riderPage);
        ownerPage = Math.max(0, ownerPage);
        final String riderStatusFilter = normalizeReservationStatusParam(riderStatus);
        final String ownerStatusFilter = normalizeReservationStatusParam(ownerStatus);

        // Rider reservations (reservations made BY the user)
        final Page<ReservationCard> riderResultPage = reservationService.getRiderReservationCards(
                me.getId(), riderPage, PAGE_SIZE, riderStatusFilter);
        final Locale locale = LocaleContextHolder.getLocale();
        final List<ReservationCardView> riderReservations = riderResultPage.getContent().stream()
                .map(card -> toReservationCardView(card, locale))
                .collect(Collectors.toList());

        // Owner reservations (reservations made ON the user's cars)
        final Page<ReservationCard> ownerResultPage = reservationService.getOwnerReservationCards(
                me.getId(), ownerPage, PAGE_SIZE, ownerStatusFilter);
        final List<ReservationCardView> ownerReservations = ownerResultPage.getContent().stream()
                .map(card -> toReservationCardView(card, locale))
                .collect(Collectors.toList());

        final int lastRiderPage = riderResultPage.getTotalPages() - 1;
        final int lastOwnerPage = ownerResultPage.getTotalPages() - 1;
        if (riderPage > lastRiderPage || ownerPage > lastOwnerPage) {
            final UriComponentsBuilder uriBuilder = UriComponentsBuilder
                    .fromHttpRequest(new ServletServerHttpRequest(request));
            if (riderPage > lastRiderPage) {
                uriBuilder.replaceQueryParam("riderPage", lastRiderPage);
            }
            if (ownerPage > lastOwnerPage) {
                uriBuilder.replaceQueryParam("ownerPage", lastOwnerPage);
            }
            final RedirectView redirectView = new RedirectView(uriBuilder.build().toUriString());
            redirectView.setExposeModelAttributes(false);
            return new ModelAndView(redirectView);
        }

        final ModelAndView mav = new ModelAndView("myReservations");
        mav.addObject("riderReservations", riderReservations);
        mav.addObject("riderReservationsPage", riderResultPage);
        mav.addObject("ownerReservations", ownerReservations);
        mav.addObject("ownerReservationsPage", ownerResultPage);
        mav.addObject("selectedReservationsTab", selectedTab);
        mav.addObject("activeTab", "my-reservations");
        mav.addObject("riderStatusFilter", riderStatusFilter);
        mav.addObject("ownerStatusFilter", ownerStatusFilter);
        return mav;
    }

    private static String normalizeReservationStatusParam(final String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        final String t = raw.trim().toLowerCase();
        return RESERVATION_STATUS_WHITELIST.contains(t) ? t : null;
    }

    private static String normalizeReservationsTab(final String tab) {
        if (TAB_OWNER.equals(tab)) {
            return TAB_OWNER;
        }
        return TAB_RIDER;
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
        final Listing listing = listingDetail.getListing();
        final boolean viewerIsOwner = "owner".equals(role);
        final String reservationPickupLocationDisplay = listingService.formatPickupForReservationView(
                listing,
                reservation,
                viewerIsOwner);
        final Locale locale = LocaleContextHolder.getLocale();
        final String pickupDisplay = WallDateTimeDisplayFormat.formatUtcAsWallLocalNoSeconds(reservation.getStartDate(), locale);
        final String returnDisplay = WallDateTimeDisplayFormat.formatUtcAsWallLocalNoSeconds(reservation.getEndDate(), locale);
        final String totalPrice = reservation.getTotalPrice().toString();
        final long carImageId = listingDetail.getPictures().isEmpty() ? 0L : listingDetail.getPictures().get(0).getImageId();

        final ModelAndView mav = new ModelAndView("myReservationDetail");
        mav.addObject("reservation", reservation);
        mav.addObject("listing", listing);
        mav.addObject("reservationPickupLocationDisplay", reservationPickupLocationDisplay);
        mav.addObject("car", listingDetail.getCar());
        mav.addObject("owner", listingDetail.getOwner());
        mav.addObject("pickupDateTime", pickupDisplay);
        mav.addObject("returnDateTime", returnDisplay);
        mav.addObject("statusKey", reservation.getStatus().name().toLowerCase());
        mav.addObject("totalPrice", totalPrice);
        mav.addObject("carImageId", carImageId);
        mav.addObject("activeTab", "my-reservations");
        mav.addObject("reservationRole", role);
        mav.addObject("uploadMaxImageBytes", imageService.getMaxImageBytes());
        mav.addObject("uploadMaxImageMegabytes", imageService.getMaxImageMegabytesRoundedUp());
        reservation.getPaymentProofDeadlineAt()
                .ifPresent(deadline -> mav.addObject(
                        "paymentProofDeadlineDisplay",
                        WallDateTimeDisplayFormat.formatUtcAsWallLocalNoSeconds(deadline, locale)));
        mav.addObject("hasPaymentReceipt", reservation.getPaymentReceiptFileId().isPresent());
        mav.addObject("paymentReceiptApproved", reservation.isPaymentApproved());
        return mav;
    }

    @GetMapping("/my-reservations/{reservationId}/payment-receipt/download")
    public ResponseEntity<byte[]> downloadPaymentReceipt(
            @ModelAttribute(name = LoggedUserAdvice.CURRENT_USER_MODEL_KEY, binding = false) final User currentUser,
            @PathVariable("reservationId") final long reservationId) {
        final User me = WebAuthUtils.requireUser(currentUser);
        final Optional<StoredFile> fileOpt = reservationService.findPaymentReceiptForParticipant(me.getId(), reservationId);
        if (fileOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        final StoredFile sf = fileOpt.get();
        final HttpHeaders headers = new HttpHeaders();
        MediaType contentType = MediaType.APPLICATION_OCTET_STREAM;
        if (sf.getContentType() != null && !sf.getContentType().isBlank()) {
            try {
                contentType = MediaType.parseMediaType(sf.getContentType());
            } catch (final IllegalArgumentException ignored) {
                contentType = MediaType.APPLICATION_OCTET_STREAM;
            }
        }
        headers.setContentType(contentType);
        final String safeName = sanitizeDownloadFileName(sf.getFileName());
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + safeName + "\"");
        return new ResponseEntity<>(sf.getData(), headers, HttpStatus.OK);
    }

    @PostMapping("/my-reservations/{reservationId}/payment-receipt/approval")
    public ModelAndView setPaymentReceiptApproval(
            @ModelAttribute(name = LoggedUserAdvice.CURRENT_USER_MODEL_KEY, binding = false) final User currentUser,
            @PathVariable("reservationId") final long reservationId,
            @RequestParam("approved") final boolean approved,
            final RedirectAttributes redirectAttributes) {
        final User me = WebAuthUtils.requireUser(currentUser);
        try {
            reservationService.setPaymentReceiptApprovalByOwner(me.getId(), reservationId, approved);
            redirectAttributes.addFlashAttribute(
                    "paymentApprovalMessage",
                    localeMessages.msg("myReservationDetail.payment.approvalUpdated"));
        } catch (final RydenException e) {
            redirectAttributes.addFlashAttribute("paymentReceiptError", localeMessages.msg(e));
        }
        final RedirectView rv = new RedirectView("/my-reservations/" + reservationId + "?role=owner", true);
        rv.setExposeModelAttributes(false);
        return new ModelAndView(rv);
    }

    private static String sanitizeDownloadFileName(final String raw) {
        if (raw == null || raw.isBlank()) {
            return "receipt";
        }
        final String trimmed = raw.trim().replace("\"", "'").replaceAll("[\\\\/:*?|<>\\r\\n]+", "_");
        return trimmed.length() > 120 ? trimmed.substring(0, 120) : trimmed;
    }

    @PostMapping("/my-reservations/{reservationId}/payment-receipt")
    public ModelAndView uploadPaymentReceipt(
            @ModelAttribute(name = LoggedUserAdvice.CURRENT_USER_MODEL_KEY, binding = false) final User currentUser,
            @PathVariable("reservationId") final long reservationId,
            @RequestParam("paymentReceipt") final MultipartFile file,
            final RedirectAttributes redirectAttributes) {
        final User me = WebAuthUtils.requireUser(currentUser);
        try {
            reservationService.attachPaymentReceipt(
                    me.getId(),
                    reservationId,
                    file.getOriginalFilename(),
                    file.getContentType(),
                    file.getBytes());
        } catch (final RydenException e) {
            redirectAttributes.addFlashAttribute("paymentReceiptError", localeMessages.msg(e));
        } catch (final IOException e) {
            redirectAttributes.addFlashAttribute("paymentReceiptError", localeMessages.msg(MessageKeys.PUBLISH_IMAGES_READ));
        }
        final RedirectView rv = new RedirectView("/my-reservations/" + reservationId + "?role=rider", true);
        rv.setExposeModelAttributes(false);
        return new ModelAndView(rv);
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
            @RequestParam(defaultValue = "rider") final String role,
            final RedirectAttributes redirectAttributes) {
        final User me = WebAuthUtils.requireUser(currentUser);
        final Optional<Reservation> reservationOpt = "owner".equals(role)
                ? reservationService.getOwnerReservationById(me.getId(), reservationId)
                : reservationService.getRiderReservationById(me.getId(), reservationId);

        if (reservationOpt.isEmpty()) {
            return new ModelAndView(new RedirectView("/my-reservations", true));
        }

        final Optional<Reservation> cancelled =
                reservationService.cancelReservationAsParticipant(me.getId(), reservationId);
        if (cancelled.isEmpty()) {
            redirectAttributes.addFlashAttribute(
                    "cancelReservationError",
                    localeMessages.msg(MessageKeys.RESERVATION_CANCEL_NOT_ALLOWED));
            final RedirectView rv = new RedirectView("/my-reservations/" + reservationId + "?role=" + role, true);
            rv.setExposeModelAttributes(false);
            return new ModelAndView(rv);
        }

        final ModelAndView mav = new ModelAndView("reservationCancelled");
        mav.addObject("reservationRole", role);
        return mav;
    }
}

