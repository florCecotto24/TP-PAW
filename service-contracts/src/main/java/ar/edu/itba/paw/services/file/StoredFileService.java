package ar.edu.itba.paw.services.file;

import ar.edu.itba.paw.models.domain.file.StoredFile;
import ar.edu.itba.paw.models.dto.file.BinaryContent;

import java.util.Optional;

/**
 * Generic uploaded file blobs (documents, receipts) keyed by uploader and file id.
 * Implementations use {@code StoredFileDao} only.
 */
public interface StoredFileService {

    /** Persists content and returns the stored file row. */
    StoredFile create(long uploaderUserId, String fileName, String contentType, byte[] data);

    /** Loads file metadata and bytes when the id exists. */
    Optional<StoredFile> findById(long id);

    Optional<BinaryContent> findContentById(long id);
}
