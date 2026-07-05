package ar.edu.itba.paw.webapp.controller.car;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ar.edu.itba.paw.services.file.ImageService;
import ar.edu.itba.paw.webapp.support.CacheableBinaryResponses;

/**
 * Serves public image bytes at {@code GET /image/{id}} (profile pictures, review photos, etc.).
 *
 * <p>Anonymous read is intentional: these bytes back publicly browsable UI. KYC documents,
 * payment proofs and insurance PDFs are <strong>not</strong> served here.</p>
 */
@Path("/image")
@Component
public final class ImageController {

    private final ImageService imageService;

    @Context
    private Request request;

    @Autowired
    public ImageController(final ImageService imageService) {
        this.imageService = imageService;
    }

    @GET
    @Path("/{id}")
    public Response getImage(@PathParam("id") final long id) {
        return imageService.getImageContent(id)
                .map(content -> CacheableBinaryResponses.of(request, content))
                .orElseGet(() -> Response.status(Response.Status.NOT_FOUND).build());
    }
}
