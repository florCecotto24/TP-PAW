package ar.edu.itba.paw.webapp.controller.reservation;

import java.util.Optional;

import ar.edu.itba.paw.models.dto.file.BinaryContent;
import ar.edu.itba.paw.services.reservation.ReservationMessageService;
import ar.edu.itba.paw.webapp.security.auth.userdetails.RydenUserDetails;
import ar.edu.itba.paw.webapp.support.CacheableBinaryResponses;
import ar.edu.itba.paw.webapp.support.CurrentUserResolver;
import ar.edu.itba.paw.webapp.support.ReservationResourceAccess;

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
 */
@Path("/reservations/{id}/messages/{messageId}/attachment")
@Component
public class ReservationMessageAttachmentController {

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
    @PreAuthorize(
            "@reservationResourceAccess.canViewReservation(#id, @currentUserResolver.currentPrincipalOrNull())")
    public Response download(
            @P("id") @PathParam("id") final long reservationId,
            @PathParam("messageId") final long messageId) {
        final Optional<BinaryContent> content;
        if (reservationResourceAccess.isAdmin()) {
            content = reservationMessageService.findMessageAttachmentContentForAdmin(
                    reservationId, messageId);
        } else {
            final RydenUserDetails viewer = currentUserResolver.requirePrincipal();
            content = reservationMessageService.findMessageAttachmentContentForParticipant(
                    viewer.getUserId(), reservationId, messageId);
        }
        return content.map(this::binaryResponse)
                .orElseGet(() -> Response.status(Response.Status.NOT_FOUND).build());
    }

    private Response binaryResponse(final BinaryContent content) {
        return CacheableBinaryResponses.sensitive(content, content.getFileName());
    }
}
