package ar.edu.itba.paw.persistence.hibernate;

import java.time.OffsetDateTime;
import java.util.Optional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.itba.paw.models.domain.StoredFile;
import ar.edu.itba.paw.models.domain.User;
import ar.edu.itba.paw.persistence.StoredFileDao;

@Transactional
@Repository
public class StoredFileJpaDao implements StoredFileDao {

    @PersistenceContext
    private EntityManager em;

    @Override
    public StoredFile create(final long uploaderUserId, final String fileName,
                             final String contentType, final byte[] data) {
        final User uploaderRef = em.getReference(User.class, uploaderUserId);
        final StoredFile file = new StoredFile(uploaderRef, fileName, contentType, data, OffsetDateTime.now());
        em.persist(file);
        return file;
    }

    @Override
    public Optional<StoredFile> findById(final long id) {
        return Optional.ofNullable(em.find(StoredFile.class, id));
    }
}
