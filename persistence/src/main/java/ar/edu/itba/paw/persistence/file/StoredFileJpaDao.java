package ar.edu.itba.paw.persistence.file;

import java.time.OffsetDateTime;
import java.util.Optional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.itba.paw.models.domain.file.StoredFile;
import ar.edu.itba.paw.models.domain.user.User;
import ar.edu.itba.paw.persistence.file.StoredFileDao;

@Transactional(readOnly = true)
@Repository
public class StoredFileJpaDao implements StoredFileDao {

    @PersistenceContext
    private EntityManager em;

    @Override
    @Transactional
    public StoredFile create(final long uploaderUserId, final String fileName,
                             final String contentType, final byte[] data) {
        final User uploaderRef = em.getReference(User.class, uploaderUserId);
        final StoredFile file = StoredFile.forUpload(uploaderRef, fileName, contentType, data, OffsetDateTime.now());
        em.persist(file);
        return file;
    }

    @Override
    public Optional<StoredFile> findById(final long id) {
        return Optional.ofNullable(em.find(StoredFile.class, id));
    }

    @Override
    @Transactional
    public boolean deleteById(final long id) {
        final StoredFile file = em.find(StoredFile.class, id);
        if (file == null) {
            return false;
        }
        em.remove(file);
        return true;
    }
}
