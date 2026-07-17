package ar.edu.itba.paw.webapp.controller.reservation;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.NotFoundException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.method.P;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

import ar.edu.itba.paw.models.dto.file.BinaryContent;
import ar.edu.itba.paw.services.reservation.ReservationService;
import ar.edu.itba.paw.webapp.security.auth.userdetails.RydenUserDetails;
import ar.edu.itba.paw.webapp.support.BinaryPayloadSupport;
import ar.edu.itba.paw.webapp.support.CacheableBinaryResponses;
import ar.edu.itba.paw.webapp.support.CurrentUserResolver;
import ar.edu.itba.paw.webapp.support.ReservationResourceAccess;

/**
 * Rider payment proof ({@code /reservations/{id}/payment-receipt}).
 */
@Path("/reservations/{id}/payment-receipt")
@Component
public class ReservationPaymentReceiptController {

    private final ReservationService reservationService;
    private final CurrentUserResolver currentUserResolver;
    private final BinaryPayloadSupport binaryPayloadSupport;
    private final ReservationResourceAccess reservationResourceAccess;

    @Context
    private HttpHeaders httpHeaders;

    @Autowired
    public ReservationPaymentReceiptController(
            final ReservationService reservationService,
            final CurrentUserResolver currentUserResolver,
            final BinaryPayloadSupport binaryPayloadSupport,
            final ReservationResourceAccess reservationResourceAccess) {
        this.reservationService = reservationService;
        this.currentUserResolver = currentUserResolver;
        this.binaryPayloadSupport = binaryPayloadSupport;
        this.reservationResourceAccess = reservationResourceAccess;
    }

    @GET
    @PreAuthorize(
            "@reservationResourceAccess.canViewReservation(#id, @currentUserResolver.currentPrincipalOrNull())")
    public Response download(@P("id") @PathParam("id") final long reservationId) {
        final Optional<BinaryContent> content;
        if (reservationResourceAccess.isAdmin()) {
            content = reservationService.findPaymentReceiptContentForAdmin(reservationId);
        } else {
            final RydenUserDetails viewer = currentUserResolver.requirePrincipal();
            content = reservationService.findPaymentReceiptContentForParticipant(
                    viewer.getUserId(), reservationId);
        }
        return content.map(this::binaryResponse)
                .orElseThrow(NotFoundException::new);
    }

    @PUT
    @Consumes({MediaType.APPLICATION_OCTET_STREAM, MediaType.WILDCARD})
    @PreAuthorize("@reservationResourceAccess.isRider(#id, @currentUserResolver.currentPrincipalOrNull())")
    public Response upload(@P("id") @PathParam("id") final long reservationId, final InputStream body)
            throws IOException {
        final RydenUserDetails viewer = currentUserResolver.requirePrincipal();
        final byte[] bytes = binaryPayloadSupport.readValidatedBody(body);
        final String contentType = httpHeaders.getMediaType() != null
                ? httpHeaders.getMediaType().toString()
                : MediaType.APPLICATION_OCTET_STREAM;
        reservationService.attachPaymentReceipt(
                viewer.getUserId(), reservationId, "payment-receipt", contentType, bytes);
        return Response.noContent().build();
    }

    private Response binaryResponse(final BinaryContent content) {
        return CacheableBinaryResponses.sensitive(content, content.getFileName());
    }
}
