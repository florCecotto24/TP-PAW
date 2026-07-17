package ar.edu.itba.paw.persistence.file;

import java.util.Optional;

import ar.edu.itba.paw.models.domain.file.StoredFile;

/** Binary file payloads linked to an uploader user. */
public interface StoredFileDao {

    StoredFile create(long uploaderUserId, String fileName, String contentType, byte[] data);

    Optional<StoredFile> findById(long id);

    /**
     * Removes a stored file row by id when present (dirty-checking {@code em.remove}).
     * Used to compensate an orphan upload if a later attach step fails in the same use case.
     *
     * @return {@code true} when a row was removed
     */
    boolean deleteById(long id);
}
