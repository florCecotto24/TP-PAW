package ar.edu.itba.paw.webapp.controller.reservation;

import java.io.IOException;
import java.io.InputStream;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

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

/**
 * Rider payment proof ({@code /reservations/{id}/payment-receipt}).
 *
 * <p>The participant/rider gates are declarative ({@code @PreAuthorize}, backed by the
 * {@code reservationResourceAccess} bean referenced by name), so it isn't injected as a field.
 */
@Path("/reservations/{id}/payment-receipt")
@Component
public class ReservationPaymentReceiptController {

    private final ReservationService reservationService;
    private final CurrentUserResolver currentUserResolver;
    private final BinaryPayloadSupport binaryPayloadSupport;

    @Context
    private HttpHeaders httpHeaders;

    @Autowired
    public ReservationPaymentReceiptController(
            final ReservationService reservationService,
            final CurrentUserResolver currentUserResolver,
            final BinaryPayloadSupport binaryPayloadSupport) {
        this.reservationService = reservationService;
        this.currentUserResolver = currentUserResolver;
        this.binaryPayloadSupport = binaryPayloadSupport;
    }

    @GET
    @PreAuthorize(
            "@reservationResourceAccess.canViewReservation(#id, @currentUserResolver.currentPrincipalOrNull())")
    public Response download(@P("id") @PathParam("id") final long reservationId) {
        final RydenUserDetails viewer = currentUserResolver.requirePrincipal();
        return reservationService.findPaymentReceiptContentForParticipant(viewer.getUserId(), reservationId)
                .map(this::binaryResponse)
                .orElseGet(() -> Response.status(Response.Status.NOT_FOUND).build());
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
        // Payment/refund receipts are sensitive: never cache, never sniff, always download.
        return CacheableBinaryResponses.sensitive(content, content.getFileName());
    }
}
