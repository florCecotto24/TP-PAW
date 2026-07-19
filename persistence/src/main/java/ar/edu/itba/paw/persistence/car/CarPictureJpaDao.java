package ar.edu.itba.paw.persistence.car;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.itba.paw.models.domain.car.Car;
import ar.edu.itba.paw.models.domain.car.CarPicture;
import ar.edu.itba.paw.models.domain.file.Image;
import ar.edu.itba.paw.models.domain.file.StoredFile;
import ar.edu.itba.paw.models.dto.car.CarPictureSummary;

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
        final List<CarPicture> rows = em.createQuery(
                        "FROM CarPicture cp "
                                + "LEFT JOIN FETCH cp.image "
                                + "LEFT JOIN FETCH cp.storedFile "
                                + "WHERE cp.id = :id",
                        CarPicture.class)
                .setParameter("id", id)
                .getResultList();
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
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
    @SuppressWarnings("unchecked")
    public List<CarPicture> findByCarIdOrderByDisplayOrderAsc(
            final long carId, final int offset, final int limit) {
        final int safeOffset = Math.max(0, offset);
        final int safeLimit = Math.max(1, limit);
        final List<Number> idRows = em.createNativeQuery(
                        "SELECT cp.id FROM car_pictures cp "
                                + "WHERE cp.car_id = :carId "
                                + "ORDER BY cp.display_order ASC, cp.id ASC "
                                + "LIMIT :limit OFFSET :offset")
                .setParameter("carId", carId)
                .setParameter("limit", safeLimit)
                .setParameter("offset", safeOffset)
                .getResultList();
        if (idRows.isEmpty()) {
            return List.of();
        }
        final List<Long> ids = idRows.stream().map(Number::longValue).collect(Collectors.toList());
        final List<CarPicture> hydrated = em.createQuery(
                        "FROM CarPicture cp "
                                + "LEFT JOIN FETCH cp.image "
                                + "LEFT JOIN FETCH cp.storedFile "
                                + "WHERE cp.id IN :ids",
                        CarPicture.class)
                .setParameter("ids", ids)
                .getResultList();
        return hydratePicturesInOrder(ids, hydrated);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<CarPictureSummary> findSummariesByCarIdOrderByDisplayOrderAsc(
            final long carId, final int offset, final int limit) {
        // Native page of scalar columns only — image/stored-file byte[] blobs stay out of memory.
        final int safeOffset = Math.max(0, offset);
        final int safeLimit = Math.max(1, limit);
        final List<Object[]> rows = em.createNativeQuery(
                        "SELECT cp.id, cp.car_id, cp.display_order, img.content_type AS image_content_type, sf.content_type AS stored_file_content_type "
                                + "FROM car_pictures cp "
                                + "LEFT JOIN images img ON img.id = cp.image_id "
                                + "LEFT JOIN stored_files sf ON sf.id = cp.stored_file_id "
                                + "WHERE cp.car_id = :carId "
                                + "ORDER BY cp.display_order ASC, cp.id ASC "
                                + "LIMIT :limit OFFSET :offset")
                .setParameter("carId", carId)
                .setParameter("limit", safeLimit)
                .setParameter("offset", safeOffset)
                .getResultList();
        final List<CarPictureSummary> summaries = new ArrayList<>(rows.size());
        for (final Object[] row : rows) {
            summaries.add(new CarPictureSummary(
                    ((Number) row[0]).longValue(),
                    ((Number) row[1]).longValue(),
                    ((Number) row[2]).intValue(),
                    (String) row[3],
                    (String) row[4]));
        }
        return summaries;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Optional<CarPicture> findFirstByCarIdOrderByDisplayOrderAsc(final long carId) {
        final List<Number> idRows = em.createNativeQuery(
                        "SELECT cp.id FROM car_pictures cp "
                                + "WHERE cp.car_id = :carId "
                                + "ORDER BY cp.display_order ASC, cp.id ASC "
                                + "LIMIT 1")
                .setParameter("carId", carId)
                .getResultList();
        if (idRows.isEmpty()) {
            return Optional.empty();
        }
        final long pictureId = idRows.get(0).longValue();
        final List<CarPicture> rows = em.createQuery(
                        "FROM CarPicture cp "
                                + "LEFT JOIN FETCH cp.image "
                                + "LEFT JOIN FETCH cp.storedFile "
                                + "WHERE cp.id = :pictureId",
                        CarPicture.class)
                .setParameter("pictureId", pictureId)
                .getResultList();
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    private static List<CarPicture> hydratePicturesInOrder(
            final List<Long> ids, final List<CarPicture> hydrated) {
        final Map<Long, CarPicture> byId = hydrated.stream()
                .collect(Collectors.toMap(CarPicture::getId, Function.identity()));
        final List<CarPicture> ordered = new ArrayList<>(ids.size());
        for (final Long id : ids) {
            final CarPicture picture = byId.get(id);
            if (picture != null) {
                ordered.add(picture);
            }
        }
        return ordered;
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
    public List<Long> findCarIdsByImageId(final long imageId) {
        // Scalar car.id only: ACL callers need membership, not gallery entities or image blobs.
        return em.createQuery(
                        "SELECT DISTINCT cp.car.id FROM CarPicture cp WHERE cp.image.id = :imageId",
                        Long.class)
                .setParameter("imageId", imageId)
                .getResultList();
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
