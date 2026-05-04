package ar.edu.itba.paw.webapp.support;

import java.util.Optional;

import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import ar.edu.itba.paw.models.dto.ReservationDetailPageModel;
import ar.edu.itba.paw.services.ReservationViewService;
import ar.edu.itba.paw.webapp.form.ReservationReviewForm;

@Component
public final class MyReservationDetailModelFactory {

    private final ReservationViewService reservationViewService;

    public MyReservationDetailModelFactory(final ReservationViewService reservationViewService) {
        this.reservationViewService = reservationViewService;
    }

    public ModelAndView detailWithForms(
            final ReservationDetailPageModel detail,
            final ReservationReviewForm ownerReviewForm,
            final BindingResult ownerReviewBinding,
            final ReservationReviewForm riderReviewForm,
            final BindingResult riderReviewBinding,
            final Long ownerListingHubForBreadcrumb) {
        final ModelAndView mav = new ModelAndView("myReservationDetail");
        detail.populateModel(mav::addObject);
        mav.addObject("activeTab", "my-reservations");
        if (ownerListingHubForBreadcrumb != null) {
            mav.addObject("reservationDetailOwnerListingHubId", ownerListingHubForBreadcrumb);
        }
        final ReservationReviewForm owner = ownerReviewForm != null ? ownerReviewForm : new ReservationReviewForm();
        final ReservationReviewForm rider = riderReviewForm != null ? riderReviewForm : new ReservationReviewForm();
        mav.addObject("ownerReviewForm", owner);
        mav.addObject("riderReviewForm", rider);
        if (ownerReviewBinding != null) {
            mav.addObject(BindingResult.MODEL_KEY_PREFIX + "ownerReviewForm", ownerReviewBinding);
        }
        if (riderReviewBinding != null) {
            mav.addObject(BindingResult.MODEL_KEY_PREFIX + "riderReviewForm", riderReviewBinding);
        }
        return mav;
    }

    public ModelAndView detailOrRedirect(
            final long viewerUserId,
            final long reservationId,
            final String viewerRole,
            final ReservationReviewForm ownerReviewForm,
            final BindingResult ownerReviewBinding,
            final ReservationReviewForm riderReviewForm,
            final BindingResult riderReviewBinding,
            final Long fromListing) {
        final Optional<ReservationDetailPageModel> detailOpt = reservationViewService.loadMyReservationDetailForViewer(
                viewerUserId, reservationId, viewerRole, LocaleContextHolder.getLocale());
        if (detailOpt.isEmpty()) {
            return new ModelAndView(new RedirectView("/my-reservations", true));
        }
        final ReservationDetailPageModel detail = detailOpt.get();
        final Long hub = ownerListingHubIfValid(fromListing, viewerRole, detail);
        return detailWithForms(
                detail,
                ownerReviewForm,
                ownerReviewBinding,
                riderReviewForm,
                riderReviewBinding,
                hub);
    }

    /**
     * When the owner opens reservation detail from {@code /my-listings/{id}/reservations}, {@code fromListing} matches
     * the reservation's listing; otherwise returns {@code null} (ignore tampered or stale params).
     */
    public static Long ownerListingHubIfValid(
            final Long fromListing,
            final String viewerRole,
            final ReservationDetailPageModel detail) {
        if (fromListing == null || !"owner".equals(viewerRole) || fromListing.longValue() != detail.getListingId()) {
            return null;
        }
        return fromListing;
    }
}
