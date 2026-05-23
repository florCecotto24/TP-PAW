package ar.edu.itba.paw.persistence.hibernate;

import java.util.List;
import java.util.Optional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.itba.paw.models.domain.Car;
import ar.edu.itba.paw.models.domain.CarBrand;
import ar.edu.itba.paw.models.domain.CarModel;
import ar.edu.itba.paw.persistence.CarModelDao;

@Transactional(readOnly = true)
@Repository
public class CarModelJpaDao implements CarModelDao {

    @PersistenceContext
    private EntityManager em;

    @Override
    public List<CarModel> findByBrandIdOrdered(final long brandId) {
        return em.createQuery(
                "FROM CarModel m WHERE m.brand.id = :brandId "
                        + "ORDER BY m.validated DESC, LOWER(m.name) ASC", CarModel.class)
                .setParameter("brandId", brandId)
                .getResultList();
    }

    @Override
    public List<CarModel> findValidatedByBrandIdOrdered(final long brandId) {
        return em.createQuery(
                "FROM CarModel m WHERE m.brand.id = :brandId AND m.validated = TRUE "
                        + "ORDER BY LOWER(m.name) ASC", CarModel.class)
                .setParameter("brandId", brandId)
                .getResultList();
    }

    @Override
    public List<CarModel> findAllOrderedGroupedByBrand() {
        return em.createQuery(
                "FROM CarModel m ORDER BY m.brand.id ASC, m.validated DESC, LOWER(m.name) ASC",
                CarModel.class)
                .getResultList();
    }

    @Override
    public Optional<CarModel> findById(final long modelId) {
        return Optional.ofNullable(em.find(CarModel.class, modelId));
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
                .setParameter("name", normalized.toLowerCase())
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
}
