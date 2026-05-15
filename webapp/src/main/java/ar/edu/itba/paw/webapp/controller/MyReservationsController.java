package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.models.dto.Page;
import ar.edu.itba.paw.models.pagination.UiPaging;
import ar.edu.itba.paw.models.dto.profile.CounterpartyProfilePageModel;
import ar.edu.itba.paw.models.domain.Reservation;
import ar.edu.itba.paw.models.dto.ReservationCard;
import ar.edu.itba.paw.models.dto.ReservationCardDisplayRow;
import ar.edu.itba.paw.models.dto.ReservationChatPageModel;
import ar.edu.itba.paw.models.dto.ReservationDetailPageModel;
import ar.edu.itba.paw.models.util.MyHubSortSanitizer;
import ar.edu.itba.paw.models.domain.StoredFile;
import ar.edu.itba.paw.models.domain.User;
import ar.edu.itba.paw.exception.MessageKeys;
import ar.edu.itba.paw.exception.RydenException;
import ar.edu.itba.paw.exception.reservation.RiderReservationException;
import ar.edu.itba.paw.services.ReservationService;
import ar.edu.itba.paw.services.ReservationViewService;
import ar.edu.itba.paw.services.ReviewService;
import ar.edu.itba.paw.webapp.form.ReservationReviewAction;
import ar.edu.itba.paw.webapp.form.ReservationReviewForm;
import ar.edu.itba.paw.webapp.support.CurrentUser;
import ar.edu.itba.paw.webapp.support.MyReservationDetailModelFactory;
import ar.edu.itba.paw.webapp.support.RiderReservationReviewExceptionMapper;
import ar.edu.itba.paw.webapp.util.LocaleMessages;
import ar.edu.itba.paw.webapp.util.WebAuthUtils;
import ar.edu.itba.paw.webapp.validation.ReservationReviewFormValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
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
import java.util.stream.Collectors;

/** Rider and owner reservation lists, detail, payment proof upload, reviews, and download flows. */
@Controller
public final class MyReservationsController {

    private static final Logger LOG = LoggerFactory.getLogger(MyReservationsController.class);

    private final ReservationService reservationService;
    private final ReservationViewService reservationViewService;
    private final LocaleMessages localeMessages;
    private final ReviewService reviewService;
    private final ReservationReviewFormValidator reservationReviewFormValidator;
    private final MyReservationDetailModelFactory reservationDetailFactory;
    private final RiderReservationReviewExceptionMapper riderReservationReviewExceptionMapper;

    public MyReservationsController(
            final ReservationService reservationService,
            final ReservationViewService reservationViewService,
            final LocaleMessages localeMessages,
            final ReviewService reviewService,
            final ReservationReviewFormValidator reservationReviewFormValidator,
            final MyReservationDetailModelFactory reservationDetailFactory,
            final RiderReservationReviewExceptionMapper riderReservationReviewExceptionMapper) {
        this.reservationService = reservationService;
        this.reservationViewService = reservationViewService;
        this.localeMessages = localeMessages;
        this.reviewService = reviewService;
        this.reservationReviewFormValidator = reservationReviewFormValidator;
        this.reservationDetailFactory = reservationDetailFactory;
        this.riderReservationReviewExceptionMapper = riderReservationReviewExceptionMapper;
    }

    private static final String DEFAULT_SORT = "date,desc";

    private static String myReservationDetailRedirectUrl(
            final long reservationId,
            final String role,
            final Long fromListing) {
        final StringBuilder sb = new StringBuilder();
        sb.append("/my-reservations/").append(reservationId).append("?role=").append(role);
        if (fromListing != null) {
            sb.append("&fromListing=").append(fromListing);
        }
        return sb.toString();
    }

    private static RedirectView redirectToMyReservationDetailView(
            final long reservationId,
            final String role,
            final Long fromListing) {
        final RedirectView rv = new RedirectView(myReservationDetailRedirectUrl(reservationId, role, fromListing), true);
        rv.setExposeModelAttributes(false);
        return rv;
    }

    @InitBinder({"ownerReviewForm", "riderReviewForm"})
    public void reservationReviewFormsBinder(final WebDataBinder binder) {
        binder.addValidators(reservationReviewFormValidator);
    }

