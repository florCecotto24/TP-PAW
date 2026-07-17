package ar.edu.itba.paw.webapp.support;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;

import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.springframework.stereotype.Component;

import ar.edu.itba.paw.models.domain.car.Car;
import ar.edu.itba.paw.models.domain.reservation.Reservation;
import ar.edu.itba.paw.models.dto.Page;
import ar.edu.itba.paw.models.dto.reservation.ReservationCard;
import ar.edu.itba.paw.models.util.search.MyHubSortSanitizer;
import ar.edu.itba.paw.models.util.search.ReservationSearchCriteria;
import ar.edu.itba.paw.services.reservation.ReservationService;
import ar.edu.itba.paw.services.user.AdminService;
import ar.edu.itba.paw.webapp.api.common.PaginationLinks;
import ar.edu.itba.paw.webapp.api.common.VndMediaType;
import ar.edu.itba.paw.webapp.dto.rest.LinksDto;
import ar.edu.itba.paw.webapp.security.auth.userdetails.RydenUserDetails;
import ar.edu.itba.paw.webapp.util.RestUriUtils;

/**
 * Query parsing / Accept negotiation for reservation collections and item teasers.
 * Keeps {@code ReservationController} free of sort-token and enum-parse loops.
 */
@Component
public final class ReservationListSupport {

    private static final String DEFAULT_HUB_SORT = "date,desc";

    private final ReservationService reservationService;
    private final AdminService adminService;
    private final ReservationResourceAccess reservationResourceAccess;

    public ReservationListSupport(
            final ReservationService reservationService,
            final AdminService adminService,
            final ReservationResourceAccess reservationResourceAccess) {
        this.reservationService = reservationService;
        this.adminService = adminService;
        this.reservationResourceAccess = reservationResourceAccess;
    }

    public Response listForViewer(
            final PaginationParams paging,
            final Long riderId,
            final Long ownerId,
            final Long carId,
            final List<String> status,
            final List<String> riderStatus,
            final List<String> category,
            final List<String> transmission,
            final List<String> powertrain,
            final BigDecimal priceMin,
            final BigDecimal priceMax,
            final List<String> rating,
            final String q,
            final String sort,
            final RydenUserDetails viewer,
            final UriInfo uriInfo) {
        final Page<ReservationCard> resultPage;
        if (riderId != null) {
            reservationResourceAccess.requireSelfOrAdmin(riderId, viewer);
            resultPage = reservationService.getRiderReservationCards(buildListCriteria(
                    null, riderId, carId, status, riderStatus, category, transmission, powertrain,
                    priceMin, priceMax, rating, q, paging.getZeroBasedPage(), paging.getPageSize(), sort));
        } else if (ownerId != null) {
            reservationResourceAccess.requireSelfOrAdmin(ownerId, viewer);
            resultPage = reservationService.getOwnerReservationCards(buildListCriteria(
                    ownerId, null, carId, status, riderStatus, category, transmission, powertrain,
                    priceMin, priceMax, rating, q, paging.getZeroBasedPage(), paging.getPageSize(), sort));
        } else {
            reservationResourceAccess.requireAdmin();
            resultPage = adminService.listAllReservations(buildListCriteria(
                    null, null, carId, status, riderStatus, category, transmission, powertrain,
                    priceMin, priceMax, rating, q, paging.getZeroBasedPage(), paging.getPageSize(), sort));
        }
        return pagedReservations(resultPage, paging, uriInfo);
    }

    public ReservationSearchCriteria buildListCriteria(
            final Long ownerId,
            final Long riderId,
            final Long carId,
            final List<String> status,
            final List<String> riderStatus,
            final List<String> category,
            final List<String> transmission,
            final List<String> powertrain,
            final BigDecimal priceMin,
            final BigDecimal priceMax,
            final List<String> rating,
            final String q,
            final int page,
            final int pageSize,
            final String sort) {
        return reservationService.buildReservationSearchCriteria(
                ownerId,
                riderId,
                toCarTypes(category),
                toTransmissions(transmission),
                toPowertrains(powertrain),
                priceMin,
                priceMax,
                rating,
                parseStatuses(status, riderStatus),
                page,
                pageSize,
                toHubSort(sort),
                q,
                carId);
    }

