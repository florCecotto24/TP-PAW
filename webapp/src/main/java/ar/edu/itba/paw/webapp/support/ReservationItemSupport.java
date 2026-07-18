package ar.edu.itba.paw.webapp.support;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.springframework.stereotype.Component;

import ar.edu.itba.paw.exception.MessageKeys;
import ar.edu.itba.paw.models.domain.reservation.Reservation;
import ar.edu.itba.paw.models.domain.user.User;
import ar.edu.itba.paw.models.dto.reservation.ReservationCard;
import ar.edu.itba.paw.services.reservation.ReservationService;
import ar.edu.itba.paw.services.user.AdminService;
import ar.edu.itba.paw.webapp.api.common.VndMediaType;
import ar.edu.itba.paw.webapp.dto.rest.CounterpartyContactDto;
import ar.edu.itba.paw.webapp.dto.rest.ReservationDto;
import ar.edu.itba.paw.webapp.dto.rest.ReservationSummaryDto;
import ar.edu.itba.paw.webapp.form.reservation.ReservationCreateForm;
import ar.edu.itba.paw.webapp.form.reservation.ReservationPatchForm;
import ar.edu.itba.paw.webapp.security.auth.userdetails.RydenUserDetails;
import ar.edu.itba.paw.webapp.util.RestUriUtils;

/**
 * Item-level HTTP orchestration for reservations (GET/POST/PATCH and viewer-scoped load).
 * ACL predicates stay on {@link ReservationResourceAccess}; no domain rules live here.
 */
@Component
public final class ReservationItemSupport {

    private final ReservationService reservationService;
    private final AdminService adminService;
    private final ReservationResourceAccess reservationResourceAccess;
    private final ReservationDtoEnricher reservationDtoEnricher;
    private final ReservationListSupport reservationListSupport;

    public ReservationItemSupport(
            final ReservationService reservationService,
            final AdminService adminService,
            final ReservationResourceAccess reservationResourceAccess,
            final ReservationDtoEnricher reservationDtoEnricher,
            final ReservationListSupport reservationListSupport) {
        this.reservationService = reservationService;
        this.adminService = adminService;
        this.reservationResourceAccess = reservationResourceAccess;
        this.reservationDtoEnricher = reservationDtoEnricher;
        this.reservationListSupport = reservationListSupport;
    }

    public Response getItem(
            final long reservationId,
            final RydenUserDetails viewer,
            final HttpHeaders httpHeaders,
            final UriInfo uriInfo) {
        if (reservationListSupport.acceptsReservationSummary(httpHeaders)) {
            final ReservationCard card = reservationService.findReservationCardById(reservationId)
                    .orElseThrow(() -> new NotFoundException(MessageKeys.RESERVATION_RIDER_LISTING_NOT_FOUND));
            return Response.ok(ReservationSummaryDto.fromCard(card, uriInfo))
                    .type(VndMediaType.RESERVATION_SUMMARY_V1_JSON)
                    .build();
        }
        final Reservation reservation = requireViewableReservation(reservationId, viewer);
        return Response.ok(toDto(reservation, uriInfo))
                .type(VndMediaType.RESERVATION_V1_JSON)
                .build();
    }

    public Response create(
            final long riderId,
            final ReservationCreateForm form,
            final UriInfo uriInfo) {
        final long carId = RestUriPaths.parseCarId(form.getCarUri());
        final RestUriPaths.CarAvailabilityIds availabilityIds =
                RestUriPaths.parseAvailabilityUri(form.getAvailabilityUri());
        final String from = ReservationRestDateTimes.toWallLocalInput(form.getStartDate());
        final String until = ReservationRestDateTimes.toWallLocalInput(form.getEndDate());
        final Reservation created = reservationService.submitRiderReservationByCar(
                riderId, carId, availabilityIds.availabilityId(), from, until);
        return Response.created(RestUriUtils.reservationUri(uriInfo, created.getId()))
                .entity(toDto(created, uriInfo))
                .build();
    }

    public Response patch(
            final long reservationId,
            final ReservationPatchForm form,
            final RydenUserDetails viewer,
            final UriInfo uriInfo) {
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
                reservationId,
                cancellationStatus,
                form.getCarReturned(),
                fromWall,
                untilWall);
        return Response.ok(toDto(updated, uriInfo)).build();
    }

    public Response getCounterparty(
            final long reservationId,
            final RydenUserDetails viewer,
            final UriInfo uriInfo) {
        final Reservation reservation = requireViewableReservation(reservationId, viewer);
        final User counterparty = reservationDtoEnricher
                .resolveCounterparty(reservation, viewer.getUserId())
                .orElseThrow(NotFoundException::new);
        return Response.ok(CounterpartyContactDto.from(counterparty, uriInfo)).build();
    }

    public Reservation requireViewableReservation(final long reservationId, final RydenUserDetails viewer) {
        if (reservationResourceAccess.isAdmin()) {
            return adminService.getReservationById(reservationId)
                    .orElseThrow(() -> new NotFoundException(MessageKeys.RESERVATION_RIDER_LISTING_NOT_FOUND));
        }
        final long userId = viewer.getUserId();
        return reservationService.getRiderReservationById(userId, reservationId)
                .or(() -> reservationService.getOwnerReservationById(userId, reservationId))
                .orElseThrow(() -> new NotFoundException(MessageKeys.RESERVATION_RIDER_LISTING_NOT_FOUND));
    }

    public ReservationDto toDto(final Reservation reservation, final UriInfo uriInfo) {
        final long ownerId = reservation.getCar().getOwnerId();
        return reservationDtoEnricher.toDto(reservation, ownerId, uriInfo);
    }
}
