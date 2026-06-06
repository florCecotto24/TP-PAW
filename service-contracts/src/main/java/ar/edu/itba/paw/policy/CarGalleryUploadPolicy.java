package ar.edu.itba.paw.policy;

/**
 * Limits for car publish gallery uploads: up to {@link #MAX_ITEMS} mixed photos/videos with per-type byte caps.
 */
public interface CarGalleryUploadPolicy {

    /** Maximum number of media items (photos + videos combined) per car gallery. */
    int MAX_ITEMS = 8;

    int getMaxItems();

    int getMaxImageBytes();

    int getMaxVideoBytes();

    int getMaxImageMegabytesRoundedUp();

    int getMaxVideoMegabytesRoundedUp();
}
