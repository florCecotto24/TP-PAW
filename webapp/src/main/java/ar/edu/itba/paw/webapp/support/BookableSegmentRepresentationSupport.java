package ar.edu.itba.paw.webapp.support;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.springframework.stereotype.Component;

import ar.edu.itba.paw.models.dto.car.BookableSegmentProjection;
import ar.edu.itba.paw.services.car.CarAvailabilityService;
import ar.edu.itba.paw.webapp.dto.rest.BookableSegmentDto;
import ar.edu.itba.paw.webapp.security.auth.userdetails.RydenUserDetails;

/** REST representation for {@code GET /cars/{id}/bookable-segments}. */
@Component
public final class BookableSegmentRepresentationSupport {

    private final CarAvailabilityService carAvailabilityService;
    private final CarResourceAccess carResourceAccess;

    public BookableSegmentRepresentationSupport(
            final CarAvailabilityService carAvailabilityService,
            final CarResourceAccess carResourceAccess) {
        this.carAvailabilityService = carAvailabilityService;
        this.carResourceAccess = carResourceAccess;
    }

    public Response listBookableSegments(
            final long carId,
            final RydenUserDetails viewer,
            final UriInfo uriInfo) {
        carResourceAccess.requireViewableCar(carId, viewer);
        final List<BookableSegmentProjection> segments =
                carAvailabilityService.getBookableSegmentsForRiderDatePickerByCar(carId, Instant.now());
        final List<BookableSegmentDto> dtos = segments.stream()
                .map(segment -> BookableSegmentDto.from(segment, uriInfo))
                .collect(Collectors.toList());
        if (dtos.isEmpty()) {
            return Response.noContent().build();
        }
        return Response.ok(new GenericEntity<List<BookableSegmentDto>>(dtos) {})
                .header("X-Total-Count", dtos.size())
                .build();
    }
}
