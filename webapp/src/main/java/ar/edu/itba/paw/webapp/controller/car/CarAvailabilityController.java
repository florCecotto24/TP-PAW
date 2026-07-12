package ar.edu.itba.paw.webapp.controller.car;

import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.stream.Collectors;

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
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.method.P;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

import ar.edu.itba.paw.exception.car.CarNotFoundException;
import ar.edu.itba.paw.models.domain.car.AvailabilityPeriod;
import ar.edu.itba.paw.models.domain.car.Car;
import ar.edu.itba.paw.models.domain.car.CarAvailability;
import ar.edu.itba.paw.models.dto.Page;
import ar.edu.itba.paw.models.dto.car.AvailabilityCreateInput;
import ar.edu.itba.paw.models.dto.car.BookableSegmentProjection;
import ar.edu.itba.paw.services.car.CarAvailabilityService;
import ar.edu.itba.paw.services.car.CarService;
import ar.edu.itba.paw.webapp.api.common.PaginationLinks;
import ar.edu.itba.paw.webapp.api.common.VndMediaType;
import ar.edu.itba.paw.webapp.dto.rest.AvailabilityDto;
import ar.edu.itba.paw.webapp.form.car.AvailabilityCreateForm;
import ar.edu.itba.paw.webapp.security.auth.userdetails.RydenUserDetails;
import ar.edu.itba.paw.webapp.support.CarResourceAccess;
import ar.edu.itba.paw.webapp.support.CurrentUserResolver;
import ar.edu.itba.paw.webapp.support.FormValidationSupport;
import ar.edu.itba.paw.webapp.support.PaginationParams;
import ar.edu.itba.paw.webapp.support.PaginationSupport;
import ar.edu.itba.paw.webapp.util.RestUriUtils;
import ar.edu.itba.paw.webapp.validation.ValidationGroups;
import ar.edu.itba.paw.webapp.validation.constraint.common.ValidYearMonth;


/** Car availability sub-resource ({@code /cars/{id}/availabilities}). */
@Path("/cars/{id}/availabilities")
@Component
public class CarAvailabilityController {

    private final CarService carService;
    private final CarAvailabilityService carAvailabilityService;
    private final FormValidationSupport formValidationSupport;
    private final CurrentUserResolver currentUserResolver;
    private final CarResourceAccess carResourceAccess;
    private final PaginationSupport paginationSupport;

    @Context
    private UriInfo uriInfo;

    @Autowired
    public CarAvailabilityController(
            final CarService carService,
            final CarAvailabilityService carAvailabilityService,
            final FormValidationSupport formValidationSupport,
            final CurrentUserResolver currentUserResolver,
            final CarResourceAccess carResourceAccess,
            final PaginationSupport paginationSupport) {
        this.carService = carService;
        this.carAvailabilityService = carAvailabilityService;
        this.formValidationSupport = formValidationSupport;
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
        final PaginationParams paging = paginationSupport.forAvailabilities(page, pageSizeParam);

        if (month != null && !month.isBlank()) {
            final YearMonth yearMonth = YearMonth.parse(month);
            final Page<BookableSegmentProjection> segmentPage =
                    carAvailabilityService.getEffectiveSegmentsForOwnerCalendarInRangePaginated(
                            carId,
                            yearMonth.atDay(1),
                            yearMonth.atEndOfMonth(),
                            paging.getZeroBasedPage(),
                            paging.getPageSize());
            return pagedAvailabilityDtos(
                    segmentPage.getContent().stream()
                            .map(segment -> AvailabilityDto.fromSegment(segment, carId, uriInfo))
                            .collect(Collectors.toList()),
                    paging,
                    (int) segmentPage.getTotalItems());
        }

        final Page<CarAvailability> availabilityPage = carAvailabilityService.findEffectiveOfferedByCarPaginated(
                carId, paging.getZeroBasedPage(), paging.getPageSize());
        return pagedAvailabilityDtos(
                availabilityPage.getContent().stream()
                        .map(row -> AvailabilityDto.from(row, uriInfo))
                        .collect(Collectors.toList()),
                paging,
                (int) availabilityPage.getTotalItems());
    }

