package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.models.ListingCard;
import ar.edu.itba.paw.models.Page;
import ar.edu.itba.paw.services.ListingService;
import ar.edu.itba.paw.webapp.dto.VehicleCardView;
import ar.edu.itba.paw.webapp.util.CarEnumOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;
import java.util.stream.Collectors;

@Controller
public class SearchController {

    private final ListingService listingService;
    private final CarEnumOptions carEnumOptions;

    @Autowired
    public SearchController(final ListingService listingService, final CarEnumOptions carEnumOptions) {
        this.listingService = listingService;
        this.carEnumOptions = carEnumOptions;
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

        mav.addObject("categoryFilterOptions", carEnumOptions.carTypeSelectOptions());
        mav.addObject("transmissionFilterOptions", carEnumOptions.transmissionSelectOptions());
        mav.addObject("powertrainFilterOptions", carEnumOptions.powertrainSelectOptions());
        mav.addObject("priceFilterOptions", carEnumOptions.searchPriceBandOptions());

        final var criteria = listingService.buildSearchCriteria(
                query, category, transmission, powertrain, price, from, until, page, sort);
        final Page<ListingCard> resultPage = listingService.searchListingCards(criteria);
        final List<VehicleCardView> results = resultPage.getContent().stream()
                .map(SearchController::toVehicleCardView)
                .collect(Collectors.toList());

        mav.addObject("results", results);
        mav.addObject("searchPage", resultPage);
        mav.addObject("currentSort", sort != null ? sort : "date,desc");
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
