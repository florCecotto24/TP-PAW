package ar.edu.itba.paw.webapp.support;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.springframework.stereotype.Component;

import ar.edu.itba.paw.exception.car.CarNotFoundException;
import ar.edu.itba.paw.models.domain.car.Car;
import ar.edu.itba.paw.services.car.CarService;
import ar.edu.itba.paw.webapp.api.common.VndMediaType;
import ar.edu.itba.paw.webapp.dto.rest.CarDto;
import ar.edu.itba.paw.webapp.dto.rest.CarSummaryDto;
import ar.edu.itba.paw.webapp.security.auth.userdetails.RydenUserDetails;

/**
 * Item-level HTTP orchestration for cars ({@code GET /cars/{id}}, {@code DELETE /cars/{id}}).
 * ACL predicates stay on {@link CarResourceAccess}; no domain rules live here.
 */
@Component
public final class CarItemSupport {

    private final CarService carService;
    private final CarResourceAccess carResourceAccess;
    private final CarRepresentationSupport carRepresentationSupport;

    public CarItemSupport(
            final CarService carService,
            final CarResourceAccess carResourceAccess,
            final CarRepresentationSupport carRepresentationSupport) {
        this.carService = carService;
        this.carResourceAccess = carResourceAccess;
        this.carRepresentationSupport = carRepresentationSupport;
    }

    public Response get(
            final long carId,
            final RydenUserDetails viewer,
            final HttpHeaders httpHeaders,
            final Request request,
            final UriInfo uriInfo) {
        final Car car = carResourceAccess.requireViewableCar(carId, viewer);
        if (carRepresentationSupport.acceptsCarSummary(httpHeaders)) {
            return ConditionalJsonResponses.okOrNotModified(
                    request,
                    CarRepresentationVersions.etagValue(car, CarRepresentationVersions.SUMMARY),
                    VndMediaType.CAR_SUMMARY_V1_JSON,
                    () -> CarSummaryDto.from(car, uriInfo));
        }
        // Plate is a sensitive identifier: only expose it to the owner/admin, never on the public detail.
        final boolean includePlate = carResourceAccess.isOwnerOrAdmin(car, viewer);
        return ConditionalJsonResponses.okOrNotModified(
                request,
                CarRepresentationVersions.etagValue(car, CarRepresentationVersions.DETAIL),
                VndMediaType.CAR_V1_JSON,
                () -> CarDto.from(car, uriInfo, includePlate));
    }

    public Response deactivate(final long carId) {
        final Car car = carService.getCarById(carId)
                .orElseThrow(() -> new CarNotFoundException(carId));
        if (!carService.deactivateCar(car.getOwnerId(), carId)) {
            throw new CarNotFoundException(carId);
        }
        return Response.noContent().build();
    }
}
