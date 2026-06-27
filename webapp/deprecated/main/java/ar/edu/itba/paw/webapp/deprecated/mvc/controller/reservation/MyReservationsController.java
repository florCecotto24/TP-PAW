package ar.edu.itba.paw.webapp.deprecated.mvc.controller.reservation;


import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;

import javax.validation.constraints.Min;

import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
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
import ar.edu.itba.paw.exception.reservation.ReservationAccessDeniedException;
import ar.edu.itba.paw.exception.reservation.ReservationCancelNotAllowedException;
import ar.edu.itba.paw.models.domain.car.Car;
import ar.edu.itba.paw.models.domain.file.StoredFile;
import ar.edu.itba.paw.models.domain.reservation.Reservation;
import ar.edu.itba.paw.models.domain.user.User;
import ar.edu.itba.paw.models.dto.profile.CounterpartyProfilePageModel;
import ar.edu.itba.paw.models.dto.reservation.ReservationChatPageModel;
import ar.edu.itba.paw.models.dto.reservation.ReservationDetailPageModel;
import ar.edu.itba.paw.models.dto.reservation.ReservationEditPageModel;
import ar.edu.itba.paw.models.dto.reservation.RiderReservationsListPageModel;
import ar.edu.itba.paw.models.pagination.UiPaging;
import ar.edu.itba.paw.models.util.search.MyHubSortSanitizer;
import ar.edu.itba.paw.services.file.ImageService;
import ar.edu.itba.paw.services.reservation.ReservationService;
import ar.edu.itba.paw.services.reservation.view.ReservationViewService;
import ar.edu.itba.paw.services.user.view.CounterpartyProfileViewService;
import ar.edu.itba.paw.webapp.config.properties.AppPaginationProperties;
import ar.edu.itba.paw.webapp.dto.VehicleCardView;
import ar.edu.itba.paw.webapp.form.reservation.ReservationReviewForm;
import ar.edu.itba.paw.webapp.support.ConsumerVehicleCardViewFactory;
import ar.edu.itba.paw.webapp.deprecated.mvc.support.CurrentUser;
import ar.edu.itba.paw.webapp.support.MyReservationDetailModelFactory;
import ar.edu.itba.paw.webapp.support.ReservationViewerRole;
import ar.edu.itba.paw.webapp.support.StoredFileDownloadResponses;
import ar.edu.itba.paw.webapp.deprecated.mvc.support.facade.ReviewSubmissionFacade;
import ar.edu.itba.paw.webapp.util.LocaleMessages;
import ar.edu.itba.paw.webapp.util.WebAuthUtils;
import ar.edu.itba.paw.webapp.validation.reservation.ReservationReviewFormValidator;

/** Rider and owner reservation lists, detail, payment proof upload, reviews, and download flows. */
@Controller
@Validated
public class MyReservationsController {

    private final ReservationService reservationService;
    private final ReservationViewService reservationViewService;
    private final CounterpartyProfileViewService counterpartyProfileViewService;
    private final LocaleMessages localeMessages;
    private final ReservationReviewFormValidator reservationReviewFormValidator;
    private final MyReservationDetailModelFactory reservationDetailFactory;
    private final ReviewSubmissionFacade reviewSubmissionFacade;
    private final ConsumerVehicleCardViewFactory consumerVehicleCardViewFactory;
    private final ImageService imageService;
    private final AppPaginationProperties appPaginationProperties;

