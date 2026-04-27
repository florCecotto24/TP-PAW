package ar.edu.itba.paw.services;

import java.util.Optional;

import ar.edu.itba.paw.models.domain.Image;

public interface ImageService {
    Image createImage(final String name, final String contentType, final byte[] data);

    Optional<Image> getImageById(final long id);

    void deleteImage(long id);

    long getMaxImageBytes();

    long getMaxImageMegabytesRoundedUp();
}