    public Response pagedReservations(
            final Page<ReservationCard> page, final PaginationParams paging, final UriInfo uriInfo) {
        if (page.getTotalItems() == 0L) {
            return Response.noContent().build();
        }
        final List<LinksDto> links = page.getContent().stream()
                .map(card -> LinksDto.ofSelf(
                        RestUriUtils.reservationUri(uriInfo, card.getReservationId()).toString()))
                .collect(Collectors.toList());
        final Response.ResponseBuilder builder = Response.ok(new GenericEntity<List<LinksDto>>(links) {})
                .type(VndMediaType.RESERVATION_LINKS_V1_JSON)
                .header("X-Total-Count", page.getTotalItems());
        PaginationLinks.add(builder, uriInfo, paging.getPage(), paging.getPageSize(), (int) page.getTotalItems());
        return builder.build();
    }

    public boolean acceptsReservationSummary(final HttpHeaders httpHeaders) {
        if (httpHeaders == null) {
            return false;
        }
        final List<MediaType> acceptable = httpHeaders.getAcceptableMediaTypes();
        if (acceptable == null || acceptable.isEmpty()) {
            return false;
        }
        final MediaType summary = MediaType.valueOf(VndMediaType.RESERVATION_SUMMARY_V1_JSON);
        final MediaType full = MediaType.valueOf(VndMediaType.RESERVATION_V1_JSON);
        boolean wantsSummary = false;
        boolean wantsFull = false;
        for (final MediaType candidate : acceptable) {
            if (candidate.isCompatible(summary)) {
                wantsSummary = true;
            }
            if (candidate.isCompatible(full)) {
                wantsFull = true;
            }
        }
        return wantsSummary && !wantsFull;
    }

    static String toHubSort(final String sort) {
        if (sort == null || sort.isBlank() || "recent".equalsIgnoreCase(sort)) {
            return DEFAULT_HUB_SORT;
        }
        if ("start_date".equalsIgnoreCase(sort)) {
            return "date,asc";
        }
        if ("price_asc".equalsIgnoreCase(sort)) {
            return "price,asc";
        }
        if ("price_desc".equalsIgnoreCase(sort)) {
            return "price,desc";
        }
        return MyHubSortSanitizer.sanitize(sort, DEFAULT_HUB_SORT);
    }

    private static List<Car.Type> toCarTypes(final List<String> raw) {
        return toDistinctCarEnums(raw, CarRestEnums::parseType);
    }

    private static List<Car.Transmission> toTransmissions(final List<String> raw) {
        return toDistinctCarEnums(raw, CarRestEnums::parseTransmission);
    }

    private static List<Car.Powertrain> toPowertrains(final List<String> raw) {
        return toDistinctCarEnums(raw, CarRestEnums::parsePowertrain);
    }

    private static <E> List<E> toDistinctCarEnums(
            final List<String> raw,
            final java.util.function.Function<String, E> parser) {
        if (raw == null || raw.isEmpty()) {
            return List.of();
        }
        final LinkedHashSet<E> out = new LinkedHashSet<>();
        for (final String token : raw) {
            if (token != null && !token.isBlank()) {
                out.add(parser.apply(token));
            }
        }
        return new ArrayList<>(out);
    }

    private static List<Reservation.Status> parseStatuses(
            final List<String> status,
            final List<String> riderStatus) {
        final LinkedHashSet<Reservation.Status> out = new LinkedHashSet<>();
        addStatusTokens(out, status);
        addStatusTokens(out, riderStatus);
        return new ArrayList<>(out);
    }

    private static void addStatusTokens(
            final LinkedHashSet<Reservation.Status> out,
            final List<String> tokens) {
        if (tokens == null) {
            return;
        }
        for (final String token : tokens) {
            if (token == null || token.isBlank()) {
                continue;
            }
            out.add(ReservationRestEnums.parseStatus(token));
        }
    }
}
