package ar.edu.itba.paw.webapp.controller.car;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.stream.Collectors;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
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

import ar.edu.itba.paw.exception.car.CarNotFoundException;
import ar.edu.itba.paw.models.domain.car.AvailabilityPeriod;
import ar.edu.itba.paw.models.domain.car.Car;
import ar.edu.itba.paw.models.domain.car.CarAvailability;
import ar.edu.itba.paw.models.dto.Page;
import ar.edu.itba.paw.models.dto.car.AvailabilityCreateInput;
import ar.edu.itba.paw.services.car.CarAvailabilityService;
import ar.edu.itba.paw.services.car.CarService;
import ar.edu.itba.paw.services.location.LocationService;
import ar.edu.itba.paw.webapp.api.common.PaginationLinks;
import ar.edu.itba.paw.webapp.api.common.VndMediaType;
import ar.edu.itba.paw.webapp.config.properties.AppPaginationProperties;
import ar.edu.itba.paw.webapp.dto.rest.AvailabilityDto;
import ar.edu.itba.paw.webapp.form.car.AvailabilityCreateForm;
import ar.edu.itba.paw.webapp.security.auth.userdetails.RydenUserDetails;
import ar.edu.itba.paw.webapp.support.CarResourceAccess;
import ar.edu.itba.paw.webapp.support.CurrentUserResolver;
import ar.edu.itba.paw.webapp.support.FormValidationSupport;
import ar.edu.itba.paw.webapp.validation.ValidationGroups;

/** Car availability sub-resource ({@code /cars/{id}/availabilities}). */
@Path("/cars/{id}/availabilities")
@Component
public final class CarAvailabilityController {

    private final CarService carService;
    private final CarAvailabilityService carAvailabilityService;
    private final FormValidationSupport formValidationSupport;
    private final CurrentUserResolver currentUserResolver;
    private final CarResourceAccess carResourceAccess;
    private final LocationService locationService;
    private final AppPaginationProperties paginationProperties;

    @Context
    private UriInfo uriInfo;

    @Autowired
    public CarAvailabilityController(
            final CarService carService,
            final CarAvailabilityService carAvailabilityService,
            final FormValidationSupport formValidationSupport,
            final CurrentUserResolver currentUserResolver,
            final CarResourceAccess carResourceAccess,
            final LocationService locationService,
            final AppPaginationProperties paginationProperties) {
        this.carService = carService;
        this.carAvailabilityService = carAvailabilityService;
        this.formValidationSupport = formValidationSupport;
        this.currentUserResolver = currentUserResolver;
        this.carResourceAccess = carResourceAccess;
        this.locationService = locationService;
        this.paginationProperties = paginationProperties;
    }

    @GET
    @Produces(VndMediaType.AVAILABILITY_V1_JSON)
    public Response listAvailabilities(
            @PathParam("id") final long carId,
            @QueryParam("month") final String month,
            @QueryParam("page") @DefaultValue("1") final int page,
            @QueryParam("pageSize") final Integer pageSizeParam) {
        requireCarExists(carId);
        final int safePage = Math.max(1, page);
        final int pageSize = pageSizeParam != null && pageSizeParam > 0
                ? pageSizeParam
                : paginationProperties.getDefaultPageSize();
        final int zeroBasedPage = safePage - 1;

        final List<CarAvailability> rows;
        final long total;
        if (month != null && !month.isBlank()) {
            final YearMonth yearMonth = parseMonth(month);
            final Page<CarAvailability> paged = carAvailabilityService.findEffectiveOfferedByCarAndMonth(
                    carId, yearMonth, zeroBasedPage, pageSize);
            rows = paged.getContent();
            total = paged.getTotalItems();
        } else {
            rows = carAvailabilityService.findEffectiveOfferedByCar(carId);
            total = rows.size();
        }

        if (total == 0L) {
            return Response.noContent().build();
        }
        final List<AvailabilityDto> dtos = rows.stream()
                .map(row -> AvailabilityDto.from(row, uriInfo))
                .collect(Collectors.toList());
        final Response.ResponseBuilder builder =
                Response.ok(new GenericEntity<List<AvailabilityDto>>(dtos) {})
                        .header("X-Total-Count", total);
        PaginationLinks.add(builder, uriInfo, safePage, pageSize, (int) total);
        return builder.build();
    }

