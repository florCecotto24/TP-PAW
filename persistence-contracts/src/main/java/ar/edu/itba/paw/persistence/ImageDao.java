package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.domain.Image;
import java.util.Optional;

/** Image blobs referenced by profile and car gallery flows. */
public interface ImageDao {
    Image createImage(final String name, final String contentType, final byte[] data);

    Optional<Image> getImageById(final long id);

    void deleteImage(long id);
}
