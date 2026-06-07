package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.models.domain.Car;
import ar.edu.itba.paw.models.domain.User;
import ar.edu.itba.paw.models.dto.car.CarCard;
import ar.edu.itba.paw.models.dto.Page;
import ar.edu.itba.paw.models.pagination.UiPaging;
import ar.edu.itba.paw.models.util.search.MyHubSortSanitizer;
import ar.edu.itba.paw.services.car.CarService;
import ar.edu.itba.paw.services.location.LocationService;
import ar.edu.itba.paw.webapp.config.properties.AppPaginationProperties;
import ar.edu.itba.paw.webapp.support.ConsumerVehicleCardViewFactory;
import ar.edu.itba.paw.webapp.support.CurrentUser;
import ar.edu.itba.paw.webapp.dto.VehicleCardView;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.Min;
import java.math.BigDecimal;
import java.util.List;

/** Public listing search and filters with canonical sort tokens and pagination. */
@Controller
@Validated
public class SearchController {

    private static final String DEFAULT_SORT = "date,desc";

    private final CarService carService;
    private final LocationService locationService;
    private final ConsumerVehicleCardViewFactory consumerVehicleCardViewFactory;
    private final AppPaginationProperties appPaginationProperties;

    public SearchController(
            final CarService carService,
            final LocationService locationService,
            final ConsumerVehicleCardViewFactory consumerVehicleCardViewFactory,
            final AppPaginationProperties appPaginationProperties) {
        this.carService = carService;
        this.locationService = locationService;
        this.consumerVehicleCardViewFactory = consumerVehicleCardViewFactory;
        this.appPaginationProperties = appPaginationProperties;
    }

    @RequestMapping(value = "/search", method = RequestMethod.GET)
    public ModelAndView search(
            @RequestParam(required = false) final String query,
            @RequestParam(required = false) final List<Car.Type> category,
            @RequestParam(required = false) final List<Car.Transmission> transmission,
            @RequestParam(required = false) final List<Car.Powertrain> powertrain,
            @RequestParam(required = false) final BigDecimal priceMin,
            @RequestParam(required = false) final BigDecimal priceMax,
            @RequestParam(required = false) final List<String> rating,
            @RequestParam(required = false) final String from,
            @RequestParam(required = false) final String until,
            @RequestParam(defaultValue = "0") @Min(0) final int page,
            @RequestParam(required = false) final String sort,
            @RequestParam(required = false) final List<String> neighborhoodId,
            @RequestParam(defaultValue = "false") final boolean flexible,
            @RequestParam(required = false) final String flexMonth,
            @RequestParam(required = false) final Integer flexDays,
            @CurrentUser final User currentUser,
            final HttpServletRequest request) {
        final ModelAndView mav = new ModelAndView("search");

        final User viewer = currentUser;

        final List<Long> neighborhoodIds = locationService.resolveSearchNeighborhoodIds(neighborhoodId);
        final var criteria = carService.buildSearchCriteria(
                query, category, transmission, powertrain, priceMin, priceMax, rating, from, until, page,
                appPaginationProperties.getUiPageSize(), sort,
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
                consumerVehicleCardViewFactory.toConsumerVehicleCardViews(
                        resultPage.getContent(), viewer == null ? null : viewer.getId());

        final String safeSort = MyHubSortSanitizer.sanitize(sort, DEFAULT_SORT);
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
