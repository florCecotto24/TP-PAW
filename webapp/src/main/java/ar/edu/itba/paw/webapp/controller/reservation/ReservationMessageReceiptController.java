package ar.edu.itba.paw.webapp.controller.reservation;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.method.P;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

import ar.edu.itba.paw.webapp.support.CurrentUserResolver;
import ar.edu.itba.paw.webapp.support.ReservationMessageHttpSupport;

/**
 * Read receipts for reservation chat ({@code POST /reservations/{id}/message-receipts}).
 * HTTP routing only; mark-seen orchestration lives in {@link ReservationMessageHttpSupport}.
 */
@Path("/reservations/{id}/message-receipts")
@Component
public class ReservationMessageReceiptController {

    private final CurrentUserResolver currentUserResolver;
    private final ReservationMessageHttpSupport reservationMessageHttpSupport;

    @Autowired
    public ReservationMessageReceiptController(
            final CurrentUserResolver currentUserResolver,
            final ReservationMessageHttpSupport reservationMessageHttpSupport) {
        this.currentUserResolver = currentUserResolver;
        this.reservationMessageHttpSupport = reservationMessageHttpSupport;
    }

    @POST
    @PreAuthorize(
            "@reservationResourceAccess.canViewReservation(#id, @currentUserResolver.currentPrincipalOrNull())")
    public Response createReceipt(@P("id") @PathParam("id") final long reservationId) {
        return reservationMessageHttpSupport.createReceipt(
                reservationId, currentUserResolver.requirePrincipal());
    }
}
