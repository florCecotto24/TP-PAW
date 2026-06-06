package ar.edu.itba.paw.services.file;

import java.util.Optional;

import ar.edu.itba.paw.models.domain.Image;
import ar.edu.itba.paw.models.dto.file.BinaryContent;

/**
 * Binary image rows (profile, car pictures) with configured size limits.
 * Implementations use {@code ImageDao} for persistence; size rules come from configuration.
 */
public interface ImageService {
    /** Stores bytes and metadata; validates size and content type per policy. */
    Image createImage(final String name, final String contentType, final byte[] data);

    /** Loads image metadata and payload when present. */
    Optional<Image> getImageById(final long id);

    /**
     * Loads inline bytes + content type for {@code id} and returns them as a detached
     * {@link BinaryContent} value object so download endpoints don't leak the JPA entity
     */
    Optional<BinaryContent> getImageContent(long id);

    /** Removes the image row when it exists. */
    void deleteImage(long id);

    /** Maximum upload size in bytes ({@code app.upload.*}). */
    long getMaxImageBytes();

    /** Same limit expressed in whole megabytes rounded up (UI hints). */
    long getMaxImageMegabytesRoundedUp();
}
