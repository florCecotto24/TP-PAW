package ar.edu.itba.paw.webapp.controller;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.web.util.UriComponentsBuilder;

import ar.edu.itba.paw.exception.MessageKeys;
import ar.edu.itba.paw.exception.RydenException;
import ar.edu.itba.paw.models.domain.Reservation;
import ar.edu.itba.paw.models.domain.StoredFile;
import ar.edu.itba.paw.models.domain.User;
import ar.edu.itba.paw.models.dto.Page;
import ar.edu.itba.paw.models.dto.reservation.ReservationCard;
import ar.edu.itba.paw.models.dto.reservation.ReservationCardDisplayRow;
import ar.edu.itba.paw.models.dto.reservation.ReservationChatPageModel;
import ar.edu.itba.paw.models.dto.reservation.ReservationDetailPageModel;
import ar.edu.itba.paw.models.dto.profile.CounterpartyProfilePageModel;
import ar.edu.itba.paw.models.pagination.UiPaging;
import ar.edu.itba.paw.models.util.search.MyHubSortSanitizer;
import ar.edu.itba.paw.services.ImageService;
import ar.edu.itba.paw.services.ReservationService;
import ar.edu.itba.paw.services.CounterpartyProfileViewService;
import ar.edu.itba.paw.services.ReservationViewService;
import ar.edu.itba.paw.webapp.dto.VehicleCardView;
import ar.edu.itba.paw.webapp.form.ReservationReviewForm;
import ar.edu.itba.paw.webapp.support.ConsumerVehicleCardViewFactory;
import ar.edu.itba.paw.webapp.support.CurrentUser;
import ar.edu.itba.paw.webapp.support.MyReservationDetailModelFactory;
import ar.edu.itba.paw.webapp.support.ReservationViewerRole;
import ar.edu.itba.paw.webapp.support.facade.ReviewSubmissionFacade;
import ar.edu.itba.paw.webapp.util.LocaleMessages;
import ar.edu.itba.paw.webapp.util.WebAuthUtils;
import ar.edu.itba.paw.webapp.validation.ReservationReviewFormValidator;

/** Rider and owner reservation lists, detail, payment proof upload, reviews, and download flows. */
@Controller
public final class MyReservationsController {

    private static final Logger LOG = LoggerFactory.getLogger(MyReservationsController.class);

    private final ReservationService reservationService;
    private final ReservationViewService reservationViewService;
    private final CounterpartyProfileViewService counterpartyProfileViewService;
    private final LocaleMessages localeMessages;
    private final ReservationReviewFormValidator reservationReviewFormValidator;
    private final MyReservationDetailModelFactory reservationDetailFactory;
    private final ReviewSubmissionFacade reviewSubmissionFacade;
    private final ConsumerVehicleCardViewFactory consumerVehicleCardViewFactory;
    private final ImageService imageService;

    public MyReservationsController(
            final ReservationService reservationService,
            final ReservationViewService reservationViewService,
            final CounterpartyProfileViewService counterpartyProfileViewService,
            final LocaleMessages localeMessages,
            final ReservationReviewFormValidator reservationReviewFormValidator,
            final MyReservationDetailModelFactory reservationDetailFactory,
            final ReviewSubmissionFacade reviewSubmissionFacade,
            final ConsumerVehicleCardViewFactory consumerVehicleCardViewFactory,
            final ImageService imageService) {
        this.reservationService = reservationService;
        this.reservationViewService = reservationViewService;
        this.counterpartyProfileViewService = counterpartyProfileViewService;
        this.localeMessages = localeMessages;
        this.reservationReviewFormValidator = reservationReviewFormValidator;
        this.reservationDetailFactory = reservationDetailFactory;
        this.reviewSubmissionFacade = reviewSubmissionFacade;
        this.consumerVehicleCardViewFactory = consumerVehicleCardViewFactory;
        this.imageService = imageService;
    }

    @ModelAttribute("uploadMaxImageBytes")
    public long uploadMaxImageBytes() {
        return imageService.getMaxImageBytes();
    }

    @ModelAttribute("uploadMaxImageMegabytes")
    public long uploadMaxImageMegabytes() {
        return imageService.getMaxImageMegabytesRoundedUp();
    }

    private static final String DEFAULT_SORT = "date,desc";

    private static String myReservationDetailRedirectUrl(
            final long reservationId,
            final ReservationViewerRole role,
            final Long fromCar) {
        final StringBuilder sb = new StringBuilder();
        sb.append("/my-reservations/").append(reservationId).append("?role=").append(role.toLegacyString());
        if (fromCar != null) {
            sb.append("&fromCar=").append(fromCar);
        }
        return sb.toString();
    }

