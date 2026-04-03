package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.models.AvailabilityPeriod;
import ar.edu.itba.paw.models.Car;
import ar.edu.itba.paw.models.ListingCard;
import ar.edu.itba.paw.models.ListingSearchCriteria;
import ar.edu.itba.paw.services.ListingService;
import ar.edu.itba.paw.webapp.dto.VehicleCardView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class SearchController {

    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter DT_LOCAL = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

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

        final ListingSearchCriteria criteria =
                buildCriteria(query, category, transmission, powertrain, price, from, until);
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

    private static ListingSearchCriteria buildCriteria(
            final String query,
            final List<String> category,
            final List<String> transmission,
            final List<String> powertrain,
            final List<String> price,
            final String from,
            final String until) {
        final List<String> transmissions = collectTransmissionParams(transmission);
        final List<String> powertrains = collectPowertrainParams(powertrain);
        final List<String> mergedCarTypes = collectCarTypeParams(category);
        final List<String> bands = new ArrayList<>();
        if (price != null) {
            for (final String p : price) {
                if (p == null || p.isBlank()) {
                    continue;
                }
                final String u = p.trim().toUpperCase();
                if ("FREE".equals(u) || "PAID".equals(u)) {
                    bands.add(u);
                }
            }
        }
        Instant rangeStart = parseAvailabilityRangeStart(from);
        Instant rangeEndExclusive = parseAvailabilityRangeEndExclusive(until);
        if (rangeStart != null && rangeEndExclusive != null && !rangeEndExclusive.isAfter(rangeStart)) {
            final Instant rs = parseAvailabilityRangeStart(until);
            final Instant re = parseAvailabilityRangeEndExclusive(from);
            rangeStart = rs;
            rangeEndExclusive = re;
        }
        if (rangeStart == null || rangeEndExclusive == null || !rangeEndExclusive.isAfter(rangeStart)) {
            rangeStart = null;
            rangeEndExclusive = null;
        }
        return new ListingSearchCriteria(
                query, transmissions, powertrains, mergedCarTypes, bands, rangeStart, rangeEndExclusive);
    }

    private static List<String> collectCarTypeParams(final List<String> raw) {
        final List<String> out = new ArrayList<>();
        if (raw == null) {
            return out;
        }
        for (final String s : raw) {
            if (s == null || s.isBlank()) {
                continue;
            }
            final String u = s.trim().toUpperCase();
            try {
                Car.Type.valueOf(u);
                if (!out.contains(u)) {
                    out.add(u);
                }
            } catch (final IllegalArgumentException ignored) {
                // ignore
            }
        }
        return out;
    }

    private static List<String> collectTransmissionParams(final List<String> raw) {
        final List<String> out = new ArrayList<>();
        if (raw == null) {
            return out;
        }
        for (final String s : raw) {
            if (s == null || s.isBlank()) {
                continue;
            }
            final String u = s.trim().toUpperCase();
            try {
                Car.Transmission.valueOf(u);
                out.add(u);
            } catch (final IllegalArgumentException ignored) {
                // ignore
            }
        }
        return out;
    }

    private static List<String> collectPowertrainParams(final List<String> raw) {
        final List<String> out = new ArrayList<>();
        if (raw == null) {
            return out;
        }
        for (final String s : raw) {
            if (s == null || s.isBlank()) {
                continue;
            }
            final String u = s.trim().toUpperCase();
            try {
                Car.Powertrain.valueOf(u);
                out.add(u);
            } catch (final IllegalArgumentException ignored) {
                // ignore
            }
        }
        return out;
    }

    private static Instant parseAvailabilityRangeStart(final String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        final String t = s.trim();
        try {
            if (!t.contains("T")) {
                return LocalDate.parse(t, ISO_DATE).atStartOfDay(AvailabilityPeriod.WALL_ZONE).toInstant();
            }
            return LocalDateTime.parse(t, DT_LOCAL).atZone(AvailabilityPeriod.WALL_ZONE).toInstant();
        } catch (final DateTimeParseException e) {
            return null;
        }
    }


    private static Instant parseAvailabilityRangeEndExclusive(final String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        final String t = s.trim();
        try {
            if (!t.contains("T")) {
                return LocalDate.parse(t, ISO_DATE).plusDays(1).atStartOfDay(AvailabilityPeriod.WALL_ZONE).toInstant();
            }
            return LocalDateTime.parse(t, DT_LOCAL).atZone(AvailabilityPeriod.WALL_ZONE).plusMinutes(1).toInstant();
        } catch (final DateTimeParseException e) {
            return null;
        }
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

