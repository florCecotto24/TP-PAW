package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.models.domain.User;
import ar.edu.itba.paw.models.dto.CarCard;
import ar.edu.itba.paw.models.dto.Page;
import ar.edu.itba.paw.models.pagination.UiPaging;
import ar.edu.itba.paw.services.CarService;
import ar.edu.itba.paw.services.LocationService;
import ar.edu.itba.paw.webapp.support.ConsumerVehicleCardViewFactory;
import ar.edu.itba.paw.webapp.support.CurrentUser;
import ar.edu.itba.paw.webapp.dto.VehicleCardView;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

/** Public listing search and filters with canonical sort tokens and pagination. */
@Controller
public final class SearchController {

    private static final String DEFAULT_SORT = "date,desc";
    private static final Set<String> VALID_SORTS = Set.of(
            "date,desc", "date,asc", "price,asc", "price,desc", "rating,asc", "rating,desc");

    private final CarService carService;
    private final LocationService locationService;
    private final ConsumerVehicleCardViewFactory consumerVehicleCardViewFactory;

    public SearchController(
            final CarService carService,
            final LocationService locationService,
            final ConsumerVehicleCardViewFactory consumerVehicleCardViewFactory) {
        this.carService = carService;
        this.locationService = locationService;
        this.consumerVehicleCardViewFactory = consumerVehicleCardViewFactory;
    }

    @RequestMapping(value = "/search", method = RequestMethod.GET)
    public ModelAndView search(
            @RequestParam(required = false) final String query,
            @RequestParam(required = false) final List<String> category,
            @RequestParam(required = false) final List<String> transmission,
            @RequestParam(required = false) final List<String> powertrain,
            @RequestParam(required = false) final BigDecimal priceMin,
            @RequestParam(required = false) final BigDecimal priceMax,
            @RequestParam(required = false) final List<String> rating,
            @RequestParam(required = false) final String from,
            @RequestParam(required = false) final String until,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) final String sort,
            @RequestParam(required = false) final List<String> neighborhoodId,
            @RequestParam(defaultValue = "false") final boolean flexible,
            @RequestParam(required = false) final String flexMonth,
            @RequestParam(required = false) final Integer flexDays,
            @CurrentUser final User currentUser,
            final HttpServletRequest request) {
        final ModelAndView mav = new ModelAndView("search");

        page = Math.max(0, page);

        final User viewer = currentUser;

        final List<Long> neighborhoodIds = locationService.resolveSearchNeighborhoodIds(neighborhoodId);
        final var criteria = carService.buildSearchCriteria(
                query, category, transmission, powertrain, priceMin, priceMax, rating, from, until, page, sort,
                viewer, neighborhoodIds, flexible, flexMonth, flexDays);
        final Page<CarCard> resultPage = carService.searchCarCards(criteria);

        final int safePage = UiPaging.clampZeroBasedPage(page, resultPage.getTotalItems(), resultPage.getPageSize());
        if (safePage != page) {
            final String redirectUrl = UriComponentsBuilder
                    .fromHttpRequest(new ServletServerHttpRequest(request))
                    .replaceQueryParam("page", safePage)
                    .build()
                    .toUriString();
            final RedirectView redirectView = new RedirectView(redirectUrl);
            redirectView.setExposeModelAttributes(false);
            return new ModelAndView(redirectView);
        }
        final List<VehicleCardView> results =
                consumerVehicleCardViewFactory.toConsumerVehicleCardViews(resultPage.getContent());

        final String safeSort = sort != null && VALID_SORTS.contains(sort) ? sort : DEFAULT_SORT;
        mav.addObject("results", results);
        mav.addObject("searchPage", resultPage);
        mav.addObject("currentSort", safeSort);
        mav.addObject("activeTab", "search");
        mav.addObject("hasActiveSearchFilters", hasActiveSearchFilters(request, neighborhoodIds));
        mav.addObject("searchFlexible", flexible);
        mav.addObject("searchFlexMonth", flexMonth != null ? flexMonth : "");
        mav.addObject("searchFlexDays", flexDays);

        return mav;
    }

    private static boolean hasActiveSearchFilters(final HttpServletRequest request, final List<Long> neighborhoodIds) {
        if (nonBlank(request.getParameter("query"))) {
            return true;
        }
        if ("true".equalsIgnoreCase(request.getParameter("flexible")) && nonBlank(request.getParameter("flexMonth"))) {
            return true;
        }
        if (nonBlank(request.getParameter("from")) || nonBlank(request.getParameter("until"))) {
            return true;
        }
        if (neighborhoodIds != null && !neighborhoodIds.isEmpty()) {
            return true;
        }
        return hasAnyValues(request.getParameterValues("category"))
                || hasAnyValues(request.getParameterValues("transmission"))
                || hasAnyValues(request.getParameterValues("powertrain"))
                || nonBlank(request.getParameter("priceMin"))
                || nonBlank(request.getParameter("priceMax"))
                || hasAnyValues(request.getParameterValues("rating"));
    }

    private static boolean nonBlank(final String s) {
        return s != null && !s.isBlank();
    }

    private static boolean hasAnyValues(final String[] values) {
        return values != null && values.length > 0;
    }

}
