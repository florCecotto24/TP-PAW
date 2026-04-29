package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.models.domain.Listing;
import ar.edu.itba.paw.models.dto.ListingDetail;
import ar.edu.itba.paw.models.dto.ListingCard;
import ar.edu.itba.paw.models.dto.Page;
import ar.edu.itba.paw.models.dto.profile.CounterpartyHeaderDto;
import ar.edu.itba.paw.models.dto.profile.ReviewItemDto;
import ar.edu.itba.paw.models.domain.Reservation;
import ar.edu.itba.paw.models.dto.ReservationCard;
import ar.edu.itba.paw.models.domain.StoredFile;
import ar.edu.itba.paw.models.domain.User;
import ar.edu.itba.paw.models.util.WallDateTimeDisplayFormat;
import ar.edu.itba.paw.exception.MessageKeys;
import ar.edu.itba.paw.exception.RydenException;
import ar.edu.itba.paw.services.ImageService;
import ar.edu.itba.paw.services.ListingService;
import ar.edu.itba.paw.services.policy.PaymentReceiptUploadPolicy;
import ar.edu.itba.paw.services.ReservationService;
import ar.edu.itba.paw.services.ReviewService;
import ar.edu.itba.paw.services.UserService;
import ar.edu.itba.paw.webapp.support.CurrentUser;
import ar.edu.itba.paw.webapp.dto.ReservationCardView;
import ar.edu.itba.paw.webapp.dto.VehicleCardView;
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
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
public class MyReservationsController {

    private static final int PAGE_SIZE = 8;
    private static final int COUNTERPARTY_RECENT_REVIEWS_LIMIT = 3;
    private static final int COUNTERPARTY_ACTIVE_LISTINGS_PAGE_SIZE = 8;
    private static final Set<String> RESERVATION_STATUS_WHITELIST =
            Set.of("pending", "accepted", "started", "cancelled", "finished");

    private final ReservationService reservationService;
    private final ListingService listingService;
    private final ImageService imageService;
    private final LocaleMessages localeMessages;
    private final PaymentReceiptUploadPolicy paymentReceiptUploadPolicy;
    private final ReviewService reviewService;
    private final UserService userService;

    @Autowired
    public MyReservationsController(
            final ReservationService reservationService,
            final ListingService listingService,
            final ImageService imageService,
            final LocaleMessages localeMessages,
            final PaymentReceiptUploadPolicy paymentReceiptUploadPolicy,
            final ReviewService reviewService,
            final UserService userService) {
        this.reservationService = reservationService;
        this.listingService = listingService;
        this.imageService = imageService;
        this.localeMessages = localeMessages;
        this.paymentReceiptUploadPolicy = paymentReceiptUploadPolicy;
        this.reviewService = reviewService;
        this.userService = userService;
    }

    @GetMapping("/my-reservations")
    public ModelAndView myReservations(
            @CurrentUser final User currentUser,
            @RequestParam(defaultValue = "0") int riderPage,
            @RequestParam(required = false) final String riderStatus,
            final HttpServletRequest request) {
        final User me = WebAuthUtils.requireUser(currentUser);
        riderPage = Math.max(0, riderPage);
        final String riderStatusFilter = normalizeReservationStatusParam(riderStatus);

        final Page<ReservationCard> riderResultPage = reservationService.getRiderReservationCards(
                me.getId(), riderPage, PAGE_SIZE, riderStatusFilter);
        final Locale locale = LocaleContextHolder.getLocale();
        final List<ReservationCardView> riderReservations = riderResultPage.getContent().stream()
                .map(card -> toReservationCardView(card, locale))
                .collect(Collectors.toList());

        final int lastRiderPage = riderResultPage.getTotalPages() - 1;
        if (riderPage > lastRiderPage) {
            final RedirectView redirectView = new RedirectView(
                    UriComponentsBuilder.fromHttpRequest(new ServletServerHttpRequest(request))
                            .replaceQueryParam("riderPage", lastRiderPage)
                            .build()
                            .toUriString());
            redirectView.setExposeModelAttributes(false);
            return new ModelAndView(redirectView);
        }

        final ModelAndView mav = new ModelAndView("myReservations");
        mav.addObject("riderReservations", riderReservations);
        mav.addObject("riderReservationsPage", riderResultPage);
        mav.addObject("activeTab", "my-reservations");
        mav.addObject("riderStatusFilter", riderStatusFilter);
        return mav;
    }

    private static String normalizeReservationStatusParam(final String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        final String t = raw.trim().toLowerCase();
        return RESERVATION_STATUS_WHITELIST.contains(t) ? t : null;
    }

