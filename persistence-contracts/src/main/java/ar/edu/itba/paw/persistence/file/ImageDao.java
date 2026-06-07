package ar.edu.itba.paw.persistence.file;

import ar.edu.itba.paw.models.domain.file.Image;
import java.util.Optional;

/** Image blobs referenced by profile and car gallery flows. */
public interface ImageDao {
    Image createImage(final String name, final String contentType, final byte[] data);

    Optional<Image> getImageById(final long id);

    void deleteImage(long id);
}
