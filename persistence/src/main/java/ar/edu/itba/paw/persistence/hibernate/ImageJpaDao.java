package ar.edu.itba.paw.persistence.hibernate;

import java.util.Optional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.itba.paw.models.domain.Image;
import ar.edu.itba.paw.persistence.ImageDao;

@Transactional
@Repository
public class ImageJpaDao implements ImageDao {

    @PersistenceContext
    private EntityManager em;

    @Override
    public Image createImage(final String name, final String contentType, final byte[] data) {
        final Image image = new Image(name, contentType, data);
        em.persist(image);
        return image;
    }

    @Override
    public Optional<Image> getImageById(final long id) {
        return Optional.ofNullable(em.find(Image.class, id));
    }

    @Override
    public void deleteImage(final long id) {
        final Image image = em.find(Image.class, id);
        if (image != null) {
            em.remove(image);
        }
    }
}
