package ar.edu.itba.paw.webapp.controller.car;

import ar.edu.itba.paw.models.domain.user.User;
import ar.edu.itba.paw.models.dto.car.detail.CarDetailPageModel;
import ar.edu.itba.paw.models.dto.profile.CounterpartyActiveListingsFragment;
import ar.edu.itba.paw.models.dto.profile.CounterpartyProfilePageModel;
import ar.edu.itba.paw.services.car.view.CarDetailViewService;
import ar.edu.itba.paw.services.user.view.CounterpartyProfileViewService;
import ar.edu.itba.paw.webapp.config.properties.AppPaginationProperties;
import ar.edu.itba.paw.webapp.dto.VehicleCardView;
import ar.edu.itba.paw.webapp.security.auth.AuthenticationAuthorities;
import ar.edu.itba.paw.webapp.support.ConsumerVehicleCardViewFactory;
import ar.edu.itba.paw.webapp.support.CurrentUser;
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

import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** Public car detail, reviews pagination, similar cars, and bookable availability JSON for pickers. */
@Controller
public class CarDetailController {

    private final CarDetailViewService carDetailViewService;
    private final CounterpartyProfileViewService counterpartyProfileViewService;
    private final ConsumerVehicleCardViewFactory consumerVehicleCardViewFactory;
    private final AppPaginationProperties appPaginationProperties;

    @Autowired
    public CarDetailController(
            final CarDetailViewService carDetailViewService,
            final CounterpartyProfileViewService counterpartyProfileViewService,
            final ConsumerVehicleCardViewFactory consumerVehicleCardViewFactory,
            final AppPaginationProperties appPaginationProperties) {
        this.carDetailViewService = carDetailViewService;
        this.counterpartyProfileViewService = counterpartyProfileViewService;
        this.consumerVehicleCardViewFactory = consumerVehicleCardViewFactory;
        this.appPaginationProperties = appPaginationProperties;
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

        // viewerIsAdmin stays as a business input: CarDetailViewService uses it to expose pages
        // for blocked owners only to the owner themselves or to an admin. JSP rendering of the
        // admin action panel uses <sec:authorize hasRole('ADMIN')> instead of a model flag, so the
        // controller no longer leaks that decision into the view layer.
        final boolean viewerIsAdmin = AuthenticationAuthorities.hasAdminRole(authentication);
        final Optional<CarDetailPageModel> pageOpt = carDetailViewService.loadCarDetailPage(
                carId,
                currentUser,
                viewerIsAdmin,
                reviewPage,
                appPaginationProperties.getCarPublicReviewsPageSize(),
                reviewsViewParam,
                RequestContextUtils.getLocale(request));
        if (pageOpt.isEmpty()) {
            return new ModelAndView(new RedirectView("/search", true));
        }
        final CarDetailPageModel pageModel = pageOpt.get();
        final ModelAndView mav = new ModelAndView("car/carDetail");
        pageModel.populateModel(mav::addObject);
        final List<VehicleCardView> similarListings = consumerVehicleCardViewFactory.toConsumerVehicleCardViews(
                pageModel.getSimilarListings(), currentUser == null ? null : currentUser.getId());
        mav.addObject("similarListings", similarListings);
        mav.addObject("reservationFromDefault", fromDateParam != null ? fromDateParam : "");
        mav.addObject("reservationUntilDefault", untilDateParam != null ? untilDateParam : "");
        mav.addObject("searchNeighborhoodIds", searchNeighborhoodIds != null ? searchNeighborhoodIds : List.of());
        // The admin section in the JSP uses <form:form modelAttribute="adminActionForm">, which
        // requires the attribute to exist whenever the section is rendered. The actual
        // rendering decision lives in <sec:authorize hasRole('ADMIN')>; supplying an empty
        // map unconditionally keeps the form binding contract intact without inspecting roles
        // in the controller.
        mav.addObject("adminActionForm", new HashMap<>());
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
