package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.models.Car;
import ar.edu.itba.paw.models.ListingCard;
import ar.edu.itba.paw.services.ListingService;
import ar.edu.itba.paw.webapp.dto.VehicleCardView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class SearchController {

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
            @RequestParam(required = false) final String until) {
        final ModelAndView mav = new ModelAndView("search");

        mav.addObject("categoryFilterOptions", categoryFilterOptions());
        mav.addObject("transmissionFilterOptions", transmissionFilterOptions());
        mav.addObject("powertrainFilterOptions", powertrainFilterOptions());
        mav.addObject("priceFilterOptions", priceFilterOptions());

        final var criteria = listingService.buildSearchCriteria(
                query, category, transmission, powertrain, price, from, until);
        final List<VehicleCardView> results = listingService.searchListingCards(criteria).stream()
                .map(SearchController::toVehicleCardView)
                .collect(Collectors.toList());
        mav.addObject("results", results);
        mav.addObject("activeTab", "search");

        return mav;
    }

    private static List<Map<String, String>> categoryFilterOptions() {
        final List<Map<String, String>> opts = new ArrayList<>();
        for (final Car.Type t : Car.Type.values()) {
            opts.add(option(t.name(), humanizeEnum(t.name())));
        }
        return opts;
    }

    private static String humanizeEnum(final String enumName) {
        final String[] parts = enumName.toLowerCase().split("_");
        final StringBuilder sb = new StringBuilder();
        for (final String p : parts) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(Character.toUpperCase(p.charAt(0))).append(p, 1, p.length());
        }
        return sb.toString();
    }

    private static List<Map<String, String>> transmissionFilterOptions() {
        final List<Map<String, String>> opts = new ArrayList<>();
        opts.add(option("MANUAL", "Manual"));
        opts.add(option("AUTOMATIC", "Automatic"));
        opts.add(option("SEMI_AUTOMATIC", "Semi-automatic"));
        return opts;
    }

    private static List<Map<String, String>> powertrainFilterOptions() {
        final List<Map<String, String>> opts = new ArrayList<>();
        opts.add(option("HYBRID", "Hybrid"));
        opts.add(option("ELECTRIC", "Electric"));
        opts.add(option("DIESEL", "Diesel"));
        opts.add(option("GASOLINE", "Gasoline"));
        return opts;
    }

    private static List<Map<String, String>> priceFilterOptions() {
        final List<Map<String, String>> opts = new ArrayList<>();
        opts.add(option("FREE", "Free"));
        opts.add(option("PAID", "Paid"));
        return opts;
    }

    private static Map<String, String> option(final String value, final String label) {
        final Map<String, String> m = new LinkedHashMap<>();
        m.put("value", value);
        m.put("label", label);
        return m;
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
