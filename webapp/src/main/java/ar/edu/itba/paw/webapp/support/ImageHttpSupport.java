package ar.edu.itba.paw.webapp.support;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;

import org.springframework.stereotype.Component;

import ar.edu.itba.paw.services.file.ImageService;
import ar.edu.itba.paw.webapp.security.auth.userdetails.RydenUserDetails;

/**
 * HTTP binding for {@code GET /image/{id}} (ACL + conditional binary response).
 */
@Component
public final class ImageHttpSupport {

    private final ImageService imageService;
    private final ImageResourceAccess imageResourceAccess;

    public ImageHttpSupport(final ImageService imageService, final ImageResourceAccess imageResourceAccess) {
        this.imageService = imageService;
        this.imageResourceAccess = imageResourceAccess;
    }

    public Response get(final long imageId, final RydenUserDetails viewer, final Request request) {
        imageResourceAccess.requireViewableImage(imageId, viewer);
        return imageService.getImageContent(imageId)
                .map(content -> CacheableBinaryResponses.of(request, content))
                .orElseThrow(NotFoundException::new);
    }
}
