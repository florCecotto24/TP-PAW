package ar.edu.itba.paw.webapp.controller.car;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.Response;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ar.edu.itba.paw.exception.car.CarNotFoundException;
import ar.edu.itba.paw.models.dto.car.BookableSegmentProjection;
import ar.edu.itba.paw.services.car.CarAvailabilityService;
import ar.edu.itba.paw.services.car.CarService;
import ar.edu.itba.paw.webapp.api.common.VndMediaType;
import ar.edu.itba.paw.webapp.dto.rest.BookableSegmentDto;

/**
 * Bookable wall-day segments for the car-detail reservation picker
 * ({@code GET /cars/{id}/bookable-segments}).
 */
@Path("/cars/{id}/bookable-segments")
@Component
public final class CarBookableSegmentController {

    private final CarService carService;
    private final CarAvailabilityService carAvailabilityService;

    @Autowired
    public CarBookableSegmentController(
            final CarService carService,
            final CarAvailabilityService carAvailabilityService) {
        this.carService = carService;
        this.carAvailabilityService = carAvailabilityService;
    }

    @GET
    @Produces(VndMediaType.BOOKABLE_SEGMENT_V1_JSON)
    public Response listBookableSegments(@PathParam("id") final long carId) {
        carService.getCarById(carId)
                .orElseThrow(() -> new CarNotFoundException(carId));
        final List<BookableSegmentProjection> segments =
                carAvailabilityService.getBookableSegmentsForRiderDatePickerByCar(carId, Instant.now());
        if (segments.isEmpty()) {
            return Response.noContent().build();
        }
        final List<BookableSegmentDto> dtos = segments.stream()
                .map(BookableSegmentDto::from)
                .collect(Collectors.toList());
        return Response.ok(new GenericEntity<List<BookableSegmentDto>>(dtos) {})
                .header("X-Total-Count", dtos.size())
                .build();
    }
}
