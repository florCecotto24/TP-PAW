package ar.edu.itba.paw.persistence.file;

import java.util.Optional;

import ar.edu.itba.paw.models.domain.file.StoredFile;

/** Binary file payloads linked to an uploader user. */
public interface StoredFileDao {

    StoredFile create(long uploaderUserId, String fileName, String contentType, byte[] data);

    Optional<StoredFile> findById(long id);
}