    private static RedirectView redirectToMyReservationDetailView(
            final long reservationId,
            final ReservationViewerRole role,
            final Long fromCar) {
        final RedirectView rv = new RedirectView(myReservationDetailRedirectUrl(reservationId, role, fromCar), true);
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
                rating, riderStatus, riderPage, riderSort, q, null);
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

        final ModelAndView mav = new ModelAndView("reservation/myReservations");
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
            @RequestParam(name = "role", required = false) final ReservationViewerRole roleParam,
            @RequestParam(required = false) final Long fromCar) {
        final User me = WebAuthUtils.requireUser(currentUser);
        final ReservationViewerRole role = roleParam != null ? roleParam : ReservationViewerRole.RIDER;
        final Optional<ReservationDetailPageModel> detailOpt = reservationViewService.loadMyReservationDetailForViewer(
                me.getId(), reservationId, role.toLegacyString(), LocaleContextHolder.getLocale());
        if (detailOpt.isEmpty()) {
            return new ModelAndView(new RedirectView("/my-reservations", true));
        }
        final ReservationDetailPageModel detail = detailOpt.get();
        final Long hub = MyReservationDetailModelFactory.ownerCarHubIfValid(fromCar, role.toLegacyString(), detail);
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
            @RequestParam(name = "role", required = false) final ReservationViewerRole role,
            @RequestParam(required = false) final Long fromCar) {
        final User me = WebAuthUtils.requireUser(currentUser);
        if (role == null) {
            return new ModelAndView(new RedirectView("/my-reservations/" + reservationId, true));
        }
        final Optional<ReservationChatPageModel> chatOpt = reservationViewService.loadReservationChatForParticipant(
                me.getId(), reservationId, role.toLegacyString(), LocaleContextHolder.getLocale());
        if (chatOpt.isEmpty()) {
            return new ModelAndView(redirectToMyReservationDetailView(reservationId, role, fromCar));
        }
        final ReservationChatPageModel chat = chatOpt.get();
        final ModelAndView mav = new ModelAndView("reservation/reservationChat");
        chat.populateModel(mav::addObject);
        mav.addObject("activeTab", "my-reservations");
        if (fromCar != null) {
            mav.addObject("fromCar", fromCar);
        }
        final Long hub = MyReservationDetailModelFactory.ownerCarHubIfValid(
                fromCar, role.toLegacyString(), chat.getCarId());
        if (hub != null) {
            mav.addObject("reservationDetailOwnerCarHubId", hub);
        }
        return mav;
    }

    @GetMapping("/my-reservations/{reservationId}/counterparty-profile")
    public ModelAndView counterpartyProfile(
            @CurrentUser final User currentUser,
            @PathVariable("reservationId") final long reservationId,
            @RequestParam(name = "role", required = false) final ReservationViewerRole role,
            @RequestParam(required = false) final Long fromCar) {
        final User me = WebAuthUtils.requireUser(currentUser);
        if (role == null) {
            return new ModelAndView(new RedirectView("/my-reservations/" + reservationId, true));
        }
        final Optional<CounterpartyProfilePageModel> profileOpt =
                counterpartyProfileViewService.loadCounterpartyProfileForReservationParticipant(
                        me.getId(), reservationId, role.toLegacyString(), LocaleContextHolder.getLocale());
        if (profileOpt.isEmpty()) {
            return new ModelAndView(redirectToMyReservationDetailView(reservationId, role, fromCar));
        }
        final CounterpartyProfilePageModel profile = profileOpt.get();
        final ModelAndView mav = new ModelAndView("profile/counterpartyProfile");
        profile.populateModel(mav::addObject);
        // The JSP renders the listings grid with <ryden:consumerCarCard>, which only
        // accepts VehicleCardView. Converting here keeps the webapp-only mapping out
        // of the models module while still letting the service own the data fetch.
        final List<VehicleCardView> counterpartyActiveListings =
                consumerVehicleCardViewFactory.toConsumerVehicleCardViews(profile.getActiveOwnerCarCards(), me.getId());
        mav.addObject("counterpartyActiveListings", counterpartyActiveListings);
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
        final ModelAndView view = new ModelAndView("reservation/payment-receipt-view");
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
        final ModelAndView view = new ModelAndView("reservation/refund-receipt-view");
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

    @PostMapping("/my-reservations/{reservationId}/refund-receipt")
    public ModelAndView uploadRefundReceipt(
            @CurrentUser final User currentUser,
            @PathVariable("reservationId") final long reservationId,
            @RequestParam("refundReceipt") final MultipartFile file,
            @RequestParam(required = false) final Long fromCar,
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
        return new ModelAndView(redirectToMyReservationDetailView(reservationId, ReservationViewerRole.OWNER, fromCar));
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
        return new ModelAndView(redirectToMyReservationDetailView(reservationId, ReservationViewerRole.RIDER, null));
    }

    @PostMapping("/my-reservations/{reservationId}/car-returned")
    public ModelAndView markCarReturned(
            @CurrentUser final User currentUser,
            @PathVariable("reservationId") final long reservationId,
            @RequestParam(required = false) final Long fromCar,
            final RedirectAttributes redirectAttributes) {
        final User me = WebAuthUtils.requireUser(currentUser);
        try {
            reservationService.markCarReturnedByOwner(me.getId(), reservationId);
            redirectAttributes.addFlashAttribute(
                    "carReturnedMessage", localeMessages.msg("myReservationDetail.carReturned.success"));
        } catch (final RydenException e) {
            redirectAttributes.addFlashAttribute("carReturnedError", localeMessages.msg(e));
        }
        return new ModelAndView(redirectToMyReservationDetailView(reservationId, ReservationViewerRole.OWNER, fromCar));
    }

    @PostMapping("/my-reservations/{reservationId}/owner-review-rider")
    public ModelAndView submitOwnerReviewOfRider(
            @CurrentUser final User currentUser,
            @PathVariable("reservationId") final long reservationId,
            @ModelAttribute("ownerReviewForm") final ReservationReviewForm ownerReviewForm,
            final BindingResult ownerBinding,
            @RequestParam(required = false) final Long fromCar,
            final RedirectAttributes redirectAttributes) {
        final User me = WebAuthUtils.requireUser(currentUser);
        final ReviewSubmissionFacade.SubmissionResult result = reviewSubmissionFacade.submit(
                me.getId(), reservationId, ReviewSubmissionFacade.Role.OWNER, ownerReviewForm, ownerBinding);
        return handleReviewSubmissionResult(
                result, me.getId(), reservationId, ReservationViewerRole.OWNER,
                ownerReviewForm, ownerBinding, new ReservationReviewForm(), null,
                fromCar, redirectAttributes);
    }

    @PostMapping("/my-reservations/{reservationId}/rider-review-owner")
    public ModelAndView submitRiderReviewOfOwner(
            @CurrentUser final User currentUser,
            @PathVariable("reservationId") final long reservationId,
            @ModelAttribute("riderReviewForm") final ReservationReviewForm riderReviewForm,
            final BindingResult riderBinding,
            @RequestParam(required = false) final Long fromCar,
            final RedirectAttributes redirectAttributes) {
        final User me = WebAuthUtils.requireUser(currentUser);
        final ReviewSubmissionFacade.SubmissionResult result = reviewSubmissionFacade.submit(
                me.getId(), reservationId, ReviewSubmissionFacade.Role.RIDER, riderReviewForm, riderBinding);
        return handleReviewSubmissionResult(
                result, me.getId(), reservationId, ReservationViewerRole.RIDER,
                new ReservationReviewForm(), null, riderReviewForm, riderBinding,
                fromCar, redirectAttributes);
    }

    private ModelAndView handleReviewSubmissionResult(
            final ReviewSubmissionFacade.SubmissionResult result,
            final long viewerUserId,
            final long reservationId,
            final ReservationViewerRole viewerRole,
            final ReservationReviewForm ownerReviewForm,
            final BindingResult ownerBinding,
            final ReservationReviewForm riderReviewForm,
            final BindingResult riderBinding,
            final Long fromCar,
            final RedirectAttributes redirectAttributes) {
        switch (result.outcome()) {
            case SUCCESS:
                redirectAttributes.addFlashAttribute("reviewMessage", result.messageOrNull());
                return new ModelAndView(redirectToMyReservationDetailView(reservationId, viewerRole, fromCar));
            case NEEDS_RERENDER:
            default:
                return reservationDetailFactory.detailOrRedirect(
                        viewerUserId, reservationId, viewerRole.toLegacyString(),
                        ownerReviewForm, ownerBinding, riderReviewForm, riderBinding, fromCar);
        }
    }

    @PostMapping("/my-reservations/{reservationId}/cancel")
    public ModelAndView cancelReservation(
            @CurrentUser final User currentUser,
            @PathVariable("reservationId") final long reservationId,
            @RequestParam(name = "role", required = false) final ReservationViewerRole roleParam,
            @RequestParam(required = false) final Long fromCar,
            final RedirectAttributes redirectAttributes) {
        final User me = WebAuthUtils.requireUser(currentUser);
        final ReservationViewerRole role = roleParam != null ? roleParam : ReservationViewerRole.RIDER;
        final Optional<Reservation> reservationOpt = role == ReservationViewerRole.OWNER
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
            return new ModelAndView(redirectToMyReservationDetailView(reservationId, role, fromCar));
        }

        final ModelAndView mav = new ModelAndView("reservation/reservationCancelled");
        mav.addObject("reservationRole", role.toLegacyString());
        return mav;
    }
}