    @GetMapping("/my-reservations/{reservationId}")
    public ModelAndView reservationDetail(
            @CurrentUser final User currentUser,
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
        mav.addObject("cbu", listingDetail.getOwner().getCbu().orElse(""));
        mav.addObject("pickupDateTime", pickupDisplay);
        mav.addObject("returnDateTime", returnDisplay);
        mav.addObject("statusKey", reservation.getStatus().name().toLowerCase());
        mav.addObject("totalPrice", totalPrice);
        mav.addObject("carImageId", carImageId);
        mav.addObject("activeTab", "my-reservations");
        mav.addObject("reservationRole", role);
        mav.addObject("uploadMaxImageBytes", imageService.getMaxImageBytes());
        mav.addObject("uploadMaxImageMegabytes", imageService.getMaxImageMegabytesRoundedUp());
        mav.addObject("uploadMaxPaymentReceiptBytes", paymentReceiptUploadPolicy.getMaxBytes());
        mav.addObject("uploadMaxPaymentReceiptMegabytes", paymentReceiptUploadPolicy.getMaxMegabytesRoundedUp());
        mav.addObject("reviewCommentMaxLength", reviewService.getReviewCommentMaxLength());
        final OffsetDateTime nowUtc = OffsetDateTime.now(ZoneOffset.UTC);
        final boolean periodEnded = nowUtc.isAfter(reservation.getEndDate());
        mav.addObject("reservationPeriodEnded", periodEnded);
        mav.addObject("canOwnerMarkCarReturned", viewerIsOwner && periodEnded && !reservation.isCarReturned());
        final boolean hasOwnerReview = reviewService.hasOwnerReview(reservation.getId());
        final boolean hasRiderReview = reviewService.hasRiderReview(reservation.getId());
        mav.addObject("canOwnerReviewRider", viewerIsOwner && reservation.isCarReturned() && !hasOwnerReview);
        mav.addObject("canRiderReviewOwner", !viewerIsOwner && periodEnded && !hasRiderReview);
        reservation.getPaymentProofDeadlineAt()
                .ifPresent(deadline -> mav.addObject(
                        "paymentProofDeadlineDisplay",
                        WallDateTimeDisplayFormat.formatUtcAsWallLocalNoSeconds(deadline, locale)));
        mav.addObject("hasPaymentReceipt", reservation.getPaymentReceiptFileId().isPresent());
        mav.addObject("paymentReceiptApproved", reservation.isPaymentApproved());
        return mav;
    }

    @GetMapping("/my-reservations/{reservationId}/counterparty-profile")
    public ModelAndView counterpartyProfile(
            @CurrentUser final User currentUser,
            @PathVariable("reservationId") final long reservationId,
            @RequestParam final String role) {
        final User me = WebAuthUtils.requireUser(currentUser);
        if (!"owner".equals(role) && !"rider".equals(role)) {
            return new ModelAndView(new RedirectView("/my-reservations/" + reservationId, true));
        }
        final Optional<Reservation> reservationOpt = "owner".equals(role)
                ? reservationService.getOwnerReservationById(me.getId(), reservationId)
                : reservationService.getRiderReservationById(me.getId(), reservationId);
        if (reservationOpt.isEmpty()) {
            final RedirectView rv = new RedirectView("/my-reservations/" + reservationId + "?role=" + role, true);
            rv.setExposeModelAttributes(false);
            return new ModelAndView(rv);
        }

        final Reservation reservation = reservationOpt.get();
        final Optional<User> counterpartyOpt = "owner".equals(role)
                ? userService.getUserById(reservation.getRiderId())
                : userService.getListingOwner(reservation.getListingId());
        if (counterpartyOpt.isEmpty()) {
            final RedirectView rv = new RedirectView("/my-reservations/" + reservationId + "?role=" + role, true);
            rv.setExposeModelAttributes(false);
            return new ModelAndView(rv);
        }

        final User counterparty = counterpartyOpt.get();
        final boolean counterpartyIsOwner = "rider".equals(role);
        final Locale locale = LocaleContextHolder.getLocale();
        final DateTimeFormatter memberSinceFormatter = DateTimeFormatter.ofPattern("LLLL uuuu").withLocale(locale);
        final BigDecimal averageRating = reviewService.getAverageRatingForCounterparty(counterparty.getId(), counterpartyIsOwner);
        final List<ReviewItemDto> recentReviewItems = reviewService.getRecentCommentReviewsForCounterparty(
                counterparty.getId(),
                counterpartyIsOwner,
                COUNTERPARTY_RECENT_REVIEWS_LIMIT);
        final CounterpartyHeaderDto headerDto = new CounterpartyHeaderDto(
                counterparty.getForename() + " " + counterparty.getSurname(),
                counterparty.getForename(),
                counterparty.getSurname(),
                null,
                averageRating,
                0L,
                counterparty.getAbout().map(String::trim).orElse(null),
                counterparty.getMemberSince().orElse(null),
                counterparty.getMemberSince().map(memberSinceFormatter::format).orElse(null),
                counterparty.getProfilePictureId().orElse(null));
        final List<VehicleCardView> counterpartyActiveListings = counterpartyIsOwner
                ? listingService.getOwnerListingCards(
                                counterparty.getId(),
                                0,
                                COUNTERPARTY_ACTIVE_LISTINGS_PAGE_SIZE,
                                "active",
                                null)
                        .getContent()
                        .stream()
                        .filter(card -> card.getListingId() != reservation.getListingId())
                        .map(MyReservationsController::toVehicleCardView)
                        .collect(Collectors.toList())
                : List.of();

        final ModelAndView mav = new ModelAndView("counterpartyProfile");
        mav.addObject("activeTab", "my-reservations");
        mav.addObject("counterpartyForename", headerDto.getForename());
        mav.addObject("counterpartySurname", headerDto.getSurname());
        mav.addObject("counterpartyAbout", headerDto.getAbout().orElse(""));
        mav.addObject("counterpartyProfileImageId", headerDto.getProfileImageId().orElse(null));
        mav.addObject("counterpartyMemberSinceDisplay", headerDto.getMemberSinceDisplay().orElse(null));
        mav.addObject("counterpartyAverageRating", headerDto.getAverageRating());
        mav.addObject(
                "counterpartyLicenseValidated",
                counterparty.isLicenseValidated() || counterparty.getLicenseFileId().isPresent());
        mav.addObject(
                "counterpartyInsuranceValidated",
                counterparty.isInsuranceValidated() || counterparty.getInsuranceFileId().isPresent());
        mav.addObject(
                "counterpartyIdentityValidated",
                counterparty.isIdentityValidated() || counterparty.getIdentityFileId().isPresent());
        mav.addObject(
                "recentReviewComments",
                recentReviewItems.stream()
                        .map(item -> item.getComment().orElse("").trim())
                        .filter(comment -> !comment.isEmpty())
                        .collect(Collectors.toList()));
        mav.addObject("showCounterpartyActiveListings", counterpartyIsOwner);
        mav.addObject("counterpartyActiveListings", counterpartyActiveListings);
        return mav;
    }

