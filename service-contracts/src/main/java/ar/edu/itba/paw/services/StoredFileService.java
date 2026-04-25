package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.StoredFile;

import java.util.Optional;

public interface StoredFileService {

    StoredFile create(long uploaderUserId, String fileName, String contentType, byte[] data);

    Optional<StoredFile> findById(long id);
}
