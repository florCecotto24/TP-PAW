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

import ar.edu.itba.paw.webapp.api.common.VndMediaType;
import ar.edu.itba.paw.webapp.form.reservation.ReservationCreateForm;
import ar.edu.itba.paw.webapp.form.reservation.ReservationPatchForm;
import ar.edu.itba.paw.webapp.support.CurrentUserResolver;
import ar.edu.itba.paw.webapp.support.PaginationParams;
import ar.edu.itba.paw.webapp.support.PaginationSupport;
import ar.edu.itba.paw.webapp.support.ReservationItemSupport;
import ar.edu.itba.paw.webapp.support.ReservationListSupport;
import ar.edu.itba.paw.webapp.validation.constraint.car.ValidCarPowertrainList;
import ar.edu.itba.paw.webapp.validation.constraint.car.ValidCarTransmissionList;
import ar.edu.itba.paw.webapp.validation.constraint.car.ValidCarTypeList;
import ar.edu.itba.paw.webapp.validation.constraint.reservation.ValidReservationStatusList;

/**
 * Reservations resource ({@code /reservations}, {@code /reservations/{id}}).
 * HTTP routing only; list/item binding lives in {@code *Support} helpers.
 */
@Path("/reservations")
@Component
public class ReservationController {

    private final CurrentUserResolver currentUserResolver;
    private final PaginationSupport paginationSupport;
    private final ReservationListSupport reservationListSupport;
    private final ReservationItemSupport reservationItemSupport;

    @Context
    private UriInfo uriInfo;

    @Context
    private HttpHeaders httpHeaders;

    @Autowired
    public ReservationController(
            final CurrentUserResolver currentUserResolver,
            final PaginationSupport paginationSupport,
            final ReservationListSupport reservationListSupport,
            final ReservationItemSupport reservationItemSupport) {
        this.currentUserResolver = currentUserResolver;
        this.paginationSupport = paginationSupport;
        this.reservationListSupport = reservationListSupport;
        this.reservationItemSupport = reservationItemSupport;
    }

    // A14 (audit): documented decision — a single collection whose visibility is a query-param
    // filter, not a different operation per role (see openapi.yaml for the full per-branch
    // breakdown): riderId (self-or-admin) / ownerId (self-or-admin) / neither (admin-only, every
    // reservation). Not a @PreAuthorize candidate: which check applies depends on *which* query
    // params the caller sent — a single method-level precondition can't express that three-way
    // routing, so it stays imperative, delegated to ReservationResourceAccess via list support.
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
        return reservationListSupport.listForViewer(
                paging,
                riderId,
                ownerId,
                carId,
                status,
                riderStatus,
                category,
                transmission,
                powertrain,
                priceMin,
                priceMax,
                rating,
                q,
                sort,
                currentUserResolver.currentPrincipalOrNull(),
                uriInfo);
    }

    @GET
    @Path("/{id}")
    @Produces({VndMediaType.RESERVATION_SUMMARY_V1_JSON, VndMediaType.RESERVATION_V1_JSON})
    @PreAuthorize("@reservationResourceAccess.canViewReservation(#id, @currentUserResolver.currentPrincipalOrNull())")
    public Response getReservation(@P("id") @PathParam("id") final long id) {
        return reservationItemSupport.getItem(
                id, currentUserResolver.currentPrincipalOrNull(), httpHeaders, uriInfo);
    }

    @GET
    @Path("/{id}/counterparty")
    @Produces(VndMediaType.COUNTERPARTY_CONTACT_V1_JSON)
    @PreAuthorize("@reservationResourceAccess.canViewReservation(#id, @currentUserResolver.currentPrincipalOrNull())")
    public Response getCounterparty(@P("id") @PathParam("id") final long id) {
        return reservationItemSupport.getCounterparty(
                id, currentUserResolver.currentPrincipalOrNull(), uriInfo);
    }

    @POST
    @Consumes(VndMediaType.RESERVATION_V1_JSON)
    @Produces(VndMediaType.RESERVATION_V1_JSON)
    public Response createReservation(@Valid final ReservationCreateForm form) {
        return reservationItemSupport.create(currentUserResolver.requireUserId(), form, uriInfo);
    }

    // The base "must be a participant or admin" gate is declarative below; the per-field escalation
    // to rider/owner (carReturned, startDate) depends on which fields the caller actually sent in
    // the PATCH body, so those narrower checks stay inside the reservation service.
    @PATCH
    @Path("/{id}")
    @Consumes(VndMediaType.RESERVATION_V1_JSON)
    @Produces(VndMediaType.RESERVATION_V1_JSON)
    @PreAuthorize("@reservationResourceAccess.canViewReservation(#id, @currentUserResolver.currentPrincipalOrNull())")
    public Response patchReservation(
            @P("id") @PathParam("id") final long id,
            @Valid final ReservationPatchForm form) {
        return reservationItemSupport.patch(id, form, currentUserResolver.requirePrincipal(), uriInfo);
    }
}
