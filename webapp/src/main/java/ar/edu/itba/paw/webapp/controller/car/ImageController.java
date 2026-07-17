package ar.edu.itba.paw.webapp.controller.car;

import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ar.edu.itba.paw.services.file.ImageService;
import ar.edu.itba.paw.webapp.support.CacheableBinaryResponses;
import ar.edu.itba.paw.webapp.support.CurrentUserResolver;
import ar.edu.itba.paw.webapp.support.ImageResourceAccess;

/**
 * Serves image bytes at {@code GET /image/{id}}.
 *
 * Profile and review images are public. Gallery images inherit their car's visibility.
 * KYC documents, payment proofs and insurance PDFs are <strong>not</strong> served here.
 */
@Path("/image")
@Component
public final class ImageController {

    private final ImageService imageService;
    private final ImageResourceAccess imageResourceAccess;
    private final CurrentUserResolver currentUserResolver;

    @Context
    private Request request;

    @Autowired
    public ImageController(
            final ImageService imageService,
            final ImageResourceAccess imageResourceAccess,
            final CurrentUserResolver currentUserResolver) {
        this.imageService = imageService;
        this.imageResourceAccess = imageResourceAccess;
        this.currentUserResolver = currentUserResolver;
    }

    @GET
    @Path("/{id}")
    public Response getImage(@PathParam("id") final long id) {
        imageResourceAccess.requireViewableImage(id, currentUserResolver.currentPrincipalOrNull());
        return imageService.getImageContent(id)
                .map(content -> CacheableBinaryResponses.of(request, content))
                .orElseThrow(NotFoundException::new);
    }
}