    @POST
    @Consumes(VndMediaType.AVAILABILITY_V1_JSON)
    @Produces(VndMediaType.AVAILABILITY_V1_JSON)
    public Response createAvailability(
            @PathParam("id") final long carId,
            final AvailabilityCreateForm form) {
        final Car car = requireCarExists(carId);
        final RydenUserDetails viewer = currentUserResolver.currentPrincipalOrNull();
        carResourceAccess.requireOwner(car, viewer);
        validateAvailabilityForm(form);

        final List<CarAvailability> created = carAvailabilityService.createListing(
                viewer.getUserId(), carId, toInput(form), Instant.now());
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
        requireCarExists(carId);
        final CarAvailability availability = carAvailabilityService.findByIdForCar(carId, availabilityId)
                .orElseThrow(() -> new CarNotFoundException(carId));
        return Response.ok(AvailabilityDto.from(availability, uriInfo)).build();
    }

    @PUT
    @Path("/{availabilityId}")
    @Consumes(VndMediaType.AVAILABILITY_V1_JSON)
    @Produces(VndMediaType.AVAILABILITY_V1_JSON)
    public Response updateAvailability(
            @PathParam("id") final long carId,
            @PathParam("availabilityId") final long availabilityId,
            final AvailabilityCreateForm form) {
        final Car car = requireCarExists(carId);
        final RydenUserDetails viewer = currentUserResolver.currentPrincipalOrNull();
        carResourceAccess.requireOwner(car, viewer);
        validateAvailabilityForm(form);

        final CarAvailability updated = carAvailabilityService.editAvailability(
                viewer.getUserId(), carId, availabilityId, toInput(form), Instant.now());
        return Response.ok(AvailabilityDto.from(updated, uriInfo)).build();
    }

    @DELETE
    @Path("/{availabilityId}")
    public Response deleteAvailability(
            @PathParam("id") final long carId,
            @PathParam("availabilityId") final long availabilityId) {
        final Car car = requireCarExists(carId);
        final RydenUserDetails viewer = currentUserResolver.currentPrincipalOrNull();
        carResourceAccess.requireOwner(car, viewer);
        carAvailabilityService.applyOwnerWithdrawByCar(carId, availabilityId);
        return Response.noContent().build();
    }

    private Car requireCarExists(final long carId) {
        return carService.getCarById(carId)
                .orElseThrow(() -> new CarNotFoundException(carId));
    }

    private void validateAvailabilityForm(final AvailabilityCreateForm form) {
        formValidationSupport.validate(form, ValidationGroups.OnCreateListing.class);
        if (form.getNeighborhoodId() != null
                && locationService.findNeighborhoodById(form.getNeighborhoodId()).isEmpty()) {
            throw new javax.ws.rs.BadRequestException("Invalid neighborhoodId.");
        }
        if (form.getStartDate() != null && form.getEndDate() != null
                && form.getEndDate().isBefore(form.getStartDate())) {
            throw new javax.ws.rs.BadRequestException("endDate must be on or after startDate.");
        }
    }

    private static AvailabilityCreateInput toInput(final AvailabilityCreateForm form) {
        return new AvailabilityCreateInput(
                form.getDayPrice(),
                form.getStartPointStreet(),
                form.getStartPointNumber(),
                form.getNeighborhoodId(),
                form.getCheckInTime(),
                form.getCheckOutTime(),
                List.of(new AvailabilityPeriod(form.getStartDate(), form.getEndDate())),
                List.of(form.getDayPrice()),
                1);
    }

    private static YearMonth parseMonth(final String month) {
        try {
            return YearMonth.parse(month);
        } catch (final DateTimeParseException ex) {
            throw new javax.ws.rs.BadRequestException("Invalid month: " + month);
        }
    }
}
