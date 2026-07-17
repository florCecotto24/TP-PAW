package ar.edu.itba.paw.webapp.controller.reservation;

import java.math.BigDecimal;
import java.util.List;

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
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.method.P;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

import ar.edu.itba.paw.exception.MessageKeys;
import ar.edu.itba.paw.models.domain.reservation.Reservation;
import ar.edu.itba.paw.models.dto.Page;
import ar.edu.itba.paw.models.dto.reservation.ReservationCard;
import ar.edu.itba.paw.models.domain.user.User;
import ar.edu.itba.paw.services.reservation.ReservationService;
import ar.edu.itba.paw.services.user.AdminService;
import ar.edu.itba.paw.webapp.api.common.VndMediaType;
import ar.edu.itba.paw.webapp.dto.rest.CounterpartyContactDto;
import ar.edu.itba.paw.webapp.dto.rest.ReservationDto;
import ar.edu.itba.paw.webapp.dto.rest.ReservationSummaryDto;
import ar.edu.itba.paw.webapp.form.reservation.ReservationCreateForm;
import ar.edu.itba.paw.webapp.form.reservation.ReservationPatchForm;
import ar.edu.itba.paw.webapp.security.auth.userdetails.RydenUserDetails;
import ar.edu.itba.paw.webapp.support.CurrentUserResolver;
import ar.edu.itba.paw.webapp.support.PaginationParams;
import ar.edu.itba.paw.webapp.support.PaginationSupport;
import ar.edu.itba.paw.webapp.support.ReservationDtoEnricher;
import ar.edu.itba.paw.webapp.support.ReservationListSupport;
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
public class ReservationController {

    private final ReservationService reservationService;
    private final AdminService adminService;
    private final CurrentUserResolver currentUserResolver;
    private final ReservationResourceAccess reservationResourceAccess;
    private final PaginationSupport paginationSupport;
    private final ReservationDtoEnricher reservationDtoEnricher;
    private final ReservationListSupport reservationListSupport;

    @Context
    private UriInfo uriInfo;

    @Context
    private HttpHeaders httpHeaders;

    @Autowired
    public ReservationController(
            final ReservationService reservationService,
            final AdminService adminService,
            final CurrentUserResolver currentUserResolver,
            final ReservationResourceAccess reservationResourceAccess,
            final PaginationSupport paginationSupport,
            final ReservationDtoEnricher reservationDtoEnricher,
            final ReservationListSupport reservationListSupport) {
        this.reservationService = reservationService;
        this.adminService = adminService;
        this.currentUserResolver = currentUserResolver;
        this.reservationResourceAccess = reservationResourceAccess;
        this.paginationSupport = paginationSupport;
        this.reservationDtoEnricher = reservationDtoEnricher;
        this.reservationListSupport = reservationListSupport;
    }

    // A14 (audit): documented decision — a single collection whose visibility is a query-param
    // filter, not a different operation per role (see openapi.yaml for the full per-branch
    // breakdown): riderId (self-or-admin) / ownerId (self-or-admin) / neither (admin-only, every
    // reservation). Not a @PreAuthorize candidate: which check applies depends on *which* query
    // params the caller sent — a single method-level precondition can't express that three-way
    // routing, so it stays imperative, delegated to the same ReservationResourceAccess predicates
    // used everywhere else.
    @GET
    @Produces(VndMediaType.RESERVATION_LINKS_V1_JSON)
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
        final PaginationParams paging = paginationSupport.forDefaultCollection(page, pageSizeParam);
        final RydenUserDetails viewer = currentUserResolver.currentPrincipalOrNull();

        final Page<ReservationCard> resultPage;
        if (riderId != null) {
            reservationResourceAccess.requireSelfOrAdmin(riderId, viewer);
            resultPage = reservationService.getRiderReservationCards(reservationListSupport.buildListCriteria(
                    null, riderId, carId, status, riderStatus, category, transmission, powertrain,
                    priceMin, priceMax, rating, q, paging.getZeroBasedPage(), paging.getPageSize(), sort));
        } else if (ownerId != null) {
            reservationResourceAccess.requireSelfOrAdmin(ownerId, viewer);
            resultPage = reservationService.getOwnerReservationCards(reservationListSupport.buildListCriteria(
                    ownerId, null, carId, status, riderStatus, category, transmission, powertrain,
                    priceMin, priceMax, rating, q, paging.getZeroBasedPage(), paging.getPageSize(), sort));
        } else {
            reservationResourceAccess.requireAdmin();
            resultPage = adminService.listAllReservations(paging.getZeroBasedPage(), paging.getPageSize());
        }

