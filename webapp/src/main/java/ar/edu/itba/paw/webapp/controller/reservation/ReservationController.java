package ar.edu.itba.paw.webapp.controller.reservation;

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
import ar.edu.itba.paw.models.domain.reservation.Reservation;
import ar.edu.itba.paw.models.dto.Page;
import ar.edu.itba.paw.models.dto.reservation.ReservationCard;
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
import ar.edu.itba.paw.webapp.support.CurrentUserResolver;
import ar.edu.itba.paw.webapp.support.ReservationResourceAccess;
import ar.edu.itba.paw.webapp.support.ReservationRestDateTimes;
import ar.edu.itba.paw.webapp.support.ReservationRestEnums;
import ar.edu.itba.paw.webapp.support.RestUriPaths;
import ar.edu.itba.paw.webapp.util.RestUriUtils;

/**
 * Reservations resource ({@code /reservations}, {@code /reservations/{id}}).
 */
@Path("/reservations")
@Component
public final class ReservationController {

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
            @QueryParam("status") final String status,
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
            resultPage = reservationService.getRiderReservationCards(buildCriteria(
                    null, riderId, carId, status, zeroBasedPage, pageSize, sort));
        } else if (ownerId != null) {
            reservationResourceAccess.requireSelfOrAdmin(ownerId, viewer);
            resultPage = reservationService.getOwnerReservationCards(buildCriteria(
                    ownerId, null, carId, status, zeroBasedPage, pageSize, sort));
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
        if (availabilityIds.carId() != carId) {
            throw new javax.ws.rs.BadRequestException("availabilityUri does not belong to carUri.");
        }
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
        final Reservation.Status status;
        try {
            status = ReservationRestEnums.parseStatus(statusRaw);
        } catch (final IllegalArgumentException ex) {
            throw new javax.ws.rs.BadRequestException(ex.getMessage());
        }
        if (status == Reservation.Status.CANCELLED_BY_RIDER) {
            reservationService.cancelReservationAsParticipantScoped(viewer.getUserId(), id, "rider");
            return;
        }
        if (status == Reservation.Status.CANCELLED_BY_OWNER) {
            reservationService.cancelReservationAsParticipantScoped(viewer.getUserId(), id, "owner");
            return;
        }
        throw new javax.ws.rs.BadRequestException("Unsupported status transition: " + statusRaw);
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

    private ReservationSearchCriteria buildCriteria(
            final Long ownerId,
            final Long riderId,
            final Long carId,
            final String status,
            final int page,
            final int pageSize,
            final String sort) {
        final List<String> statusFilters;
        if (status != null && !status.isBlank()) {
            try {
                statusFilters = List.of(ReservationRestEnums.parseStatus(status).name().toLowerCase());
            } catch (final IllegalArgumentException ex) {
                throw new javax.ws.rs.BadRequestException(ex.getMessage());
            }
        } else {
            statusFilters = List.of();
        }
        final String sortBy;
        final String sortDirection;
        if (sort == null || sort.isBlank() || "recent".equalsIgnoreCase(sort)) {
            sortBy = "date";
            sortDirection = "desc";
        } else if ("start_date".equalsIgnoreCase(sort)) {
            sortBy = "startDate";
            sortDirection = "asc";
        } else if ("price_asc".equalsIgnoreCase(sort)) {
            sortBy = "price";
            sortDirection = "asc";
        } else if ("price_desc".equalsIgnoreCase(sort)) {
            sortBy = "price";
            sortDirection = "desc";
        } else {
            throw new javax.ws.rs.BadRequestException("Unknown sort: " + sort);
        }
        return new ReservationSearchCriteria(
                ownerId,
                riderId,
                carId,
                page,
                pageSize,
                statusFilters,
                List.of(),
                List.of(),
                List.of(),
                null,
                null,
                List.of(),
                sortBy,
                sortDirection,
                null);
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
