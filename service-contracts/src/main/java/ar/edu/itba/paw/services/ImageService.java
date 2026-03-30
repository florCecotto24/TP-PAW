package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.Image;

import java.util.Optional;

public interface ImageService {
    Image createImage(final String name, final String contentType, final byte[] data);

    Optional<Image> getImageById(final long id);
}