    public MyReservationsController(
            final ReservationService reservationService,
            final ReservationViewService reservationViewService,
            final CounterpartyProfileViewService counterpartyProfileViewService,
            final LocaleMessages localeMessages,
            final ReservationReviewFormValidator reservationReviewFormValidator,
            final MyReservationDetailModelFactory reservationDetailFactory,
            final ReviewSubmissionFacade reviewSubmissionFacade,
            final ConsumerVehicleCardViewFactory consumerVehicleCardViewFactory,
            final ImageService imageService,
            final AppPaginationProperties appPaginationProperties) {
        this.reservationService = reservationService;
        this.reservationViewService = reservationViewService;
        this.counterpartyProfileViewService = counterpartyProfileViewService;
        this.localeMessages = localeMessages;
        this.reservationReviewFormValidator = reservationReviewFormValidator;
        this.reservationDetailFactory = reservationDetailFactory;
        this.reviewSubmissionFacade = reviewSubmissionFacade;
        this.consumerVehicleCardViewFactory = consumerVehicleCardViewFactory;
        this.imageService = imageService;
        this.appPaginationProperties = appPaginationProperties;
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
        final UriComponentsBuilder uri = UriComponentsBuilder
                .fromPath("/my-reservations/" + reservationId)
                .queryParam("role", role.toLegacyString());
        if (fromCar != null) {
            uri.queryParam("fromCar", fromCar);
        }
        return uri.build().toUriString();
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
            @RequestParam(defaultValue = "0") @Min(0) final int riderPage,
            @RequestParam(required = false) final List<Reservation.Status> riderStatus,
            @RequestParam(required = false) final List<Car.Type> category,
            @RequestParam(required = false) final List<Car.Transmission> transmission,
            @RequestParam(required = false) final List<Car.Powertrain> powertrain,
            @RequestParam(required = false) final BigDecimal priceMin,
            @RequestParam(required = false) final BigDecimal priceMax,
            @RequestParam(required = false) final List<String> rating,
            @RequestParam(required = false) final String sort,
            @RequestParam(required = false) final String q,
            final HttpServletRequest request) {
        final User me = WebAuthUtils.requireUser(currentUser);
        final String riderSort = MyHubSortSanitizer.sanitize(sort, DEFAULT_SORT);
        final var criteria = reservationService.buildReservationSearchCriteria(
                null, me.getId(), category, transmission, powertrain, priceMin, priceMax,
                rating, riderStatus, riderPage, appPaginationProperties.getDefaultPageSize(),
                riderSort, q, null);
        final RiderReservationsListPageModel pageModel = reservationViewService.loadRiderReservationsListPage(
                criteria, riderSort, LocaleContextHolder.getLocale());

        final int safeRiderPage = UiPaging.clampZeroBasedPage(
                riderPage, pageModel.getResultPage().getTotalItems(), pageModel.getResultPage().getPageSize());
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
        pageModel.populateModel(mav::addObject);
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
        return StoredFileDownloadResponses.inlineOr404(
                reservationService.findPaymentReceiptContentForParticipant(me.getId(), reservationId),
                "payment receipt reservationId=" + reservationId);
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
        return StoredFileDownloadResponses.inlineOr404(
                reservationService.findRefundReceiptContentForParticipant(me.getId(), reservationId),
                "refund receipt reservationId=" + reservationId);
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

    @GetMapping("/my-reservations/{reservationId}/edit")
    public ModelAndView editReservationForm(
            @CurrentUser final User currentUser,
            @PathVariable("reservationId") final long reservationId) {
        // The Spring Security filter (ReservationWebAuthorization#riderUnpaidPendingAccess)
        // already enforces that the caller is the rider on a still-unpaid PENDING reservation.
        // The view-service call below repeats the same check (defense in depth) and quietly
        // redirects to the detail page when state has shifted between the filter and the handler.
        final User me = WebAuthUtils.requireUser(currentUser);
        final Optional<ReservationEditPageModel> editOpt =
                reservationViewService.loadRiderEditReservationPage(
                        me.getId(), reservationId, LocaleContextHolder.getLocale());
        if (editOpt.isEmpty()) {
            return new ModelAndView(redirectToMyReservationDetailView(
                    reservationId, ReservationViewerRole.RIDER, null));
        }
        final ModelAndView mav = new ModelAndView("reservation/myReservationEdit");
        editOpt.get().populateModel(mav::addObject);
        mav.addObject("activeTab", "my-reservations");
        return mav;
    }

    @PostMapping("/my-reservations/{reservationId}/edit")
    public ModelAndView submitReservationEdit(
            @CurrentUser final User currentUser,
            @PathVariable("reservationId") final long reservationId,
            @RequestParam(name = "fromDateTime", required = false) final String fromDateTime,
            @RequestParam(name = "untilDateTime", required = false) final String untilDateTime,
            final RedirectAttributes redirectAttributes) {
        final User me = WebAuthUtils.requireUser(currentUser);
        try {
            reservationService.editPendingReservationByRider(
                    me.getId(), reservationId, fromDateTime, untilDateTime);
            redirectAttributes.addFlashAttribute(
                    "reservationEditSuccess",
                    localeMessages.msg("myReservationEdit.savedFlash"));
        } catch (final RydenException e) {
            redirectAttributes.addFlashAttribute("reservationEditError", localeMessages.msg(e));
            return new ModelAndView(new RedirectView(
                    "/my-reservations/" + reservationId + "/edit", true));
        }
        return new ModelAndView(redirectToMyReservationDetailView(
                reservationId, ReservationViewerRole.RIDER, null));
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
        try {
            reservationService.cancelReservationAsParticipantScoped(
                    me.getId(), reservationId, role.toLegacyString());
        } catch (final ReservationAccessDeniedException e) {
            return new ModelAndView(new RedirectView("/my-reservations", true));
        } catch (final ReservationCancelNotAllowedException e) {
            redirectAttributes.addFlashAttribute(
                    "cancelReservationError", localeMessages.msg(e));
            return new ModelAndView(redirectToMyReservationDetailView(reservationId, role, fromCar));
        }
        // PRG: render the cancellation acknowledgement page from a GET so a refresh does not
        // resubmit the cancel; the role flag is carried as a query param (it only affects the
        // "back" link target on the confirmation view).
        final String cancelledUrl = UriComponentsBuilder
                .fromPath("/my-reservations/cancelled")
                .queryParam("role", role.toLegacyString())
                .build()
                .toUriString();
        return new ModelAndView(new RedirectView(cancelledUrl, true));
    }

    @GetMapping("/my-reservations/cancelled")
    public ModelAndView cancelledAcknowledgement(
            @CurrentUser final User currentUser,
            @RequestParam(name = "role", required = false) final ReservationViewerRole roleParam) {
        WebAuthUtils.requireUser(currentUser);
        final ReservationViewerRole role = roleParam != null ? roleParam : ReservationViewerRole.RIDER;
        final ModelAndView mav = new ModelAndView("reservation/reservationCancelled");
        mav.addObject("reservationRole", role.toLegacyString());
        return mav;
    }
}

