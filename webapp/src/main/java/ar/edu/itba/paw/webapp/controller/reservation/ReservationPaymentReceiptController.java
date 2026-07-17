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

import ar.edu.itba.paw.webapp.support.ReservationReceiptHttpSupport;

/** Rider payment proof ({@code /reservations/{id}/payment-receipt}). HTTP routing only. */
@Path("/reservations/{id}/payment-receipt")
@Component
public class ReservationPaymentReceiptController {

    private final ReservationReceiptHttpSupport reservationReceiptHttpSupport;

    @Context
    private HttpHeaders httpHeaders;

    @Autowired
    public ReservationPaymentReceiptController(
            final ReservationReceiptHttpSupport reservationReceiptHttpSupport) {
        this.reservationReceiptHttpSupport = reservationReceiptHttpSupport;
    }

    @GET
    @PreAuthorize(
            "@reservationResourceAccess.canViewReservation(#id, @currentUserResolver.currentPrincipalOrNull())")
    public Response download(@P("id") @PathParam("id") final long reservationId) {
        return reservationReceiptHttpSupport.downloadPayment(reservationId);
    }

    @PUT
    @Consumes({MediaType.APPLICATION_OCTET_STREAM, MediaType.WILDCARD})
    @PreAuthorize("@reservationResourceAccess.isRider(#id, @currentUserResolver.currentPrincipalOrNull())")
    public Response upload(@P("id") @PathParam("id") final long reservationId, final InputStream body)
            throws IOException {
        return reservationReceiptHttpSupport.uploadPayment(reservationId, body, httpHeaders);
    }
}
