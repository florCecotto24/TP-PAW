package ar.edu.itba.paw.persistence.car;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.itba.paw.models.domain.car.Car;
import ar.edu.itba.paw.models.domain.car.CarBrand;
import ar.edu.itba.paw.models.domain.car.CarModel;
import ar.edu.itba.paw.models.dto.Page;

@Transactional(readOnly = true)
@Repository
public class CarModelJpaDao implements CarModelDao {

    @PersistenceContext
    private EntityManager em;

    @Override
    public List<CarModel> findByBrandIdOrdered(final long brandId) {
        return em.createQuery(
                "FROM CarModel m JOIN FETCH m.brand WHERE m.brand.id = :brandId "
                        + "ORDER BY m.validated DESC, LOWER(m.name) ASC", CarModel.class)
                .setParameter("brandId", brandId)
                .getResultList();
    }

    @Override
    public List<CarModel> findValidatedByBrandIdOrdered(final long brandId) {
        return em.createQuery(
                "FROM CarModel m JOIN FETCH m.brand WHERE m.brand.id = :brandId AND m.validated = TRUE "
                        + "ORDER BY LOWER(m.name) ASC", CarModel.class)
                .setParameter("brandId", brandId)
                .getResultList();
    }

    @Override
    public List<CarModel> findAllOrderedGroupedByBrand() {
        return em.createQuery(
                "FROM CarModel m JOIN FETCH m.brand "
                        + "ORDER BY m.brand.id ASC, m.validated DESC, LOWER(m.name) ASC",
                CarModel.class)
                .getResultList();
    }

    @Override
    public Optional<CarModel> findById(final long modelId) {
        return em.createQuery(
                        "FROM CarModel m JOIN FETCH m.brand WHERE m.id = :modelId",
                        CarModel.class)
                .setParameter("modelId", modelId)
                .getResultList()
                .stream()
                .findFirst();
    }

    @Override
    public Optional<CarModel> findByBrandIdAndNameIgnoreCase(final long brandId, final String name) {
        if (name == null) {
            return Optional.empty();
        }
        final String normalized = name.trim();
        if (normalized.isEmpty()) {
            return Optional.empty();
        }
        return em.createQuery(
                "FROM CarModel m WHERE m.brand.id = :brandId AND LOWER(m.name) = :name",
                CarModel.class)
                .setParameter("brandId", brandId)
                .setParameter("name", normalized.toLowerCase(Locale.ROOT))
                .getResultList()
                .stream()
                .findFirst();
    }

    @Override
    @Transactional
    public CarModel create(final long brandId, final String name, final boolean validated, final Car.Type type) {
        final CarBrand brandRef = em.getReference(CarBrand.class, brandId);
        final CarModel model = CarModel.builder()
                .brand(brandRef)
                .name(name)
                .validated(validated)
                .type(type)
                .build();
        em.persist(model);
        return model;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Page<CarModel> findPendingPage(final int page, final int pageSize) {
        final int safePage = Math.max(0, page);
        final int safePageSize = Math.max(1, pageSize);

        final long total = ((Number) em.createNativeQuery(
                        "SELECT COUNT(*) FROM car_models m WHERE m.validated = FALSE")
                .getSingleResult()).longValue();
        if (total == 0L) {
            return new Page<>(List.of(), safePage, safePageSize, 0L);
        }

        final List<Number> idRows = em.createNativeQuery(
                        "SELECT m.id FROM car_models m "
                                + "WHERE m.validated = FALSE "
                                + "ORDER BY m.brand_id ASC, LOWER(m.name) ASC, m.id ASC "
                                + "LIMIT :limit OFFSET :offset")
                .setParameter("limit", safePageSize)
                .setParameter("offset", safePage * safePageSize)
                .getResultList();
        if (idRows.isEmpty()) {
            return new Page<>(List.of(), safePage, safePageSize, total);
        }

        final List<Long> ids = idRows.stream().map(Number::longValue).collect(Collectors.toList());
        final List<CarModel> hydrated = em.createQuery(
                        "FROM CarModel m JOIN FETCH m.brand WHERE m.id IN :ids",
                        CarModel.class)
                .setParameter("ids", ids)
                .getResultList();
        return new Page<>(hydrateInOrder(ids, hydrated), safePage, safePageSize, total);
    }

    private static List<CarModel> hydrateInOrder(final List<Long> ids, final List<CarModel> hydrated) {
        final Map<Long, CarModel> byId = hydrated.stream()
                .collect(Collectors.toMap(CarModel::getId, Function.identity()));
        final List<CarModel> ordered = new ArrayList<>(ids.size());
        for (final Long id : ids) {
            final CarModel model = byId.get(id);
            if (model != null) {
                ordered.add(model);
            }
        }
        return ordered;
    }

    @Override
    @Transactional
    public void deleteById(final long modelId) {
        final CarModel model = em.find(CarModel.class, modelId);
        if (model != null) {
            em.remove(model);
        }
    }

    @Override
    public int countByBrandId(final long brandId) {
        final Long count = em.createQuery(
                "SELECT COUNT(m) FROM CarModel m WHERE m.brand.id = :brandId", Long.class)
                .setParameter("brandId", brandId)
                .getSingleResult();
        return count.intValue();
    }
}