    private static VehicleCardView toVehicleCardView(final ListingCard card) {
        return new VehicleCardView(
                card.getListingId(),
                card.getBrand(),
                card.getModel(),
                card.getDayPrice(),
                card.getImageId(),
                null,
                card.getRatingAvg().orElse(null));
    }

    @GetMapping("/my-reservations/{reservationId}/payment-receipt/download")
    public ResponseEntity<byte[]> downloadPaymentReceipt(
            @CurrentUser final User currentUser,
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
            @CurrentUser final User currentUser,
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
            @CurrentUser final User currentUser,
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

    @PostMapping("/my-reservations/{reservationId}/car-returned")
    public ModelAndView markCarReturned(
            @CurrentUser final User currentUser,
            @PathVariable("reservationId") final long reservationId,
            final RedirectAttributes redirectAttributes) {
        final User me = WebAuthUtils.requireUser(currentUser);
        try {
            reservationService.markCarReturnedByOwner(me.getId(), reservationId);
            redirectAttributes.addFlashAttribute("carReturnedMessage", localeMessages.msg("myReservationDetail.carReturned.success"));
        } catch (final RydenException e) {
            redirectAttributes.addFlashAttribute("carReturnedError", localeMessages.msg(e));
        }
        final RedirectView rv = new RedirectView("/my-reservations/" + reservationId + "?role=owner", true);
        rv.setExposeModelAttributes(false);
        return new ModelAndView(rv);
    }

    @PostMapping("/my-reservations/{reservationId}/owner-review-rider")
    public ModelAndView submitOwnerReviewOfRider(
            @CurrentUser final User currentUser,
            @PathVariable("reservationId") final long reservationId,
            @RequestParam(name = "rating", required = false) final Integer rating,
            @RequestParam(name = "comment", required = false) final String comment,
            final RedirectAttributes redirectAttributes) {
        final User me = WebAuthUtils.requireUser(currentUser);
        try {
            reviewService.submitOwnerReviewOfRider(me.getId(), reservationId, rating, comment);
            redirectAttributes.addFlashAttribute("reviewMessage", localeMessages.msg("myReservationDetail.review.success"));
        } catch (final RydenException e) {
            redirectAttributes.addFlashAttribute("reviewError", localeMessages.msg(e));
        }
        final RedirectView rv = new RedirectView("/my-reservations/" + reservationId + "?role=owner", true);
        rv.setExposeModelAttributes(false);
        return new ModelAndView(rv);
    }

    @PostMapping("/my-reservations/{reservationId}/rider-review-owner")
    public ModelAndView submitRiderReviewOfOwner(
            @CurrentUser final User currentUser,
            @PathVariable("reservationId") final long reservationId,
            @RequestParam(name = "rating", required = false) final Integer rating,
            @RequestParam(name = "comment", required = false) final String comment,
            final RedirectAttributes redirectAttributes) {
        final User me = WebAuthUtils.requireUser(currentUser);
        try {
            reviewService.submitRiderReviewOfOwner(me.getId(), reservationId, rating, comment);
            redirectAttributes.addFlashAttribute("reviewMessage", localeMessages.msg("myReservationDetail.review.success"));
        } catch (final RydenException e) {
            redirectAttributes.addFlashAttribute("reviewError", localeMessages.msg(e));
        }
        final RedirectView rv = new RedirectView("/my-reservations/" + reservationId + "?role=rider", true);
        rv.setExposeModelAttributes(false);
        return new ModelAndView(rv);
    }

    @PostMapping("/my-reservations/{reservationId}/cancel")
    public ModelAndView cancelReservation(
            @CurrentUser final User currentUser,
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

