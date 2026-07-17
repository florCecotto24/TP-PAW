package ar.edu.itba.paw.persistence.car;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.itba.paw.models.domain.car.Car;
import ar.edu.itba.paw.models.domain.car.CarBrand;
import ar.edu.itba.paw.models.domain.car.CarModel;
import ar.edu.itba.paw.persistence.car.CarModelDao;

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
    public List<CarModel> findPendingOrdered() {
        return em.createQuery(
                "FROM CarModel m JOIN FETCH m.brand "
                        + "WHERE m.validated = FALSE "
                        + "ORDER BY m.brand.id ASC, LOWER(m.name) ASC",
                CarModel.class)
                .getResultList();
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
