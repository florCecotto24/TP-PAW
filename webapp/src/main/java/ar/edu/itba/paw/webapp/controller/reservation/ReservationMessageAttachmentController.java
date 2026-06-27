package ar.edu.itba.paw.webapp.controller.reservation;

import ar.edu.itba.paw.models.dto.file.BinaryContent;
import ar.edu.itba.paw.services.reservation.ReservationMessageService;
import ar.edu.itba.paw.webapp.security.auth.userdetails.RydenUserDetails;
import ar.edu.itba.paw.webapp.support.CurrentUserResolver;
import ar.edu.itba.paw.webapp.support.ReservationResourceAccess;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/** Chat attachment bytes ({@code /reservations/{id}/messages/{messageId}/attachment}). */
@Path("/reservations/{id}/messages/{messageId}/attachment")
@Component
public final class ReservationMessageAttachmentController {

    private final ReservationMessageService reservationMessageService;
    private final CurrentUserResolver currentUserResolver;
    private final ReservationResourceAccess reservationResourceAccess;

    @Autowired
    public ReservationMessageAttachmentController(
            final ReservationMessageService reservationMessageService,
            final CurrentUserResolver currentUserResolver,
            final ReservationResourceAccess reservationResourceAccess) {
        this.reservationMessageService = reservationMessageService;
        this.currentUserResolver = currentUserResolver;
        this.reservationResourceAccess = reservationResourceAccess;
    }

    @GET
    public Response download(
            @PathParam("id") final long reservationId,
            @PathParam("messageId") final long messageId) {
        final RydenUserDetails viewer = currentUserResolver.requirePrincipal();
        reservationResourceAccess.requireViewReservation(reservationId, viewer);
        return reservationMessageService.findMessageAttachmentContentForParticipant(
                        viewer.getUserId(), reservationId, messageId)
                .map(this::binaryResponse)
                .orElseGet(() -> Response.status(Response.Status.NOT_FOUND).build());
    }

    private Response binaryResponse(final BinaryContent content) {
        return Response.ok(content.getBytes())
                .type(content.getContentType())
                .build();
    }
}
