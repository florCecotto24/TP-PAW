package ar.edu.itba.paw.webapp.controller.reservation;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;

import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.PATCH;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ar.edu.itba.paw.exception.MessageKeys;
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
import ar.edu.itba.paw.webapp.config.properties.AppPaginationProperties;
import ar.edu.itba.paw.webapp.dto.rest.ReservationDto;
import ar.edu.itba.paw.webapp.form.reservation.ReservationCreateForm;
import ar.edu.itba.paw.webapp.form.reservation.ReservationPatchForm;
import ar.edu.itba.paw.webapp.security.auth.userdetails.RydenUserDetails;
import ar.edu.itba.paw.webapp.support.CarRestEnums;
import ar.edu.itba.paw.webapp.support.CurrentUserResolver;
import ar.edu.itba.paw.webapp.support.ReservationResourceAccess;
import ar.edu.itba.paw.webapp.support.ReservationRestDateTimes;
import ar.edu.itba.paw.webapp.support.ReservationRestEnums;
import ar.edu.itba.paw.webapp.support.RestUriPaths;
import ar.edu.itba.paw.webapp.util.RestUriUtils;
import ar.edu.itba.paw.webapp.validation.constraint.car.ValidCarPowertrainList;
import ar.edu.itba.paw.webapp.validation.constraint.car.ValidCarTransmissionList;
import ar.edu.itba.paw.webapp.validation.constraint.car.ValidCarTypeList;
import ar.edu.itba.paw.webapp.validation.constraint.reservation.ValidReservationStatusList;

/**
 * Reservations resource ({@code /reservations}, {@code /reservations/{id}}).
 */
@Path("/reservations")
@Component
public final class ReservationController {

    private static final String DEFAULT_HUB_SORT = "date,desc";

    private final ReservationService reservationService;
    private final AdminService adminService;
    private final CurrentUserResolver currentUserResolver;
    private final ReservationResourceAccess reservationResourceAccess;
    private final AppPaginationProperties paginationProperties;

    @Context
    private UriInfo uriInfo;

    @Autowired
    public ReservationController(
            final ReservationService reservationService,
            final AdminService adminService,
            final CurrentUserResolver currentUserResolver,
            final ReservationResourceAccess reservationResourceAccess,
            final AppPaginationProperties paginationProperties) {
        this.reservationService = reservationService;
        this.adminService = adminService;
        this.currentUserResolver = currentUserResolver;
        this.reservationResourceAccess = reservationResourceAccess;
        this.paginationProperties = paginationProperties;
    }

    @GET
    @Produces(VndMediaType.RESERVATION_V1_JSON)
    public Response listReservations(
            @QueryParam("page") @DefaultValue("1") final int page,
            @QueryParam("pageSize") final Integer pageSizeParam,
            @QueryParam("riderId") final Long riderId,
            @QueryParam("ownerId") final Long ownerId,
            @QueryParam("carId") final Long carId,
            @QueryParam("status") @ValidReservationStatusList final List<String> status,
            @QueryParam("riderStatus") @ValidReservationStatusList final List<String> riderStatus,
            @QueryParam("q") final String q,
            @QueryParam("category") @ValidCarTypeList final List<String> category,
            @QueryParam("transmission") @ValidCarTransmissionList final List<String> transmission,
            @QueryParam("powertrain") @ValidCarPowertrainList final List<String> powertrain,
            @QueryParam("priceMin") final BigDecimal priceMin,
            @QueryParam("priceMax") final BigDecimal priceMax,
            @QueryParam("rating") final List<String> rating,
            @QueryParam("sort") final String sort) {
        final int safePage = Math.max(1, page);
        final int pageSize = pageSizeParam != null && pageSizeParam > 0
                ? pageSizeParam
                : paginationProperties.getDefaultPageSize();
        final int zeroBasedPage = safePage - 1;
        final RydenUserDetails viewer = currentUserResolver.currentPrincipalOrNull();

        final Page<ReservationCard> resultPage;
        if (riderId != null) {
            reservationResourceAccess.requireSelfOrAdmin(riderId, viewer);
            resultPage = reservationService.getRiderReservationCards(buildListCriteria(
                    null, riderId, carId, status, riderStatus, category, transmission, powertrain,
                    priceMin, priceMax, rating, q, zeroBasedPage, pageSize, sort));
        } else if (ownerId != null) {
            reservationResourceAccess.requireSelfOrAdmin(ownerId, viewer);
            resultPage = reservationService.getOwnerReservationCards(buildListCriteria(
                    ownerId, null, carId, status, riderStatus, category, transmission, powertrain,
                    priceMin, priceMax, rating, q, zeroBasedPage, pageSize, sort));
        } else {
            reservationResourceAccess.requireAdmin();
            resultPage = adminService.listAllReservations(zeroBasedPage, pageSize);
        }

        return pagedReservations(resultPage, safePage, pageSize);
    }

