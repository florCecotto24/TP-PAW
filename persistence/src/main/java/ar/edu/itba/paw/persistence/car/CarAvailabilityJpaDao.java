package ar.edu.itba.paw.persistence.car;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.itba.paw.models.domain.car.Car;
import ar.edu.itba.paw.models.domain.car.CarAvailability;
import ar.edu.itba.paw.models.domain.location.Neighborhood;
import ar.edu.itba.paw.persistence.car.CarAvailabilityDao;

@Transactional(readOnly = true)
@Repository
public class CarAvailabilityJpaDao implements CarAvailabilityDao {

    @PersistenceContext
    private EntityManager em;

    @Override
    public Optional<CarAvailability> findById(final long availabilityId) {
        return Optional.ofNullable(em.find(CarAvailability.class, availabilityId));
    }

    @Override
    public Optional<CarAvailability> findEffectiveForDayByCar(final long carId, final LocalDate day) {
        /*
         * 1+1 query pattern (project rule: no LIMIT/OFFSET on JPQL):
         * - Step 1 (native): filter, order by {@code (created_at DESC, id DESC)} and apply
         *   {@code LIMIT 1} on {@code car_availability} to get the most recent availability
         *   that covers the given day. The WHERE does NOT guarantee uniqueness (multiple
         *   availabilities may overlap the same day; we deliberately want the most recently
         *   created one), so this is a "top 1" pagination case and must use the 1+1 pattern.
         * - Step 2 (JPQL):   hydrate the {@link CarAvailability} entity by id.
         */
        @SuppressWarnings("unchecked")
        final List<Number> ids = em.createNativeQuery(
                        "SELECT la.id FROM car_availability la "
                                + "WHERE la.car_id = :carId "
                                + "AND la.start_date <= :day AND la.end_date >= :day "
                                + "ORDER BY la.created_at DESC, la.id DESC "
                                + "LIMIT 1")
                .setParameter("carId", carId)
                .setParameter("day", day)
                .getResultList();
        if (ids.isEmpty()) {
            return Optional.empty();
        }
        final long availabilityId = ids.get(0).longValue();
        return Optional.ofNullable(em.find(CarAvailability.class, availabilityId));
    }

    @Override
    public List<CarAvailability> findOverlappingRangeByCar(final long carId, final LocalDate from, final LocalDate to) {
        return em.createQuery(
                        "FROM CarAvailability la LEFT JOIN FETCH la.neighborhood "
                                + "WHERE la.car.id = :carId "
                                + "AND la.startInclusive <= :to AND la.endInclusive >= :from "
                                + "ORDER BY la.createdAt DESC, la.id DESC",
                        CarAvailability.class)
                .setParameter("carId", carId)
                .setParameter("from", from)
                .setParameter("to", to)
                .getResultList();
    }

    @Override
    @Transactional
    public CarAvailability createFullForCar(
            final long carId,
            final LocalDate startInclusive,
            final LocalDate endInclusive,
            final BigDecimal dayPrice,
            final String startPointStreet,
            final String startPointNumber,
            final Long neighborhoodId,
            final LocalTime checkInTime,
            final LocalTime checkOutTime,
            final CarAvailability.Kind kind) {
        final Car carRef = em.getReference(Car.class, carId);
        final Neighborhood neighborhoodRef = neighborhoodId == null
                ? null
                : em.getReference(Neighborhood.class, neighborhoodId);
        final OffsetDateTime now = OffsetDateTime.now();
        final CarAvailability availability = CarAvailability.builder()
                .car(carRef)
                .startInclusive(startInclusive)
                .endInclusive(endInclusive)
                .dayPrice(dayPrice)
                .startPointStreet(startPointStreet)
                .startPointNumber(startPointNumber)
                .neighborhood(neighborhoodRef)
                .checkInTime(checkInTime)
                .checkOutTime(checkOutTime)
                .kind(kind)
                .createdAt(now)
                .updatedAt(now)
                .build();
        em.persist(availability);
        return availability;
    }

    @Override
    public List<CarAvailability> findByCarId(final long carId) {
        return em.createQuery(
                        "FROM CarAvailability la LEFT JOIN FETCH la.neighborhood WHERE la.car.id = :carId ORDER BY la.startInclusive ASC",
                        CarAvailability.class)
                .setParameter("carId", carId)
                .getResultList();
    }

    @Override
    public List<CarAvailability> findByCarIdsEndingOnOrAfter(final Collection<Long> carIds, final LocalDate minEndDate) {
        if (carIds == null || carIds.isEmpty()) {
            return Collections.emptyList();
        }
        final StringBuilder jpql = new StringBuilder(
                "FROM CarAvailability la LEFT JOIN FETCH la.neighborhood WHERE la.car.id IN :carIds");
        if (minEndDate != null) {
            jpql.append(" AND la.endInclusive >= :minEndDate");
        }
        final var query = em.createQuery(jpql.toString(), CarAvailability.class)
                .setParameter("carIds", carIds);
        if (minEndDate != null) {
            query.setParameter("minEndDate", minEndDate);
        }
        return query.getResultList();
    }

    @Override
    public boolean existsAnyOfferedByCar(final long carId) {
        final Number count = (Number) em.createNativeQuery(
                        "SELECT COUNT(*) FROM car_availability la "
                                + "WHERE la.car_id = :carId AND la.kind = 'offered' "
                                + "LIMIT 1")
                .setParameter("carId", carId)
                .getSingleResult();
        return count != null && count.intValue() > 0;
    }

    @Override
    @Transactional
    public void deleteByCarId(final long carId) {
        final List<CarAvailability> toRemove = em.createQuery(
                        "FROM CarAvailability la WHERE la.car.id = :carId",
                        CarAvailability.class)
                .setParameter("carId", carId)
                .getResultList();
        for (final CarAvailability la : toRemove) {
            em.remove(la);
        }
    }

    @Override
    public Map<Long, BigDecimal> findMinOfferedDayPriceByCarIds(final Collection<Long> carIds) {
        if (carIds == null || carIds.isEmpty()) {
            return Map.of();
        }
        final List<Object[]> rows = em.createQuery(
                        "SELECT la.car.id, MIN(la.dayPrice) FROM CarAvailability la "
                                + "WHERE la.car.id IN :carIds AND la.kind = :offeredKind "
                                + "GROUP BY la.car.id",
                        Object[].class)
                .setParameter("carIds", carIds)
                .setParameter("offeredKind", CarAvailability.Kind.OFFERED)
                .getResultList();
        final Map<Long, BigDecimal> result = new HashMap<>();
        for (final Object[] row : rows) {
            final long carId = ((Number) row[0]).longValue();
            final BigDecimal price = (BigDecimal) row[1];
            if (price != null) {
                result.put(carId, price);
            }
        }
        return result;
    }
}
