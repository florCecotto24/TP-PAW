package ar.edu.itba.paw.persistence;

import java.util.Optional;

import ar.edu.itba.paw.models.StoredFile;

public interface StoredFileDao {

    StoredFile create(long uploaderUserId, String fileName, String contentType, byte[] data);

    Optional<StoredFile> findById(long id);
}