    @GET
    @Path("/{id}")
    @Produces(VndMediaType.RESERVATION_V1_JSON)
    public Response getReservation(@PathParam("id") final long id) {
        final RydenUserDetails viewer = currentUserResolver.currentPrincipalOrNull();
        reservationResourceAccess.requireViewReservation(id, viewer);

        final Reservation reservation;
        if (reservationResourceAccess.isAdmin()) {
            reservation = adminService.getReservationById(id)
                    .orElseThrow(() -> new javax.ws.rs.NotFoundException(MessageKeys.RESERVATION_RIDER_LISTING_NOT_FOUND));
        } else {
            final long userId = viewer.getUserId();
            reservation = reservationService.getRiderReservationById(userId, id)
                    .or(() -> reservationService.getOwnerReservationById(userId, id))
                    .orElseThrow(() -> new javax.ws.rs.NotFoundException(MessageKeys.RESERVATION_RIDER_LISTING_NOT_FOUND));
        }

        final long ownerId = reservation.getCar().getOwnerId();
        return Response.ok(ReservationDto.from(reservation, ownerId, uriInfo)).build();
    }

    @POST
    @Consumes(VndMediaType.RESERVATION_V1_JSON)
    @Produces(VndMediaType.RESERVATION_V1_JSON)
    public Response createReservation(@Valid final ReservationCreateForm form) {
        final long riderId = currentUserResolver.requireUserId();
        final long carId = RestUriPaths.parseCarId(form.getCarUri());
        final RestUriPaths.CarAvailabilityIds availabilityIds =
                RestUriPaths.parseAvailabilityUri(form.getAvailabilityUri());
        final String from = ReservationRestDateTimes.toWallLocalInput(form.getStartDate());
        final String until = ReservationRestDateTimes.toWallLocalInput(form.getEndDate());
        final Reservation created = reservationService.submitRiderReservationByCar(
                riderId, carId, availabilityIds.availabilityId(), from, until);
        final long ownerId = created.getCar().getOwnerId();
        final ReservationDto dto = ReservationDto.from(created, ownerId, uriInfo);
        return Response.created(RestUriUtils.reservationUri(uriInfo, created.getId()))
                .entity(dto)
                .build();
    }

    @PATCH
    @Path("/{id}")
    @Consumes(VndMediaType.RESERVATION_V1_JSON)
    @Produces(VndMediaType.RESERVATION_V1_JSON)
    public Response patchReservation(
            @PathParam("id") final long id,
            @Valid final ReservationPatchForm form) {
        final RydenUserDetails viewer = currentUserResolver.requirePrincipal();
        reservationResourceAccess.requireViewReservation(id, viewer);

        if (form.getStatus() != null && !form.getStatus().isBlank()) {
            applyStatusPatch(id, viewer, form.getStatus());
        }
        if (Boolean.TRUE.equals(form.getCarReturned())) {
            reservationResourceAccess.requireOwner(id, viewer);
            reservationService.markCarReturnedByOwner(viewer.getUserId(), id);
        }
        if (form.getStartDate() != null) {
            reservationResourceAccess.requireRider(id, viewer);
            reservationService.editPendingReservationByRider(
                    viewer.getUserId(),
                    id,
                    ReservationRestDateTimes.toWallLocalInput(form.getStartDate()),
                    ReservationRestDateTimes.toWallLocalInput(form.getEndDate()));
        }

        return Response.ok(toReservationDto(id, viewer)).build();
    }

    private void applyStatusPatch(final long id, final RydenUserDetails viewer, final String statusRaw) {
        final Reservation.Status status = ReservationRestEnums.parseStatus(statusRaw);
        if (status == Reservation.Status.CANCELLED_BY_RIDER) {
            reservationService.cancelReservationAsParticipantScoped(viewer.getUserId(), id, "rider");
            return;
        }
        if (status == Reservation.Status.CANCELLED_BY_OWNER) {
            reservationService.cancelReservationAsParticipantScoped(viewer.getUserId(), id, "owner");
            return;
        }
    }

    private ReservationDto toReservationDto(final long id, final RydenUserDetails viewer) {
        final Reservation reservation;
        if (reservationResourceAccess.isAdmin()) {
            reservation = adminService.getReservationById(id)
                    .orElseThrow(() -> new javax.ws.rs.NotFoundException(MessageKeys.RESERVATION_RIDER_LISTING_NOT_FOUND));
        } else {
            final long userId = viewer.getUserId();
            reservation = reservationService.getRiderReservationById(userId, id)
                    .or(() -> reservationService.getOwnerReservationById(userId, id))
                    .orElseThrow(() -> new javax.ws.rs.NotFoundException(MessageKeys.RESERVATION_RIDER_LISTING_NOT_FOUND));
        }
        return ReservationDto.from(reservation, reservation.getCar().getOwnerId(), uriInfo);
    }

    private ReservationSearchCriteria buildListCriteria(
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

    private static String toHubSort(final String sort) {
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

    private Response pagedReservations(
            final Page<ReservationCard> page, final int safePage, final int pageSize) {
        if (page.getTotalItems() == 0L) {
            return Response.noContent().build();
        }
        final List<ReservationDto> dtos = page.getContent().stream()
                .map(card -> ReservationDto.fromCard(card, uriInfo))
                .collect(Collectors.toList());
        final Response.ResponseBuilder builder = Response.ok(new GenericEntity<List<ReservationDto>>(dtos) {})
                .header("X-Total-Count", page.getTotalItems());
        PaginationLinks.add(builder, uriInfo, safePage, pageSize, (int) page.getTotalItems());
        return builder.build();
    }
}