        return reservationListSupport.pagedReservations(resultPage, paging, uriInfo);
    }

    @GET
    @Path("/{id}")
    @Produces({VndMediaType.RESERVATION_SUMMARY_V1_JSON, VndMediaType.RESERVATION_V1_JSON})
    @PreAuthorize("@reservationResourceAccess.canViewReservation(#id, @currentUserResolver.currentPrincipalOrNull())")
    public Response getReservation(@P("id") @PathParam("id") final long id) {
        final RydenUserDetails viewer = currentUserResolver.currentPrincipalOrNull();

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

        if (reservationListSupport.acceptsReservationSummary(httpHeaders)) {
            final ReservationCard card = reservationService.findReservationCardById(id)
                    .orElseThrow(() -> new javax.ws.rs.NotFoundException(MessageKeys.RESERVATION_RIDER_LISTING_NOT_FOUND));
            return Response.ok(ReservationSummaryDto.fromCard(card, uriInfo))
                    .type(VndMediaType.RESERVATION_SUMMARY_V1_JSON)
                    .build();
        }
        return Response.ok(toReservationDto(reservation, viewer))
                .type(VndMediaType.RESERVATION_V1_JSON)
                .build();
    }

    @GET
    @Path("/{id}/counterparty")
    @Produces(VndMediaType.COUNTERPARTY_CONTACT_V1_JSON)
    @PreAuthorize("@reservationResourceAccess.canViewReservation(#id, @currentUserResolver.currentPrincipalOrNull())")
    public Response getCounterparty(@P("id") @PathParam("id") final long id) {
        final RydenUserDetails viewer = currentUserResolver.currentPrincipalOrNull();
        final Reservation reservation = loadReservationForViewer(id, viewer);
        final User counterparty = reservationDtoEnricher
                .resolveCounterparty(reservation, viewer.getUserId())
                .orElseThrow(javax.ws.rs.NotFoundException::new);
        return Response.ok(CounterpartyContactDto.from(counterparty, uriInfo)).build();
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
        final RydenUserDetails viewer = currentUserResolver.requirePrincipal();
        final ReservationDto dto = toReservationDto(created, viewer);
        return Response.created(RestUriUtils.reservationUri(uriInfo, created.getId()))
                .entity(dto)
                .build();
    }

    // The base "must be a participant or admin" gate is declarative below; the per-field escalation
    // to rider/owner (carReturned, startDate) depends on which fields the caller actually sent in
    // the PATCH body, so those narrower checks stay imperative.
    @PATCH
    @Path("/{id}")
    @Consumes(VndMediaType.RESERVATION_V1_JSON)
    @Produces(VndMediaType.RESERVATION_V1_JSON)
    @PreAuthorize("@reservationResourceAccess.canViewReservation(#id, @currentUserResolver.currentPrincipalOrNull())")
    public Response patchReservation(
            @P("id") @PathParam("id") final long id,
            @Valid final ReservationPatchForm form) {
        final RydenUserDetails viewer = currentUserResolver.requirePrincipal();

        final Reservation.Status cancellationStatus = form.getStatus() != null && !form.getStatus().isBlank()
                ? ReservationRestEnums.parseStatus(form.getStatus())
                : null;
        final String fromWall = form.getStartDate() != null
                ? ReservationRestDateTimes.toWallLocalInput(form.getStartDate())
                : null;
        final String untilWall = form.getEndDate() != null
                ? ReservationRestDateTimes.toWallLocalInput(form.getEndDate())
                : null;
        final Reservation updated = reservationService.patchReservation(
                viewer.getUserId(),
                id,
                cancellationStatus,
                form.getCarReturned(),
                fromWall,
                untilWall);

        return Response.ok(toReservationDto(updated, viewer)).build();
    }

    private ReservationDto toReservationDto(final long id, final RydenUserDetails viewer) {
        return toReservationDto(loadReservationForViewer(id, viewer), viewer);
    }

    private Reservation loadReservationForViewer(final long id, final RydenUserDetails viewer) {
        if (reservationResourceAccess.isAdmin()) {
            return adminService.getReservationById(id)
                    .orElseThrow(() -> new javax.ws.rs.NotFoundException(MessageKeys.RESERVATION_RIDER_LISTING_NOT_FOUND));
        }
        final long userId = viewer.getUserId();
        return reservationService.getRiderReservationById(userId, id)
                .or(() -> reservationService.getOwnerReservationById(userId, id))
                .orElseThrow(() -> new javax.ws.rs.NotFoundException(MessageKeys.RESERVATION_RIDER_LISTING_NOT_FOUND));
    }

    private ReservationDto toReservationDto(final Reservation reservation, final RydenUserDetails viewer) {
        final long ownerId = reservation.getCar().getOwnerId();
        return reservationDtoEnricher.toDto(reservation, ownerId, uriInfo);
    }

}
