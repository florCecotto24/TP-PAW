package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.models.ListingCard;
import ar.edu.itba.paw.models.Page;
import ar.edu.itba.paw.services.ListingService;
import ar.edu.itba.paw.webapp.dto.VehicleCardView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
public class SearchController {

    private static final String DEFAULT_SORT = "date,desc";
    private static final Set<String> VALID_SORTS = Set.of("date,desc", "date,asc", "price,asc", "price,desc");

    private final ListingService listingService;

    @Autowired
    public SearchController(final ListingService listingService) {
        this.listingService = listingService;
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
            @RequestParam(required = false) final String sort) {
        final ModelAndView mav = new ModelAndView("search");

        page = Math.max(0, page);


        final var criteria = listingService.buildSearchCriteria(
                query, category, transmission, powertrain, price, from, until, page, sort);
        final Page<ListingCard> resultPage = listingService.searchListingCards(criteria);
        final List<VehicleCardView> results = resultPage.getContent().stream()
                .map(SearchController::toVehicleCardView)
                .collect(Collectors.toList());

        final String safeSort = VALID_SORTS.contains(sort) ? sort : DEFAULT_SORT;
        mav.addObject("results", results);
        mav.addObject("searchPage", resultPage);
        mav.addObject("currentSort", safeSort);
        mav.addObject("activeTab", "search");

        return mav;
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
