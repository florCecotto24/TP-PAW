package ar.edu.itba.paw.persistence.car;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.itba.paw.models.domain.car.Car;
import ar.edu.itba.paw.models.domain.car.CarPicture;
import ar.edu.itba.paw.models.domain.file.Image;
import ar.edu.itba.paw.models.domain.file.StoredFile;
import ar.edu.itba.paw.persistence.car.CarPictureDao;

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
        return findByCarIdOrderByDisplayOrderAsc(carId, 0, Integer.MAX_VALUE);
    }

    @Override
    public long countByCarId(final long carId) {
        final Long count = em.createQuery(
                        "SELECT COUNT(cp) FROM CarPicture cp WHERE cp.car.id = :carId", Long.class)
                .setParameter("carId", carId)
                .getSingleResult();
        return count != null ? count : 0L;
    }

    @Override
    public List<CarPicture> findByCarIdOrderByDisplayOrderAsc(
            final long carId, final int offset, final int limit) {
        return em.createQuery(
                        "FROM CarPicture cp "
                                + "LEFT JOIN FETCH cp.image "
                                + "LEFT JOIN FETCH cp.storedFile "
                                + "WHERE cp.car.id = :carId ORDER BY cp.displayOrder ASC",
                        CarPicture.class)
                .setParameter("carId", carId)
                .setFirstResult(Math.max(0, offset))
                .setMaxResults(Math.max(1, limit))
                .getResultList();
    }

    @Override
    public Optional<CarPicture> findFirstByCarIdOrderByDisplayOrderAsc(final long carId) {
        final List<CarPicture> rows = em.createQuery(
                        "FROM CarPicture cp "
                                + "LEFT JOIN FETCH cp.image "
                                + "LEFT JOIN FETCH cp.storedFile "
                                + "WHERE cp.car.id = :carId ORDER BY cp.displayOrder ASC",
                        CarPicture.class)
                .setParameter("carId", carId)
                .setMaxResults(1)
                .getResultList();
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    @Override
    public Optional<Integer> findMaxDisplayOrderByCarId(final long carId) {
        final Integer max = em.createQuery(
                        "SELECT MAX(cp.displayOrder) FROM CarPicture cp WHERE cp.car.id = :carId",
                        Integer.class)
                .setParameter("carId", carId)
                .getSingleResult();
        return Optional.ofNullable(max);
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

    @Override
    public Map<Long, Long> findCoverImageIdsByCarIds(final Collection<Long> carIds) {
        if (carIds == null || carIds.isEmpty()) {
            return Map.of();
        }
        // Multi-select projection (carId, imageId) ordered by displayOrder; the first row per car
        // is the cover. Avoids loading the full entities and any LAZY proxy access for ids.
        final List<Object[]> rows = em.createQuery(
                        "SELECT cp.car.id, cp.image.id FROM CarPicture cp "
                                + "WHERE cp.car.id IN :carIds AND cp.image IS NOT NULL "
                                + "ORDER BY cp.car.id ASC, cp.displayOrder ASC",
                        Object[].class)
                .setParameter("carIds", carIds)
                .getResultList();
        final Map<Long, Long> result = new HashMap<>();
        for (final Object[] row : rows) {
            final long carId = ((Number) row[0]).longValue();
            final long imageId = ((Number) row[1]).longValue();
            // First row per car wins (lowest displayOrder thanks to ORDER BY).
            result.putIfAbsent(carId, imageId);
        }
        return result;
    }

    @Override
    @Transactional
    public void deleteCarPicture(final long id) {
        final CarPicture picture = em.find(CarPicture.class, id);
        if (picture != null) {
            em.remove(picture);
        }
    }
}
