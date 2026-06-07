package ar.edu.itba.paw.persistence.car;

import java.util.List;
import java.util.Optional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.itba.paw.models.domain.car.CarBrand;
import ar.edu.itba.paw.persistence.car.CarBrandDao;

@Transactional(readOnly = true)
@Repository
public class CarBrandJpaDao implements CarBrandDao {

    @PersistenceContext
    private EntityManager em;

    @Override
    public List<CarBrand> findAllOrdered() {
        return em.createQuery(
                "FROM CarBrand b ORDER BY b.validated DESC, LOWER(b.name) ASC", CarBrand.class)
                .getResultList();
    }

    @Override
    public List<CarBrand> findValidatedOrdered() {
        return em.createQuery(
                "FROM CarBrand b WHERE b.validated = TRUE ORDER BY LOWER(b.name) ASC", CarBrand.class)
                .getResultList();
    }

    @Override
    public Optional<CarBrand> findById(final long brandId) {
        return Optional.ofNullable(em.find(CarBrand.class, brandId));
    }

    @Override
    public Optional<CarBrand> findByNameIgnoreCase(final String name) {
        if (name == null) {
            return Optional.empty();
        }
        final String normalized = name.trim();
        if (normalized.isEmpty()) {
            return Optional.empty();
        }
        return em.createQuery(
                "FROM CarBrand b WHERE LOWER(b.name) = :name", CarBrand.class)
                .setParameter("name", normalized.toLowerCase())
                .getResultList()
                .stream()
                .findFirst();
    }

    @Override
    @Transactional
    public CarBrand create(final String name, final boolean validated) {
        final CarBrand brand = CarBrand.builder()
                .name(name)
                .validated(validated)
                .build();
        em.persist(brand);
        return brand;
    }

    @Override
    public List<CarBrand> findPendingOrdered() {
        return em.createQuery(
                "FROM CarBrand b WHERE b.validated = FALSE ORDER BY LOWER(b.name) ASC", CarBrand.class)
                .getResultList();
    }

    @Override
    @Transactional
    public void deleteById(final long brandId) {
        final CarBrand brand = em.find(CarBrand.class, brandId);
        if (brand != null) {
            em.remove(brand);
        }
    }
}
