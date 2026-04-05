package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.AvailabilityPeriod;
import ar.edu.itba.paw.models.Car;
import ar.edu.itba.paw.models.ListingSearchCriteria;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

@Service
public class ListingSearchCriteriaServiceImpl implements ListingSearchCriteriaService {

    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter DT_LOCAL = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    @Override
    public ListingSearchCriteria build(
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
                // ignore invalid enum
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
}
