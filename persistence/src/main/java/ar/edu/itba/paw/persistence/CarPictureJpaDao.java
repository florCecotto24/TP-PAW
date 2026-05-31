package ar.edu.itba.paw.persistence;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.itba.paw.models.domain.Car;
import ar.edu.itba.paw.models.domain.CarPicture;
import ar.edu.itba.paw.models.domain.Image;
import ar.edu.itba.paw.models.domain.StoredFile;
import ar.edu.itba.paw.persistence.CarPictureDao;

@Transactional(readOnly = true)
@Repository
public class CarPictureJpaDao implements CarPictureDao {

    @PersistenceContext
    private EntityManager em;

    @Override
    @Transactional
    public CarPicture createCarPicture(final long carId, final long imageId, final int displayOrder) {
        final Car carRef = em.getReference(Car.class, carId);
        final Image imageRef = em.getReference(Image.class, imageId);
        final OffsetDateTime now = OffsetDateTime.now();
        final CarPicture picture = CarPicture.forImage(carRef, imageRef, displayOrder, now, now);
        em.persist(picture);
        return picture;
    }

    @Override
    @Transactional
    public CarPicture createCarPictureFromVideo(
            final long carId, final long storedFileId, final int displayOrder) {
        final Car carRef = em.getReference(Car.class, carId);
        final StoredFile storedFileRef = em.getReference(StoredFile.class, storedFileId);
        final OffsetDateTime now = OffsetDateTime.now();
        final CarPicture picture = CarPicture.forVideo(carRef, storedFileRef, displayOrder, now, now);
        em.persist(picture);
        return picture;
    }

    @Override
    public Optional<CarPicture> getCarPictureById(final long id) {
        return Optional.ofNullable(em.find(CarPicture.class, id));
    }

    @Override
    public List<CarPicture> getCarPicturesByCarId(final long carId) {
        return em.createQuery(
                        "SELECT cp FROM CarPicture cp "
                                + "LEFT JOIN FETCH cp.image "
                                + "LEFT JOIN FETCH cp.storedFile "
                                + "WHERE cp.car.id = :carId ORDER BY cp.displayOrder ASC",
                        CarPicture.class)
                .setParameter("carId", carId)
                .getResultList();
    }

    @Override
    public boolean isStoredFileInCarGallery(final long storedFileId) {
        final Long count = em.createQuery(
                        "SELECT COUNT(cp) FROM CarPicture cp WHERE cp.storedFile.id = :storedFileId",
                        Long.class)
                .setParameter("storedFileId", storedFileId)
                .getSingleResult();
        return count != null && count > 0;
    }
}