    private Response pagedAvailabilityDtos(
            final List<AvailabilityDto> dtos,
            final PaginationParams paging,
            final int totalItems) {
        if (totalItems == 0L || dtos.isEmpty()) {
            return Response.noContent().build();
        }
        final Response.ResponseBuilder builder =
                Response.ok(new GenericEntity<List<AvailabilityDto>>(dtos) {})
                        .header("X-Total-Count", totalItems);
        PaginationLinks.add(builder, uriInfo, paging.getPage(), paging.getPageSize(), totalItems);
        return builder.build();
    }

    @POST
    @Consumes(VndMediaType.AVAILABILITY_V1_JSON)
    @Produces(VndMediaType.AVAILABILITY_V1_JSON)
    @PreAuthorize("@carResourceAccess.isOwnerById(#id, @currentUserResolver.currentPrincipalOrNull())")
    public Response createAvailability(
            @P("id") @PathParam("id") final long carId,
            final AvailabilityCreateForm form) {
        requireCarExists(carId);
        final RydenUserDetails viewer = currentUserResolver.currentPrincipalOrNull();
        formValidationSupport.validate(form, ValidationGroups.OnCreateListing.class);

        final List<CarAvailability> created = carAvailabilityService.createListing(
                viewer.getUserId(), carId, toInput(form), java.time.Instant.now());
        final CarAvailability availability = created.get(created.size() - 1);
        final URI location = uriInfo.getBaseUriBuilder()
                .path("cars").path(String.valueOf(carId))
                .path("availabilities").path(String.valueOf(availability.getId()))
                .build();
        return Response.created(location).entity(AvailabilityDto.from(availability, uriInfo)).build();
    }

    @GET
    @Path("/{availabilityId}")
    @Produces(VndMediaType.AVAILABILITY_V1_JSON)
    public Response getAvailability(
            @PathParam("id") final long carId,
            @PathParam("availabilityId") final long availabilityId) {
        carResourceAccess.requireViewableCar(carId, currentUserResolver.currentPrincipalOrNull());
        final CarAvailability availability = carAvailabilityService.findByIdForCar(carId, availabilityId)
                .orElseThrow(() -> new CarNotFoundException(carId));
        return Response.ok(AvailabilityDto.from(availability, uriInfo)).build();
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
        requireCarExists(carId);
        final RydenUserDetails viewer = currentUserResolver.currentPrincipalOrNull();
        formValidationSupport.validate(form, ValidationGroups.OnCreateListing.class);

        final CarAvailability updated = carAvailabilityService.editAvailability(
                viewer.getUserId(), carId, availabilityId, toInput(form), Instant.now());
        return Response.ok(AvailabilityDto.from(updated, uriInfo)).build();
    }

    @DELETE
    @Path("/range")
    @PreAuthorize("@carResourceAccess.isOwnerById(#id, @currentUserResolver.currentPrincipalOrNull())")
    public Response deleteAvailabilityRange(
            @P("id") @PathParam("id") final long carId,
            @QueryParam("from") final String from,
            @QueryParam("until") final String until) {
        requireCarExists(carId);
        carAvailabilityService.parseAndApplyWithdrawRange(carId, from, until);
        return Response.noContent().build();
    }

    @DELETE
    @Path("/{availabilityId}")
    @PreAuthorize("@carResourceAccess.isOwnerById(#id, @currentUserResolver.currentPrincipalOrNull())")
    public Response deleteAvailability(
            @P("id") @PathParam("id") final long carId,
            @PathParam("availabilityId") final long availabilityId) {
        requireCarExists(carId);
        carAvailabilityService.applyOwnerWithdrawByCar(carId, availabilityId);
        return Response.noContent().build();
    }

    private Car requireCarExists(final long carId) {
        return carService.getCarById(carId)
                .orElseThrow(() -> new CarNotFoundException(carId));
    }

    private static AvailabilityCreateInput toInput(final AvailabilityCreateForm form) {
        return new AvailabilityCreateInput(
                form.getDayPrice(),
                form.getStartPointStreet(),
                form.getStartPointNumber(),
                RestUriUtils.parseNeighborhoodId(form.getNeighborhoodUri()).orElse(null),
                form.getCheckInTime(),
                form.getCheckOutTime(),
                List.of(new AvailabilityPeriod(form.getStartDate(), form.getEndDate())),
                List.of(form.getDayPrice()),
                1);
    }
}
