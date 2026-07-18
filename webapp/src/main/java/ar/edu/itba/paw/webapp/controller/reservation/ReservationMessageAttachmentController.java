package ar.edu.itba.paw.webapp.controller.reservation;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.method.P;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

import ar.edu.itba.paw.webapp.support.ReservationMessageHttpSupport;

/**
 * Chat attachment bytes ({@code /reservations/{id}/messages/{messageId}/attachment}).
 * HTTP routing only.
 */
@Path("/reservations/{id}/messages/{messageId}/attachment")
@Component
public class ReservationMessageAttachmentController {

    private final ReservationMessageHttpSupport reservationMessageHttpSupport;

    @Autowired
    public ReservationMessageAttachmentController(
            final ReservationMessageHttpSupport reservationMessageHttpSupport) {
        this.reservationMessageHttpSupport = reservationMessageHttpSupport;
    }

    @GET
    @PreAuthorize(
            "@reservationResourceAccess.canViewReservation(#id, @currentUserResolver.currentPrincipalOrNull())")
    public Response download(
            @P("id") @PathParam("id") final long reservationId,
            @PathParam("messageId") final long messageId) {
        return reservationMessageHttpSupport.downloadAttachment(reservationId, messageId);
    }
}
