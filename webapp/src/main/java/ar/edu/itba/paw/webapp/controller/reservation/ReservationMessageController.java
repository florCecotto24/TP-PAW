package ar.edu.itba.paw.webapp.controller.reservation;

import java.io.IOException;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.method.P;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

import ar.edu.itba.paw.webapp.api.common.VndMediaType;
import ar.edu.itba.paw.webapp.support.CurrentUserResolver;
import ar.edu.itba.paw.webapp.support.PaginationSupport;
import ar.edu.itba.paw.webapp.support.ReservationMessageHttpSupport;

/** Reservation chat messages ({@code /reservations/{id}/messages}). HTTP routing only. */
@Path("/reservations/{id}/messages")
@Component
public class ReservationMessageController {

    private final CurrentUserResolver currentUserResolver;
    private final PaginationSupport paginationSupport;
    private final ReservationMessageHttpSupport reservationMessageHttpSupport;

    @Context
    private UriInfo uriInfo;

    @Autowired
    public ReservationMessageController(
            final CurrentUserResolver currentUserResolver,
            final PaginationSupport paginationSupport,
            final ReservationMessageHttpSupport reservationMessageHttpSupport) {
        this.currentUserResolver = currentUserResolver;
        this.paginationSupport = paginationSupport;
        this.reservationMessageHttpSupport = reservationMessageHttpSupport;
    }

    @GET
    @Produces(VndMediaType.MESSAGE_V1_JSON)
    @PreAuthorize(
            "@reservationResourceAccess.canViewReservation(#id, @currentUserResolver.currentPrincipalOrNull())")
    public Response listMessages(
            @P("id") @PathParam("id") final long reservationId,
            @QueryParam("page") @DefaultValue("1") final int page,
            @QueryParam("pageSize") final Integer pageSizeParam,
            @QueryParam("afterId") final Long afterId) {
        return reservationMessageHttpSupport.list(
                reservationId,
                paginationSupport.forMessages(page, pageSizeParam),
                afterId,
                currentUserResolver.currentPrincipalOrNull(),
                uriInfo);
    }

    @GET
    @Path("/{messageId}")
    @Produces(VndMediaType.MESSAGE_V1_JSON)
    @PreAuthorize(
            "@reservationResourceAccess.canViewReservation(#id, @currentUserResolver.currentPrincipalOrNull())")
    public Response getMessage(
            @P("id") @PathParam("id") final long reservationId,
            @PathParam("messageId") final long messageId) {
        return reservationMessageHttpSupport.get(
                reservationId,
                messageId,
                currentUserResolver.currentPrincipalOrNull(),
                uriInfo);
    }

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(VndMediaType.MESSAGE_V1_JSON)
    @PreAuthorize(
            "@reservationResourceAccess.canViewReservation(#id, @currentUserResolver.currentPrincipalOrNull())")
    public Response postMessage(
            @P("id") @PathParam("id") final long reservationId,
            @FormDataParam("body") final String body,
            @FormDataParam("file") final FormDataBodyPart filePart) throws IOException {
        return reservationMessageHttpSupport.post(
                reservationId,
                body,
                filePart,
                currentUserResolver.requirePrincipal(),
                uriInfo);
    }
}