    @GetMapping("/my-reservations")
    public ModelAndView myReservations(
            @CurrentUser final User currentUser,
            @RequestParam(defaultValue = "0") int riderPage,
            @RequestParam(required = false) final List<String> riderStatus,
            @RequestParam(required = false) final List<String> category,
            @RequestParam(required = false) final List<String> transmission,
            @RequestParam(required = false) final List<String> powertrain,
            @RequestParam(required = false) final BigDecimal priceMin,
            @RequestParam(required = false) final BigDecimal priceMax,
            @RequestParam(required = false) final List<String> rating,
            @RequestParam(required = false) final String sort,
            @RequestParam(required = false) final String q,
            final HttpServletRequest request) {
        final User me = WebAuthUtils.requireUser(currentUser);
        riderPage = Math.max(0, riderPage);

        final String riderSort = MyHubSortSanitizer.sanitize(sort, DEFAULT_SORT);
        final var criteria = reservationService.buildReservationSearchCriteria(
                null, me.getId(), category, transmission, powertrain, priceMin, priceMax,
                rating, riderStatus, riderPage, riderSort, q);
        final Page<ReservationCard> riderResultPage = reservationService.getRiderReservationCards(criteria);
        final Locale locale = LocaleContextHolder.getLocale();
        final List<ReservationCardDisplayRow> riderReservations = riderResultPage.getContent().stream()
                .map(card -> reservationViewService.toReservationCardDisplayRow(card, locale))
                .collect(Collectors.toList());

        final int safeRiderPage = UiPaging.clampZeroBasedPage(
                riderPage, riderResultPage.getTotalItems(), riderResultPage.getPageSize());
        if (safeRiderPage != riderPage) {
            final RedirectView redirectView = new RedirectView(
                    UriComponentsBuilder.fromHttpRequest(new ServletServerHttpRequest(request))
                            .replaceQueryParam("riderPage", safeRiderPage)
                            .build()
                            .toUriString());
            redirectView.setExposeModelAttributes(false);
            return new ModelAndView(redirectView);
        }

        final ModelAndView mav = new ModelAndView("myReservations");
        mav.addObject("riderReservations", riderReservations);
        mav.addObject("riderReservationsPage", riderResultPage);
        mav.addObject("activeTab", "my-reservations");
        mav.addObject("currentSort", riderSort);
        return mav;
    }

    @GetMapping("/my-reservations/{reservationId}")
    public ModelAndView reservationDetail(
            @CurrentUser final User currentUser,
            @PathVariable("reservationId") final long reservationId,
            @RequestParam(defaultValue = "rider") final String role,
            @RequestParam(required = false) final Long fromListing) {
        final User me = WebAuthUtils.requireUser(currentUser);
        final Optional<ReservationDetailPageModel> detailOpt = reservationViewService.loadMyReservationDetailForViewer(
                me.getId(), reservationId, role, LocaleContextHolder.getLocale());
        if (detailOpt.isEmpty()) {
            return new ModelAndView(new RedirectView("/my-reservations", true));
        }
        final ReservationDetailPageModel detail = detailOpt.get();
        final Long hub = MyReservationDetailModelFactory.ownerListingHubIfValid(fromListing, role, detail);
        return reservationDetailFactory.detailWithForms(
                detail,
                new ReservationReviewForm(),
                null,
                new ReservationReviewForm(),
                null,
                hub);
    }

    @GetMapping("/my-reservations/{reservationId}/chat")
    public ModelAndView reservationChat(
            @CurrentUser final User currentUser,
            @PathVariable("reservationId") final long reservationId,
            @RequestParam final String role,
            @RequestParam(required = false) final Long fromListing) {
        final User me = WebAuthUtils.requireUser(currentUser);
        if (!"owner".equals(role) && !"rider".equals(role)) {
            return new ModelAndView(new RedirectView("/my-reservations/" + reservationId, true));
        }
        final Optional<ReservationChatPageModel> chatOpt = reservationViewService.loadReservationChatForParticipant(
                me.getId(), reservationId, role, LocaleContextHolder.getLocale());
        if (chatOpt.isEmpty()) {
            return new ModelAndView(redirectToMyReservationDetailView(reservationId, role, fromListing));
        }
        final ReservationChatPageModel chat = chatOpt.get();
        final ModelAndView mav = new ModelAndView("reservationChat");
        chat.populateModel(mav::addObject);
        mav.addObject("activeTab", "my-reservations");
        if (fromListing != null) {
            mav.addObject("fromListing", fromListing);
        }
        final Long hub = MyReservationDetailModelFactory.ownerListingHubIfValid(
                fromListing, role, chat.getListingId());
        if (hub != null) {
            mav.addObject("reservationDetailOwnerListingHubId", hub);
        }
        return mav;
    }

