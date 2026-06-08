package ar.edu.itba.paw.webapp.controller.reservation;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.web.util.UriComponentsBuilder;

import ar.edu.itba.paw.exception.reservation.ReservationException;
import ar.edu.itba.paw.models.domain.reservation.Reservation;
import ar.edu.itba.paw.models.domain.user.User;
import ar.edu.itba.paw.models.dto.reservation.ReservationConfirmationPageModel;
import ar.edu.itba.paw.models.dto.reservation.ReservationFormPageModel;
import ar.edu.itba.paw.services.reservation.ReservationService;
import ar.edu.itba.paw.services.reservation.view.ReservationFormViewService;
import ar.edu.itba.paw.webapp.form.reservation.ReservationForm;
import ar.edu.itba.paw.webapp.support.CurrentUser;
import ar.edu.itba.paw.webapp.support.facade.UserBookingDocumentsFacade;
import ar.edu.itba.paw.webapp.util.LocaleMessages;
import ar.edu.itba.paw.webapp.util.WebAuthUtils;
import ar.edu.itba.paw.webapp.validation.ValidationGroups;

/**
 * Rider flow to create a reservation for a car (GET form + POST submit). The bundle of services
 * that used to live in this controller was extracted into {@link ReservationFormViewService}; the
 * controller is left with input parsing, validation branching, and the {@code submitRider}
 * domain call.
 */
