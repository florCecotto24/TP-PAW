package ar.edu.itba.paw.webapp.controller.reservation;

import ar.edu.itba.paw.models.dto.file.BinaryContent;
import ar.edu.itba.paw.services.reservation.ReservationMessageService;
import ar.edu.itba.paw.webapp.security.auth.userdetails.RydenUserDetails;
import ar.edu.itba.paw.webapp.support.CacheableBinaryResponses;
import ar.edu.itba.paw.webapp.support.CurrentUserResolver;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.method.P;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * Chat attachment bytes ({@code /reservations/{id}/messages/{messageId}/attachment}).
 *
 * The participant/admin gate is declarative ({@code @PreAuthorize}, backed by the
 * {@code reservationResourceAccess} bean referenced by name), so it isn't injected as a field.
 */
@Path("/reservations/{id}/messages/{messageId}/attachment")
@Component
public class ReservationMessageAttachmentController {

    private final ReservationMessageService reservationMessageService;
    private final CurrentUserResolver currentUserResolver;

    @Autowired
    public ReservationMessageAttachmentController(
            final ReservationMessageService reservationMessageService,
            final CurrentUserResolver currentUserResolver) {
        this.reservationMessageService = reservationMessageService;
        this.currentUserResolver = currentUserResolver;
    }

    @GET
    @PreAuthorize(
            "@reservationResourceAccess.canViewReservation(#id, @currentUserResolver.currentPrincipalOrNull())")
    public Response download(
            @P("id") @PathParam("id") final long reservationId,
            @PathParam("messageId") final long messageId) {
        final RydenUserDetails viewer = currentUserResolver.requirePrincipal();
        return reservationMessageService.findMessageAttachmentContentForParticipant(
                        viewer.getUserId(), reservationId, messageId)
                .map(this::binaryResponse)
                .orElseGet(() -> Response.status(Response.Status.NOT_FOUND).build());
    }

    private Response binaryResponse(final BinaryContent content) {
        // Chat attachments are participant-private uploads: never cache, never sniff, always download.
        // (The SPA previews images via an authenticated fetch + blob URL, so attachment disposition here
        //  does not break inline preview.)
        return CacheableBinaryResponses.sensitive(content, content.getFileName());
    }
}