    @GetMapping("/my-reservations/{reservationId}/counterparty-profile")
    public ModelAndView counterpartyProfile(
            @CurrentUser final User currentUser,
            @PathVariable("reservationId") final long reservationId,
            @RequestParam final String role,
            @RequestParam(required = false) final Long fromListing) {
        final User me = WebAuthUtils.requireUser(currentUser);
        if (!"owner".equals(role) && !"rider".equals(role)) {
            return new ModelAndView(new RedirectView("/my-reservations/" + reservationId, true));
        }
        final Optional<CounterpartyProfilePageModel> profileOpt =
                reservationViewService.loadCounterpartyProfileForReservationParticipant(
                        me.getId(), reservationId, role, LocaleContextHolder.getLocale());
        if (profileOpt.isEmpty()) {
            return new ModelAndView(redirectToMyReservationDetailView(reservationId, role, fromListing));
        }
        final CounterpartyProfilePageModel profile = profileOpt.get();
        final ModelAndView mav = new ModelAndView("counterpartyProfile");
        profile.populateModel(mav::addObject);
        mav.addObject("activeTab", "my-reservations");
        return mav;
    }

    @GetMapping("/my-reservations/{reservationId}/payment-receipt/view")
    public ModelAndView viewPaymentReceipt(
            @CurrentUser final User currentUser,
            @PathVariable("reservationId") final long reservationId) {
        final User me = WebAuthUtils.requireUser(currentUser);
        final Optional<StoredFile> fileOpt = reservationService.findPaymentReceiptForParticipant(me.getId(), reservationId);
        if (fileOpt.isEmpty()) {
            return new ModelAndView("redirect:/my-reservations/" + reservationId);
        }
        final ModelAndView view = new ModelAndView("payment-receipt-view");
        view.addObject("reservationId", reservationId);
        view.addObject("receiptFileName", fileOpt.get().getFileName());
        return view;
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
            } catch (final IllegalArgumentException e) {
                LOG.atDebug()
                        .setMessage("Invalid payment receipt Content-Type reservationId={} [{}]")
                        .addArgument(reservationId)
                        .addArgument(sf.getContentType())
                        .setCause(e)
                        .log();
                contentType = MediaType.APPLICATION_OCTET_STREAM;
            }
        }
        headers.setContentType(contentType);
        final String safeName = sanitizeDownloadFileName(sf.getFileName());
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + safeName + "\"");
        return new ResponseEntity<>(sf.getData(), headers, HttpStatus.OK);
    }

    @GetMapping("/my-reservations/{reservationId}/refund-receipt/view")
    public ModelAndView viewRefundReceipt(
            @CurrentUser final User currentUser,
            @PathVariable("reservationId") final long reservationId) {
        final User me = WebAuthUtils.requireUser(currentUser);
        final Optional<StoredFile> fileOpt = reservationService.findRefundReceiptForParticipant(me.getId(), reservationId);
        if (fileOpt.isEmpty()) {
            return new ModelAndView("redirect:/my-reservations/" + reservationId);
        }
        final ModelAndView view = new ModelAndView("refund-receipt-view");
        view.addObject("reservationId", reservationId);
        view.addObject("receiptFileName", fileOpt.get().getFileName());
        return view;
    }

    @GetMapping("/my-reservations/{reservationId}/refund-receipt/download")
    public ResponseEntity<byte[]> downloadRefundReceipt(
            @CurrentUser final User currentUser,
            @PathVariable("reservationId") final long reservationId) {
        final User me = WebAuthUtils.requireUser(currentUser);
        final Optional<StoredFile> fileOpt = reservationService.findRefundReceiptForParticipant(me.getId(), reservationId);
        if (fileOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        final StoredFile sf = fileOpt.get();
        final HttpHeaders headers = new HttpHeaders();
        MediaType contentType = MediaType.APPLICATION_OCTET_STREAM;
        if (sf.getContentType() != null && !sf.getContentType().isBlank()) {
            try {
                contentType = MediaType.parseMediaType(sf.getContentType());
            } catch (final IllegalArgumentException e) {
                LOG.atDebug()
                        .setMessage("Invalid refund receipt Content-Type reservationId={} [{}]")
                        .addArgument(reservationId)
                        .addArgument(sf.getContentType())
                        .setCause(e)
                        .log();
                contentType = MediaType.APPLICATION_OCTET_STREAM;
            }
        }
        headers.setContentType(contentType);
        final String safeName = sanitizeDownloadFileName(sf.getFileName());
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + safeName + "\"");
        return new ResponseEntity<>(sf.getData(), headers, HttpStatus.OK);
    }

    @PostMapping("/my-reservations/{reservationId}/refund-receipt/approval")
    public ModelAndView setRefundReceiptApproval(
            @CurrentUser final User currentUser,
            @PathVariable("reservationId") final long reservationId,
            @RequestParam("approved") final boolean approved,
            @RequestParam(required = false) final Long fromListing,
            final RedirectAttributes redirectAttributes) {
        final User me = WebAuthUtils.requireUser(currentUser);
        try {
            reservationService.setPaymentRefundApprovalByRider(me.getId(), reservationId, approved);
            redirectAttributes.addFlashAttribute(
                    "refundApprovalMessage",
                    localeMessages.msg("myReservationDetail.refund.approvalUpdated"));
        } catch (final RydenException e) {
            redirectAttributes.addFlashAttribute("refundReceiptError", localeMessages.msg(e));
        }
        return new ModelAndView(redirectToMyReservationDetailView(reservationId, "rider", fromListing));
    }

    @PostMapping("/my-reservations/{reservationId}/refund-receipt")
    public ModelAndView uploadRefundReceipt(
            @CurrentUser final User currentUser,
            @PathVariable("reservationId") final long reservationId,
            @RequestParam("refundReceipt") final MultipartFile file,
            @RequestParam(required = false) final Long fromListing,
            final RedirectAttributes redirectAttributes) {
        final User me = WebAuthUtils.requireUser(currentUser);
        try {
            reservationService.attachRefundReceiptByOwner(
                    me.getId(),
                    reservationId,
                    file.getOriginalFilename(),
                    file.getContentType(),
                    file.getBytes());
        } catch (final RydenException e) {
            redirectAttributes.addFlashAttribute("refundReceiptError", localeMessages.msg(e));
        } catch (final IOException e) {
            redirectAttributes.addFlashAttribute("refundReceiptError", localeMessages.msg(MessageKeys.PUBLISH_IMAGES_READ));
        }
        return new ModelAndView(redirectToMyReservationDetailView(reservationId, "owner", fromListing));
    }

    @PostMapping("/my-reservations/{reservationId}/payment-receipt/approval")
    public ModelAndView setPaymentReceiptApproval(
            @CurrentUser final User currentUser,
            @PathVariable("reservationId") final long reservationId,
            @RequestParam("approved") final boolean approved,
            @RequestParam(required = false) final Long fromListing,
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
        return new ModelAndView(redirectToMyReservationDetailView(reservationId, "owner", fromListing));
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

    @PostMapping("/my-reservations/{reservationId}/car-returned")
    public ModelAndView markCarReturned(
            @CurrentUser final User currentUser,
            @PathVariable("reservationId") final long reservationId,
            @RequestParam(value = "returned", defaultValue = "true") final boolean returned,
            @RequestParam(required = false) final Long fromListing,
            final RedirectAttributes redirectAttributes) {
        final User me = WebAuthUtils.requireUser(currentUser);
        try {
            reservationService.markCarReturnedByOwner(me.getId(), reservationId, returned);
            redirectAttributes.addFlashAttribute(
                    "carReturnedMessage",
                    localeMessages.msg(
                            returned ? "myReservationDetail.carReturned.success" : "myReservationDetail.carReturned.unmarkSuccess"));
        } catch (final RydenException e) {
            redirectAttributes.addFlashAttribute("carReturnedError", localeMessages.msg(e));
        }
        return new ModelAndView(redirectToMyReservationDetailView(reservationId, "owner", fromListing));
    }

    @PostMapping("/my-reservations/{reservationId}/owner-review-rider")
    public ModelAndView submitOwnerReviewOfRider(
            @CurrentUser final User currentUser,
            @PathVariable("reservationId") final long reservationId,
            @ModelAttribute("ownerReviewForm") final ReservationReviewForm ownerReviewForm,
            final BindingResult ownerBinding,
            @RequestParam(required = false) final Long fromListing,
            final RedirectAttributes redirectAttributes) {
        final User me = WebAuthUtils.requireUser(currentUser);
        if (ownerReviewForm.getReviewAction() == ReservationReviewAction.OMIT) {
            try {
                reviewService.submitOwnerReviewOfRider(me.getId(), reservationId, null, null);
                redirectAttributes.addFlashAttribute("reviewMessage", localeMessages.msg("myReservationDetail.review.success"));
            } catch (final RiderReservationException ex) {
                redirectAttributes.addFlashAttribute("reviewError", localeMessages.msg(ex));
            }
            return new ModelAndView(redirectToMyReservationDetailView(reservationId, "owner", fromListing));
        }
        if (ownerBinding.hasErrors()) {
            return reservationDetailFactory.detailOrRedirect(
                    me.getId(), reservationId, "owner", ownerReviewForm, ownerBinding, new ReservationReviewForm(), null, fromListing);
        }
        try {
            reviewService.submitOwnerReviewOfRider(
                    me.getId(),
                    reservationId,
                    ownerReviewForm.getRating(),
                    ownerReviewForm.getComment());
            redirectAttributes.addFlashAttribute("reviewMessage", localeMessages.msg("myReservationDetail.review.success"));
        } catch (final RiderReservationException ex) {
            riderReservationReviewExceptionMapper.mergeOntoBinding(ex, ownerBinding);
            return reservationDetailFactory.detailOrRedirect(
                    me.getId(), reservationId, "owner", ownerReviewForm, ownerBinding, new ReservationReviewForm(), null, fromListing);
        }
        return new ModelAndView(redirectToMyReservationDetailView(reservationId, "owner", fromListing));
    }

    @PostMapping("/my-reservations/{reservationId}/rider-review-owner")
    public ModelAndView submitRiderReviewOfOwner(
            @CurrentUser final User currentUser,
            @PathVariable("reservationId") final long reservationId,
            @ModelAttribute("riderReviewForm") final ReservationReviewForm riderReviewForm,
            final BindingResult riderBinding,
            @RequestParam(required = false) final Long fromListing,
            final RedirectAttributes redirectAttributes) {
        final User me = WebAuthUtils.requireUser(currentUser);
        if (riderReviewForm.getReviewAction() == ReservationReviewAction.OMIT) {
            try {
                reviewService.submitRiderReviewOfOwner(me.getId(), reservationId, null, null);
                redirectAttributes.addFlashAttribute("reviewMessage", localeMessages.msg("myReservationDetail.review.success"));
            } catch (final RiderReservationException ex) {
                redirectAttributes.addFlashAttribute("reviewError", localeMessages.msg(ex));
            }
            return new ModelAndView(redirectToMyReservationDetailView(reservationId, "rider", fromListing));
        }
        if (riderBinding.hasErrors()) {
            return reservationDetailFactory.detailOrRedirect(
                    me.getId(), reservationId, "rider", new ReservationReviewForm(), null, riderReviewForm, riderBinding, fromListing);
        }
        try {
            reviewService.submitRiderReviewOfOwner(
                    me.getId(),
                    reservationId,
                    riderReviewForm.getRating(),
                    riderReviewForm.getComment());
            redirectAttributes.addFlashAttribute("reviewMessage", localeMessages.msg("myReservationDetail.review.success"));
        } catch (final RiderReservationException ex) {
            riderReservationReviewExceptionMapper.mergeOntoBinding(ex, riderBinding);
            return reservationDetailFactory.detailOrRedirect(
                    me.getId(), reservationId, "rider", new ReservationReviewForm(), null, riderReviewForm, riderBinding, fromListing);
        }
        return new ModelAndView(redirectToMyReservationDetailView(reservationId, "rider", fromListing));
    }

    @PostMapping("/my-reservations/{reservationId}/cancel")
    public ModelAndView cancelReservation(
            @CurrentUser final User currentUser,
            @PathVariable("reservationId") final long reservationId,
            @RequestParam(defaultValue = "rider") final String role,
            @RequestParam(required = false) final Long fromListing,
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
            return new ModelAndView(redirectToMyReservationDetailView(reservationId, role, fromListing));
        }

        final ModelAndView mav = new ModelAndView("reservationCancelled");
        mav.addObject("reservationRole", role);
        return mav;
    }
}

