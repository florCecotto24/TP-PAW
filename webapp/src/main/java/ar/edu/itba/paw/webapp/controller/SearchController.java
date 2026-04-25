package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.models.ListingCard;
import ar.edu.itba.paw.models.Page;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.services.ListingService;
import ar.edu.itba.paw.services.LocationService;
import ar.edu.itba.paw.webapp.dto.VehicleCardView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
public class SearchController {

    private static final String DEFAULT_SORT = "date,desc";
    private static final Set<String> VALID_SORTS = Set.of("date,desc", "date,asc", "price,asc", "price,desc");

    private final ListingService listingService;
    private final LocationService locationService;

    @Autowired
    public SearchController(final ListingService listingService, final LocationService locationService) {
        this.listingService = listingService;
        this.locationService = locationService;
    }

    @RequestMapping(value = "/search", method = RequestMethod.GET)
    public ModelAndView search(
            @RequestParam(required = false) final String query,
            @RequestParam(required = false) final List<String> category,
            @RequestParam(required = false) final List<String> transmission,
            @RequestParam(required = false) final List<String> powertrain,
            @RequestParam(required = false) final List<String> price,
            @RequestParam(required = false) final String from,
            @RequestParam(required = false) final String until,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) final String sort,
            @RequestParam(required = false) final List<String> neighborhoodId,
            @ModelAttribute(name = LoggedUserAdvice.CURRENT_USER_MODEL_KEY, binding = false) final User currentUser,
            final HttpServletRequest request) {
        final ModelAndView mav = new ModelAndView("search");

        page = Math.max(0, page);

        final User viewer = currentUser;

        final List<Long> neighborhoodIds = locationService.resolveSearchNeighborhoodIds(neighborhoodId);
        final var criteria = listingService.buildSearchCriteria(
                query, category, transmission, powertrain, price, from, until, page, sort, viewer, neighborhoodIds);
        final Page<ListingCard> resultPage = listingService.searchListingCards(criteria);

        final int lastPage = resultPage.getTotalPages() - 1;
        if (page > lastPage) {
            final String redirectUrl = UriComponentsBuilder
                    .fromHttpRequest(new ServletServerHttpRequest(request))
                    .replaceQueryParam("page", lastPage)
                    .build()
                    .toUriString();
            final RedirectView redirectView = new RedirectView(redirectUrl);
            redirectView.setExposeModelAttributes(false);
            return new ModelAndView(redirectView);
        }
        final List<VehicleCardView> results = resultPage.getContent().stream()
                .map(SearchController::toVehicleCardView)
                .collect(Collectors.toList());

        final String safeSort = sort != null && VALID_SORTS.contains(sort) ? sort : DEFAULT_SORT;
        mav.addObject("results", results);
        mav.addObject("searchPage", resultPage);
        mav.addObject("currentSort", safeSort);
        mav.addObject("activeTab", "search");
        mav.addObject("hasActiveSearchFilters", hasActiveSearchFilters(request, neighborhoodIds));

        return mav;
    }

    private static boolean hasActiveSearchFilters(final HttpServletRequest request, final List<Long> neighborhoodIds) {
        if (nonBlank(request.getParameter("query"))) {
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
                || hasAnyValues(request.getParameterValues("price"));
    }

    private static boolean nonBlank(final String s) {
        return s != null && !s.isBlank();
    }

    private static boolean hasAnyValues(final String[] values) {
        return values != null && values.length > 0;
    }

    private static VehicleCardView toVehicleCardView(final ListingCard card) {
        return new VehicleCardView(
                card.getListingId(),
                card.getBrand(),
                card.getModel(),
                card.getDayPrice(),
                card.getImageId());
    }
}
