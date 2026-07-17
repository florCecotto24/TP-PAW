package ar.edu.itba.paw.webapp.support;

import java.net.URI;
import java.time.Instant;
import java.time.YearMonth;
import java.util.List;
import java.util.stream.Collectors;

import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

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
import ar.edu.itba.paw.webapp.dto.rest.AvailabilityDto;
import ar.edu.itba.paw.webapp.form.car.AvailabilityCreateForm;
import ar.edu.itba.paw.webapp.security.auth.userdetails.RydenUserDetails;
import ar.edu.itba.paw.webapp.util.RestUriUtils;
import ar.edu.itba.paw.webapp.validation.ValidationGroups;

/**
 * HTTP binding for car availability sub-resource (list month vs offered, create/patch mapping).
 */
@Component
public final class CarAvailabilityHttpSupport {

    private final CarService carService;
    private final CarAvailabilityService carAvailabilityService;
    private final FormValidationSupport formValidationSupport;

    public CarAvailabilityHttpSupport(
            final CarService carService,
            final CarAvailabilityService carAvailabilityService,
            final FormValidationSupport formValidationSupport) {
        this.carService = carService;
        this.carAvailabilityService = carAvailabilityService;
        this.formValidationSupport = formValidationSupport;
    }

    public Response list(
            final long carId,
            final String month,
            final PaginationParams paging,
            final UriInfo uriInfo) {
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
                    (int) segmentPage.getTotalItems(),
                    uriInfo);
        }

        final Page<CarAvailability> availabilityPage = carAvailabilityService.findEffectiveOfferedByCarPaginated(
                carId, paging.getZeroBasedPage(), paging.getPageSize());
        return pagedAvailabilityDtos(
                availabilityPage.getContent().stream()
                        .map(row -> AvailabilityDto.from(row, uriInfo))
                        .collect(Collectors.toList()),
                paging,
                (int) availabilityPage.getTotalItems(),
                uriInfo);
    }

    public Response create(
            final long carId,
            final AvailabilityCreateForm form,
            final RydenUserDetails viewer,
            final UriInfo uriInfo) {
        requireCarExists(carId);
        formValidationSupport.validate(form, ValidationGroups.OnCreateListing.class);
        final List<CarAvailability> created = carAvailabilityService.createListing(
                viewer.getUserId(), carId, toInput(form), Instant.now());
        final CarAvailability availability = created.get(created.size() - 1);
        final URI location = uriInfo.getBaseUriBuilder()
                .path("cars").path(String.valueOf(carId))
                .path("availabilities").path(String.valueOf(availability.getId()))
                .build();
        return Response.created(location).entity(AvailabilityDto.from(availability, uriInfo)).build();
    }

    public Response get(final long carId, final long availabilityId, final UriInfo uriInfo) {
        final CarAvailability availability = carAvailabilityService.findByIdForCar(carId, availabilityId)
                .orElseThrow(() -> new CarNotFoundException(carId));
        return Response.ok(AvailabilityDto.from(availability, uriInfo)).build();
    }

    public Response patch(
            final long carId,
            final long availabilityId,
            final AvailabilityCreateForm form,
            final RydenUserDetails viewer,
            final UriInfo uriInfo) {
        requireCarExists(carId);
        formValidationSupport.validate(form, ValidationGroups.OnCreateListing.class);
        final CarAvailability updated = carAvailabilityService.editAvailability(
                viewer.getUserId(), carId, availabilityId, toInput(form), Instant.now());
        return Response.ok(AvailabilityDto.from(updated, uriInfo)).build();
    }

    public Response deleteRange(final long carId, final String from, final String until) {
        requireCarExists(carId);
        carAvailabilityService.parseAndApplyWithdrawRange(carId, from, until);
        return Response.noContent().build();
    }

    public Response deleteOne(final long carId, final long availabilityId) {
        requireCarExists(carId);
        carAvailabilityService.applyOwnerWithdrawByCar(carId, availabilityId);
        return Response.noContent().build();
    }

    private Car requireCarExists(final long carId) {
        return carService.getCarById(carId)
                .orElseThrow(() -> new CarNotFoundException(carId));
    }

    private static Response pagedAvailabilityDtos(
            final List<AvailabilityDto> dtos,
            final PaginationParams paging,
            final int totalItems,
            final UriInfo uriInfo) {
        if (totalItems == 0L || dtos.isEmpty()) {
            return Response.noContent().build();
        }
        final Response.ResponseBuilder builder =
                Response.ok(new GenericEntity<List<AvailabilityDto>>(dtos) {})
                        .header("X-Total-Count", totalItems);
        PaginationLinks.add(builder, uriInfo, paging.getPage(), paging.getPageSize(), totalItems);
        return builder.build();
    }

    private static AvailabilityCreateInput toInput(final AvailabilityCreateForm form) {
        return new AvailabilityCreateInput(
                form.getDayPrice(),
                form.getStartPointStreet(),
                form.getStartPointNumber(),
                RestUriUtils.parseNeighborhoodId(form.getNeighborhoodUri()).orElse(null),
                form.getCheckInTime(),
                form.getCheckOutTime(),
                List.of(AvailabilityPeriod.of(form.getStartDate(), form.getEndDate())),
                List.of(form.getDayPrice()),
                1);
    }
}
