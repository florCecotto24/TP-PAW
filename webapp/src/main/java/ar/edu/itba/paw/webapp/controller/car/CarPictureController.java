package ar.edu.itba.paw.webapp.controller.car;

import java.io.IOException;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.method.P;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

import ar.edu.itba.paw.webapp.api.common.VndMediaType;
import ar.edu.itba.paw.webapp.support.CarPictureHttpSupport;
import ar.edu.itba.paw.webapp.support.CurrentUserResolver;
import ar.edu.itba.paw.webapp.support.PaginationSupport;

/**
 * Car gallery sub-resource ({@code /cars/{id}/pictures}). HTTP routing only.
 *
 * Metadata and raw bytes share the same visibility gate as car detail
 * ({@code CarResourceAccess#requireViewableCar}, applied inside the support): anonymous
 * {@code <img src>} still works for
 * publicly viewable cars; paused / lack-doc / non-visible listings do not leak bytes by id.
 */
@Path("/cars/{id}/pictures")
@Component
public class CarPictureController {

    private final CarPictureHttpSupport carPictureHttpSupport;
    private final PaginationSupport paginationSupport;
    private final CurrentUserResolver currentUserResolver;

    @Context
    private UriInfo uriInfo;

    @Context
    private Request request;

    @Autowired
    public CarPictureController(
            final CarPictureHttpSupport carPictureHttpSupport,
            final PaginationSupport paginationSupport,
            final CurrentUserResolver currentUserResolver) {
        this.carPictureHttpSupport = carPictureHttpSupport;
        this.paginationSupport = paginationSupport;
        this.currentUserResolver = currentUserResolver;
    }

    @GET
    @Produces(VndMediaType.PICTURE_V1_JSON)
    public Response listPictures(
            @PathParam("id") final long carId,
            @QueryParam("page") @DefaultValue("1") final int page,
            @QueryParam("pageSize") final Integer pageSizeParam) {
        return carPictureHttpSupport.list(
                carId,
                paginationSupport.forCarGallery(page, pageSizeParam),
                currentUserResolver.currentPrincipalOrNull(),
                uriInfo);
    }

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(VndMediaType.PICTURE_V1_JSON)
    @PreAuthorize("@carResourceAccess.isOwnerById(#id, @currentUserResolver.currentPrincipalOrNull())")
    public Response addPicture(
            @P("id") @PathParam("id") final long carId,
            @FormDataParam("file") final FormDataBodyPart filePart) throws IOException {
        return carPictureHttpSupport.add(carId, filePart, uriInfo);
    }

    @GET
    @Path("/primary")
    public Response getPrimaryPictureBytes(@PathParam("id") final long carId) {
        return carPictureHttpSupport.primaryBytes(
                carId, currentUserResolver.currentPrincipalOrNull(), request);
    }

    @GET
    @Path("/{pictureId}")
    public Response getPictureBytes(
            @PathParam("id") final long carId,
            @PathParam("pictureId") final long pictureId) {
        return carPictureHttpSupport.pictureBytes(
                carId, pictureId, currentUserResolver.currentPrincipalOrNull(), request);
    }

    @DELETE
    @Path("/{pictureId}")
    @PreAuthorize("@carResourceAccess.isOwnerById(#id, @currentUserResolver.currentPrincipalOrNull())")
    public Response deletePicture(
            @P("id") @PathParam("id") final long carId,
            @PathParam("pictureId") final long pictureId) {
        return carPictureHttpSupport.delete(carId, pictureId);
    }
}
