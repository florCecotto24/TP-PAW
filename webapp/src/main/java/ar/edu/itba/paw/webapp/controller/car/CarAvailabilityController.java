package ar.edu.itba.paw.webapp.controller.car;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PATCH;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.method.P;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

import ar.edu.itba.paw.webapp.api.common.VndMediaType;
import ar.edu.itba.paw.webapp.form.car.AvailabilityCreateForm;
import ar.edu.itba.paw.webapp.support.CarAvailabilityHttpSupport;
import ar.edu.itba.paw.webapp.support.CarResourceAccess;
import ar.edu.itba.paw.webapp.support.CurrentUserResolver;
import ar.edu.itba.paw.webapp.support.PaginationSupport;
import ar.edu.itba.paw.webapp.validation.constraint.common.ValidYearMonth;

/** Car availability sub-resource ({@code /cars/{id}/availabilities}). HTTP routing only. */
@Path("/cars/{id}/availabilities")
@Component
public class CarAvailabilityController {

    private final CarAvailabilityHttpSupport carAvailabilityHttpSupport;
    private final CurrentUserResolver currentUserResolver;
    private final CarResourceAccess carResourceAccess;
    private final PaginationSupport paginationSupport;

    @Context
    private UriInfo uriInfo;

    @Autowired
    public CarAvailabilityController(
            final CarAvailabilityHttpSupport carAvailabilityHttpSupport,
            final CurrentUserResolver currentUserResolver,
            final CarResourceAccess carResourceAccess,
            final PaginationSupport paginationSupport) {
        this.carAvailabilityHttpSupport = carAvailabilityHttpSupport;
        this.currentUserResolver = currentUserResolver;
        this.carResourceAccess = carResourceAccess;
        this.paginationSupport = paginationSupport;
    }

    @GET
    @Produces(VndMediaType.AVAILABILITY_V1_JSON)
    public Response listAvailabilities(
            @PathParam("id") final long carId,
            @QueryParam("month") @ValidYearMonth final String month,
            @QueryParam("page") @DefaultValue("1") final int page,
            @QueryParam("pageSize") final Integer pageSizeParam) {
        carResourceAccess.requireViewableCar(carId, currentUserResolver.currentPrincipalOrNull());
        return carAvailabilityHttpSupport.list(
                carId, month, paginationSupport.forAvailabilities(page, pageSizeParam), uriInfo);
    }

    @POST
    @Consumes(VndMediaType.AVAILABILITY_V1_JSON)
    @Produces(VndMediaType.AVAILABILITY_V1_JSON)
    @PreAuthorize("@carResourceAccess.isOwnerById(#id, @currentUserResolver.currentPrincipalOrNull())")
    public Response createAvailability(
            @P("id") @PathParam("id") final long carId,
            final AvailabilityCreateForm form) {
        return carAvailabilityHttpSupport.create(
                carId, form, currentUserResolver.currentPrincipalOrNull(), uriInfo);
    }

    @GET
    @Path("/{availabilityId}")
    @Produces(VndMediaType.AVAILABILITY_V1_JSON)
    public Response getAvailability(
            @PathParam("id") final long carId,
            @PathParam("availabilityId") final long availabilityId) {
        carResourceAccess.requireViewableCar(carId, currentUserResolver.currentPrincipalOrNull());
        return carAvailabilityHttpSupport.get(carId, availabilityId, uriInfo);
    }

    @PATCH
    @Path("/{availabilityId}")
    @Consumes(VndMediaType.AVAILABILITY_V1_JSON)
    @Produces(VndMediaType.AVAILABILITY_V1_JSON)
    @PreAuthorize("@carResourceAccess.isOwnerById(#id, @currentUserResolver.currentPrincipalOrNull())")
    public Response patchAvailability(
            @P("id") @PathParam("id") final long carId,
            @PathParam("availabilityId") final long availabilityId,
            final AvailabilityCreateForm form) {
        return carAvailabilityHttpSupport.patch(
                carId, availabilityId, form, currentUserResolver.currentPrincipalOrNull(), uriInfo);
    }

    @DELETE
    @PreAuthorize("@carResourceAccess.isOwnerById(#id, @currentUserResolver.currentPrincipalOrNull())")
    public Response deleteAvailabilityRange(
            @P("id") @PathParam("id") final long carId,
            @QueryParam("from") final String from,
            @QueryParam("until") final String until) {
        return carAvailabilityHttpSupport.deleteRange(carId, from, until);
    }

    @DELETE
    @Path("/{availabilityId}")
    @PreAuthorize("@carResourceAccess.isOwnerById(#id, @currentUserResolver.currentPrincipalOrNull())")
    public Response deleteAvailability(
            @P("id") @PathParam("id") final long carId,
            @PathParam("availabilityId") final long availabilityId) {
        return carAvailabilityHttpSupport.deleteOne(carId, availabilityId);
    }
}
