package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.models.dto.car.detail.CarDetailPageModel;
import ar.edu.itba.paw.models.dto.profile.CounterpartyActiveListingsFragment;
import ar.edu.itba.paw.models.dto.profile.CounterpartyProfilePageModel;
import ar.edu.itba.paw.models.domain.User;
import ar.edu.itba.paw.services.CarDetailViewService;
import ar.edu.itba.paw.services.CounterpartyProfileViewService;
import ar.edu.itba.paw.webapp.security.auth.AuthenticationAuthorities;
import ar.edu.itba.paw.webapp.support.ConsumerVehicleCardViewFactory;
import ar.edu.itba.paw.webapp.support.CurrentUser;
import ar.edu.itba.paw.webapp.dto.VehicleCardView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.support.RequestContextUtils;
import org.springframework.web.servlet.view.RedirectView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Optional;

/** Public car detail, reviews pagination, similar cars, and bookable availability JSON for pickers. */
@Controller
public class CarDetailController {

    private final CarDetailViewService carDetailViewService;
    private final CounterpartyProfileViewService counterpartyProfileViewService;
    private final ConsumerVehicleCardViewFactory consumerVehicleCardViewFactory;

    @Autowired
    public CarDetailController(
            final CarDetailViewService carDetailViewService,
            final CounterpartyProfileViewService counterpartyProfileViewService,
            final ConsumerVehicleCardViewFactory consumerVehicleCardViewFactory) {
        this.carDetailViewService = carDetailViewService;
        this.counterpartyProfileViewService = counterpartyProfileViewService;
        this.consumerVehicleCardViewFactory = consumerVehicleCardViewFactory;
    }

    @GetMapping("/cars/{carId}")
    public ModelAndView carDetail(
            @PathVariable final long carId,
            @RequestParam(name = "reviewPage", defaultValue = "0") final int reviewPage,
            @RequestParam(name = "reviewsView", required = false) final String reviewsViewParam,
            @RequestParam(name = "from", required = false) final String fromDateParam,
            @RequestParam(name = "until", required = false) final String untilDateParam,
            @RequestParam(name = "searchNbId", required = false) final List<Long> searchNeighborhoodIds,
            @CurrentUser final User currentUser,
            final Authentication authentication,
            final HttpServletRequest request) {

        final boolean currentUserIsAdmin = AuthenticationAuthorities.hasAdminRole(authentication);
        // Page model assembly (8 services + visibility rules + review formatting + favorite
        // flag) lives in CarDetailViewService; the controller stays focused on HTTP concerns:
        // request params (date defaults, search context, admin action form) and the view-layer
        // VehicleCardView conversion that the JSP tag requires.
        final Optional<CarDetailPageModel> pageOpt = carDetailViewService.loadCarDetailPage(
                carId,
                currentUser,
                currentUserIsAdmin,
                reviewPage,
                reviewsViewParam,
                RequestContextUtils.getLocale(request));
        if (pageOpt.isEmpty()) {
            return new ModelAndView(new RedirectView("/search", true));
        }
        final CarDetailPageModel pageModel = pageOpt.get();
        final ModelAndView mav = new ModelAndView("car/carDetail");
        pageModel.populateModel(mav::addObject);
        // Webapp-only concerns that the service deliberately does not know about:
        final List<VehicleCardView> similarListings = consumerVehicleCardViewFactory.toConsumerVehicleCardViews(
                pageModel.getSimilarListings(), currentUser == null ? null : currentUser.getId());
        mav.addObject("similarListings", similarListings);
        mav.addObject("reservationFromDefault", fromDateParam != null ? fromDateParam : "");
        mav.addObject("reservationUntilDefault", untilDateParam != null ? untilDateParam : "");
        mav.addObject("searchNeighborhoodIds", searchNeighborhoodIds != null ? searchNeighborhoodIds : List.of());
        if (currentUserIsAdmin) {
            mav.addObject("adminActionForm", new java.util.HashMap<>());
        }
        return mav;
    }

    @GetMapping("/users/{userId}/profile")
    public ModelAndView ownerProfile(
            @PathVariable final long userId,
            @RequestParam(name = "carId", required = false) final Long currentCarId,
            @CurrentUser final User viewer) {
        // Page model assembly (counterparty header + reviews + active listings + load-more state)
        // lives in CounterpartyProfileViewService; the controller only redirects on miss and wires
        // the view-layer card conversion that the JSP demands.
        final Optional<CounterpartyProfilePageModel> profileOpt =
                counterpartyProfileViewService.loadPublicCounterpartyProfile(
                        userId, currentCarId, LocaleContextHolder.getLocale());
        if (profileOpt.isEmpty()) {
            return new ModelAndView(new RedirectView("/search", true));
        }
        final CounterpartyProfilePageModel profile = profileOpt.get();
        final ModelAndView mav = new ModelAndView("profile/counterpartyProfile");
        profile.populateModel(mav::addObject);
        final List<VehicleCardView> counterpartyActiveListings =
                consumerVehicleCardViewFactory.toConsumerVehicleCardViews(
                        profile.getActiveOwnerCarCards(), viewer == null ? null : viewer.getId());
        mav.addObject("counterpartyActiveListings", counterpartyActiveListings);
        mav.addObject("activeTab", "explore");
        return mav;
    }

    /**
     * HTML fragment: next page of active listings for the counterparty profile grid (see {@code counterparty-profile.js}).
     */
    @GetMapping("/users/{userId}/active-listings")
    public ModelAndView counterpartyActiveListingsPage(
            @PathVariable final long userId,
            @RequestParam(name = "carId", required = false) final Long excludeCarId,
            @RequestParam("page") final int page,
            @CurrentUser final User viewer,
            final HttpServletResponse response) {
        final Optional<CounterpartyActiveListingsFragment> fragmentOpt =
                counterpartyProfileViewService.loadCounterpartyActiveListingsPage(userId, excludeCarId, page);
        if (fragmentOpt.isEmpty()) {
            // Service rejected the request: bad page number or unknown user. Mirror the previous
            // controller-side behaviour for the two error responses.
            response.setStatus(page < 1 ? HttpServletResponse.SC_BAD_REQUEST : HttpServletResponse.SC_NOT_FOUND);
            return emptyCounterpartyListingsFragment();
        }
        final CounterpartyActiveListingsFragment fragment = fragmentOpt.get();
        final List<VehicleCardView> cards =
                consumerVehicleCardViewFactory.toConsumerVehicleCardViews(
                        fragment.getCards(), viewer == null ? null : viewer.getId());
        final ModelAndView mav = new ModelAndView("profile/counterpartyActiveCarCols");
        mav.addObject("counterpartyActiveListings", cards);
        mav.addObject("fragmentHasMore", fragment.isHasMore());
        mav.addObject("fragmentNextPage", fragment.getNextPage());
        return mav;
    }

    private static ModelAndView emptyCounterpartyListingsFragment() {
        final ModelAndView mav = new ModelAndView("profile/counterpartyActiveCarCols");
        mav.addObject("counterpartyActiveListings", List.of());
        mav.addObject("fragmentHasMore", false);
        mav.addObject("fragmentNextPage", 0);
        return mav;
    }

}
