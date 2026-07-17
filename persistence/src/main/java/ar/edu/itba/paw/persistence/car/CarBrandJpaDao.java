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
import javax.persistence.Query;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.itba.paw.models.domain.car.CarBrand;
import ar.edu.itba.paw.models.dto.Page;

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
    @SuppressWarnings("unchecked")
    public Page<CarBrand> findPage(final Boolean validated, final int page, final int pageSize) {
        final int safePage = Math.max(0, page);
        final int safePageSize = Math.max(1, pageSize);
        final String where = validated == null
                ? ""
                : validated ? "WHERE b.validated = TRUE " : "WHERE b.validated = FALSE ";
        final String orderBy = validated == null
                ? "ORDER BY b.validated DESC, LOWER(b.name) ASC, b.id ASC"
                : "ORDER BY LOWER(b.name) ASC, b.id ASC";

        final long total = ((Number) em.createNativeQuery(
                        "SELECT COUNT(*) FROM car_brands b " + where)
                .getSingleResult()).longValue();
        if (total == 0L) {
            return new Page<>(List.of(), safePage, safePageSize, 0L);
        }

        final Query idQuery = em.createNativeQuery(
                "SELECT b.id FROM car_brands b " + where + orderBy + " LIMIT :limit OFFSET :offset");
        idQuery.setParameter("limit", safePageSize);
        idQuery.setParameter("offset", safePage * safePageSize);
        final List<Number> idRows = idQuery.getResultList();
        if (idRows.isEmpty()) {
            return new Page<>(List.of(), safePage, safePageSize, total);
        }

        final List<Long> ids = idRows.stream().map(Number::longValue).collect(Collectors.toList());
        final List<CarBrand> hydrated = em.createQuery(
                        "FROM CarBrand b WHERE b.id IN :ids", CarBrand.class)
                .setParameter("ids", ids)
                .getResultList();
        return new Page<>(hydrateInOrder(ids, hydrated), safePage, safePageSize, total);
    }

    private static List<CarBrand> hydrateInOrder(final List<Long> ids, final List<CarBrand> hydrated) {
        final Map<Long, CarBrand> byId = hydrated.stream()
                .collect(Collectors.toMap(CarBrand::getId, Function.identity()));
        final List<CarBrand> ordered = new ArrayList<>(ids.size());
        for (final Long id : ids) {
            final CarBrand brand = byId.get(id);
            if (brand != null) {
                ordered.add(brand);
            }
        }
        return ordered;
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
                .setParameter("name", normalized.toLowerCase(Locale.ROOT))
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
