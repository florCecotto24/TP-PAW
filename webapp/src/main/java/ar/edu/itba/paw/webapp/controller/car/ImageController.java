package ar.edu.itba.paw.webapp.controller.car;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ar.edu.itba.paw.models.dto.file.BinaryContent;
import ar.edu.itba.paw.services.file.ImageService;

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

    @Autowired
    public ImageController(final ImageService imageService) {
        this.imageService = imageService;
    }

    @GET
    @Path("/{id}")
    public Response getImage(@PathParam("id") final long id) {
        return imageService.getImageContent(id)
                .map(this::binaryResponse)
                .orElseGet(() -> Response.status(Response.Status.NOT_FOUND).build());
    }

    private Response binaryResponse(final BinaryContent content) {
        return Response.ok(content.getBytes())
                .type(content.getContentType())
                .build();
    }
}
