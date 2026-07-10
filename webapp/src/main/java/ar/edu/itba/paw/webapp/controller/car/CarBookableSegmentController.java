package ar.edu.itba.paw.webapp.controller.car;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ar.edu.itba.paw.webapp.api.common.VndMediaType;
import ar.edu.itba.paw.webapp.support.BookableSegmentRepresentationSupport;
import ar.edu.itba.paw.webapp.support.CurrentUserResolver;

/**
 * Bookable wall-day segments for the car-detail reservation picker
 * ({@code GET /cars/{id}/bookable-segments}).
 */
@Path("/cars/{id}/bookable-segments")
@Component
public final class CarBookableSegmentController {

    private final BookableSegmentRepresentationSupport bookableSegmentRepresentationSupport;
    private final CurrentUserResolver currentUserResolver;

    @Autowired
    public CarBookableSegmentController(
            final BookableSegmentRepresentationSupport bookableSegmentRepresentationSupport,
            final CurrentUserResolver currentUserResolver) {
        this.bookableSegmentRepresentationSupport = bookableSegmentRepresentationSupport;
        this.currentUserResolver = currentUserResolver;
    }

    @GET
    @Produces(VndMediaType.BOOKABLE_SEGMENT_V1_JSON)
    public Response listBookableSegments(@PathParam("id") final long carId) {
        return bookableSegmentRepresentationSupport.listBookableSegments(
                carId, currentUserResolver.currentPrincipalOrNull());
    }
}
