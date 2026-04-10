package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.Image;
import java.util.Optional;

public interface ImageDao {
    Image createImage(final String name, final String contentType, final byte[] data);

    Optional<Image> getImageById(final long id);

    void deleteImage(long id);
}
