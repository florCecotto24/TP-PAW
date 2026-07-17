package ar.edu.itba.paw.webapp.support;

import java.util.List;

import javax.ws.rs.NotFoundException;

import org.springframework.stereotype.Component;

import ar.edu.itba.paw.services.car.CarPictureService;
import ar.edu.itba.paw.webapp.security.auth.userdetails.RydenUserDetails;

/** Applies gallery visibility rules to images served through the generic image resource. */
@Component
public final class ImageResourceAccess {

    private final CarPictureService carPictureService;
    private final CarResourceAccess carResourceAccess;

    public ImageResourceAccess(
            final CarPictureService carPictureService,
            final CarResourceAccess carResourceAccess) {
        this.carPictureService = carPictureService;
        this.carResourceAccess = carResourceAccess;
    }

    /**
     * Non-gallery images remain public. A gallery image is readable when at least one car exposing
     * those same bytes is viewable by the caller.
     */
    public void requireViewableImage(final long imageId, final RydenUserDetails viewer) {
        final List<Long> carIds = carPictureService.findCarIdsByImageId(imageId);
        if (!carIds.isEmpty()
                && carIds.stream().noneMatch(carId ->
                        carResourceAccess.canViewExistingCarById(carId, viewer))) {
            throw new NotFoundException();
        }
    }
}
