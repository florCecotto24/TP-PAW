package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.domain.StoredFile;
import ar.edu.itba.paw.persistence.StoredFileDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/** Pass-through to {@link StoredFileDao}. */
@Service
public final class StoredFileServiceImpl implements StoredFileService {

    private final StoredFileDao storedFileDao;

    @Autowired
    public StoredFileServiceImpl(final StoredFileDao storedFileDao) {
        this.storedFileDao = storedFileDao;
    }

    @Override
    @Transactional
    public StoredFile create(
            final long uploaderUserId,
            final String fileName,
            final String contentType,
            final byte[] data) {
        return storedFileDao.create(uploaderUserId, fileName, contentType, data);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<StoredFile> findById(final long id) {
        return storedFileDao.findById(id);
    }
}
