package ar.edu.itba.paw.webapp.support;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.springframework.stereotype.Component;

import ar.edu.itba.paw.exception.car.CarNotFoundException;
import ar.edu.itba.paw.models.domain.car.Car;
import ar.edu.itba.paw.services.car.CarService;
import ar.edu.itba.paw.webapp.api.common.VndMediaType;
import ar.edu.itba.paw.webapp.dto.rest.CarDto;
import ar.edu.itba.paw.webapp.form.car.CarPatchForm;
import ar.edu.itba.paw.webapp.security.auth.userdetails.RydenUserDetails;

/**
 * Field-scoped PATCH orchestration for {@code PATCH /cars/{id}}.
 * Authorization predicates stay on {@link CarResourceAccess}; this class only sequences HTTP fields
 * into service calls (no domain rules).
 */
@Component
public final class CarPatchSupport {

    private final CarService carService;
    private final CarResourceAccess carResourceAccess;

    public CarPatchSupport(final CarService carService, final CarResourceAccess carResourceAccess) {
        this.carService = carService;
        this.carResourceAccess = carResourceAccess;
    }

    public Response apply(
            final long carId,
            final CarPatchForm patch,
            final RydenUserDetails viewer,
            final UriInfo uriInfo) {
        final Car car = carService.getCarById(carId)
                .orElseThrow(() -> new CarNotFoundException(carId));

        // Authorize description/min-days before status so a mixed body cannot partially commit.
        // @PreAuthorize already gates owner/admin; keep the explicit require for field scope clarity.
        if (patch.getDescription() != null || patch.getMinimumRentalDays() != null) {
            carResourceAccess.requireOwnerOrAdmin(car, viewer);
        }
        if (patch.getStatus() != null) {
            final Car.Status target = CarRestEnums.parseStatus(patch.getStatus());
            carService.applyStatusTransition(car.getId(), target, viewer.getUserId());
        }
        if (patch.getDescription() != null) {
            carService.updateDescription(car.getOwnerId(), carId, patch.getDescription());
        }
        if (patch.getMinimumRentalDays() != null) {
            carService.updateMinimumRentalDays(carId, patch.getMinimumRentalDays());
        }

        final Car updated = carService.getCarById(carId)
                .orElseThrow(() -> new CarNotFoundException(carId));
        return Response.ok(CarDto.fromPrivate(updated, uriInfo))
                .type(VndMediaType.CAR_PRIVATE_V1_JSON)
                .build();
    }
}