@Controller
public final class ReservationFormController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReservationFormController.class);

    private static final String FORM_VIEW = "reservation/reservationForm";
    private static final String CONFIRMATION_VIEW = "reservation/reservationConfirmation";

    private final ReservationFormViewService reservationFormViewService;
    private final ReservationService reservationService;
    private final LocaleMessages localeMessages;
    private final UserBookingDocumentsFacade userBookingDocumentsFacade;

    public ReservationFormController(
            final ReservationFormViewService reservationFormViewService,
            final ReservationService reservationService,
            final LocaleMessages localeMessages,
            final UserBookingDocumentsFacade userBookingDocumentsFacade) {
        this.reservationFormViewService = reservationFormViewService;
        this.reservationService = reservationService;
        this.localeMessages = localeMessages;
        this.userBookingDocumentsFacade = userBookingDocumentsFacade;
    }

    @GetMapping("/cars/{carId}/reservation/new")
    public ModelAndView index(
            @CurrentUser final User currentUser,
            @PathVariable final long carId,
            @RequestParam(value = "availabilityId", required = false) final Long availabilityId,
            @RequestParam(value = "carName", required = false) final String carName,
            @RequestParam(value = "fromDateTime", required = false) final String fromDateTime,
            @RequestParam(value = "untilDateTime", required = false) final String untilDateTime,
            @RequestParam(value = "reservationTotal", required = false) final String reservationTotal,
            @ModelAttribute("reservationForm") final ReservationForm form) {
        final User rider = WebAuthUtils.requireUser(currentUser);
        final Optional<ReservationFormPageModel> pageOpt = reservationFormViewService.loadReservationFormPage(
                carId, rider, fromDateTime, untilDateTime, reservationTotal, carName, LocaleContextHolder.getLocale());
        if (pageOpt.isEmpty()) {
            return redirectToSearch();
        }
        return renderForm(pageOpt.get(), form, availabilityId, fromDateTime, untilDateTime, null);
    }

    /**
     * Saves identity/license from the reservation modal (same validation as profile); responds without a full page reload.
     */
    @PostMapping(value = "/reservation/booking-documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> bookingDocuments(
            @CurrentUser final User currentUser,
            @RequestParam(name = "licenseFile", required = false) final MultipartFile licenseFile,
            @RequestParam(name = "identityFile", required = false) final MultipartFile identityFile) {
        final User me = WebAuthUtils.requireUser(currentUser);
        final var pair = userBookingDocumentsFacade.attemptBookingPair(me.getId(), licenseFile, identityFile, true);
        if (pair.getFirstHardError().isPresent()) {
            return ResponseEntity.badRequest().build();
        }
        final var state = pair.getStateAfter();
        if (state.hasBoth()) {
            return ResponseEntity.noContent().build();
        }
        if (!pair.uploadedSomething()) {
            return ResponseEntity.badRequest().build();
        }
        final HttpHeaders headers = new HttpHeaders();
        headers.add("X-Ryden-Needs-License", state.needsLicense() ? "true" : "false");
        headers.add("X-Ryden-Needs-Identity", state.needsIdentity() ? "true" : "false");
        return new ResponseEntity<>(null, headers, HttpStatus.ACCEPTED);
    }

    @PostMapping("/reservation")
    public ModelAndView formSubmit(
            @CurrentUser final User currentUser,
            @Validated(ValidationGroups.OnReservationSubmit.class) @ModelAttribute("reservationForm") final ReservationForm form,
            final BindingResult errors,
            @RequestParam(value = "availabilityId", required = false) final Long availabilityId,
            @RequestParam(value = "reservationTotal", required = false) final String reservationTotal) {

        // carId is declared @NotNull(OnReservationSubmit) on the form, so a null binding here is already a
        // BindingResult field error: bounce to /search (no in-flight reservation to render) instead of
        // duplicating the null guard.
        if (errors.hasFieldErrors("carId")) {
            return redirectToSearch();
        }
        final long carId = form.getCarId();
        final User rider = WebAuthUtils.requireUser(currentUser);

        // Every error branch (validation errors, blank carName, ReservationException) re-renders
        // the same form bundle. We resolve the page model up front and reuse it.
        final Optional<ReservationFormPageModel> pageOpt = reservationFormViewService.loadReservationFormPage(
                carId, rider, form.getFromDateTime(), form.getUntilDateTime(), reservationTotal,
                form.getCarName(), LocaleContextHolder.getLocale());
        if (pageOpt.isEmpty()) {
            return redirectToSearch();
        }
        final ReservationFormPageModel page = pageOpt.get();

        if (errors.hasErrors()) {
            return renderForm(page, form, availabilityId, form.getFromDateTime(), form.getUntilDateTime(), null);
        }

        final Reservation reservation;
        try {
            LOGGER.atInfo()
                    .addArgument(rider.getId())
                    .addArgument(carId)
                    .addArgument(availabilityId)
                    .addArgument(form.getFromDateTime())
                    .addArgument(form.getUntilDateTime())
                    .log("Submitting reservation for riderId={}, carId={}, availabilityId={}, fromDateTime={}, untilDateTime={}");
            reservation = reservationService.submitRiderReservationByCar(
                    rider.getId(),
                    carId,
                    availabilityId,
                    form.getFromDateTime(),
                    form.getUntilDateTime());
        } catch (final ReservationException e) {
            return renderForm(page, form, availabilityId, form.getFromDateTime(), form.getUntilDateTime(),
                    localeMessages.msg(e));
        }

        // PRG: redirect to the confirmation GET so a browser refresh re-renders the confirmation
        // from the persisted reservation instead of resubmitting the form.
        final UriComponentsBuilder target = UriComponentsBuilder
                .fromPath("/cars/" + carId + "/reservation/" + reservation.getId() + "/confirmation");
        if (availabilityId != null) {
            target.queryParam("availabilityId", availabilityId);
        }
        return new ModelAndView(new RedirectView(target.build().toUriString(), true));
    }

    @GetMapping("/cars/{carId}/reservation/{reservationId}/confirmation")
    public ModelAndView confirmationGet(
            @CurrentUser final User currentUser,
            @PathVariable final long carId,
            @PathVariable final long reservationId,
            @RequestParam(value = "availabilityId", required = false) final Long availabilityId) {
        final User rider = WebAuthUtils.requireUser(currentUser);
        final Optional<ReservationConfirmationPageModel> confirmationOpt =
                reservationFormViewService.loadReservationConfirmationForRider(
                        rider.getId(), reservationId, availabilityId, LocaleContextHolder.getLocale());
        if (confirmationOpt.isEmpty()) {
            return new ModelAndView(new RedirectView("/my-reservations", true));
        }
        final ReservationConfirmationPageModel confirmation = confirmationOpt.get();
        if (confirmation.getCarId() != carId) {
            // The path embeds carId for human-readable URLs; if it does not match the rider's
            // reservation, fall back to the canonical URL on the rider's hub.
            return new ModelAndView(new RedirectView("/my-reservations/" + reservationId, true));
        }
        final ModelAndView mav = new ModelAndView(CONFIRMATION_VIEW);
        confirmation.populateModel(mav::addObject);
        return mav;
    }

    /**
     * Single rendering path for the four reservationForm scenarios (initial render and the three
     * error branches of {@code formSubmit}). Echoes back webapp-only params (availabilityId, the
     * raw datetime inputs, the optional reservation error) that the page model intentionally
     * does not own.
     */
    private ModelAndView renderForm(
            final ReservationFormPageModel page,
            final ReservationForm form,
            final Long availabilityId,
            final String fromDateTime,
            final String untilDateTime,
            final String reservationErrorOrNull) {
        // Synchronise the form-binding object with the resolved car-name and pickup location so
        // the JSP renders consistent defaults on first render and on re-renders after errors.
        form.setCarId(page.getCar().getId());
        form.setCarName(page.getResolvedCarName());
        form.setDeliveryLocation(page.getDeliveryLocation());
        form.setFromDateTime(fromDateTime);
        form.setUntilDateTime(untilDateTime);

        final ModelAndView mav = new ModelAndView(FORM_VIEW);
        page.populateModel(mav::addObject);
        mav.addObject("availabilityId", availabilityId);
        if (reservationErrorOrNull != null) {
            mav.addObject("reservationError", reservationErrorOrNull);
        }
        return mav;
    }

    private ModelAndView redirectToSearch() {
        return new ModelAndView(new RedirectView("/search", true));
    }
}
