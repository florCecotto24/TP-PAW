package ar.edu.itba.paw.persistence.hibernate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.itba.paw.models.domain.Car;
import ar.edu.itba.paw.models.domain.ListingAvailability;
import ar.edu.itba.paw.models.domain.Neighborhood;
import ar.edu.itba.paw.persistence.ListingAvailabilityDao;

@Transactional(readOnly = true)
@Repository
public class ListingAvailabilityJpaDao implements ListingAvailabilityDao {

    @PersistenceContext
    private EntityManager em;

    @Override
    public Optional<ListingAvailability> findById(final long availabilityId) {
        return Optional.ofNullable(em.find(ListingAvailability.class, availabilityId));
    }

    @Override
    public Optional<ListingAvailability> findEffectiveForDayByCar(final long carId, final LocalDate day) {
        final List<ListingAvailability> rows = em.createQuery(
                        "FROM ListingAvailability la WHERE la.car.id = :carId "
                                + "AND la.startInclusive <= :day AND la.endInclusive >= :day "
                                + "ORDER BY la.createdAt DESC, la.id DESC",
                        ListingAvailability.class)
                .setParameter("carId", carId)
                .setParameter("day", day)
                .setMaxResults(1)
                .getResultList();
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    @Override
    public List<ListingAvailability> findOverlappingRangeByCar(final long carId, final LocalDate from, final LocalDate to) {
        return em.createQuery(
                        "FROM ListingAvailability la WHERE la.car.id = :carId "
                                + "AND la.startInclusive <= :to AND la.endInclusive >= :from "
                                + "ORDER BY la.createdAt DESC, la.id DESC",
                        ListingAvailability.class)
                .setParameter("carId", carId)
                .setParameter("from", from)
                .setParameter("to", to)
                .getResultList();
    }

    @Override
    @Transactional
    public ListingAvailability createFullForCar(
            final long carId,
            final LocalDate startInclusive,
            final LocalDate endInclusive,
            final BigDecimal dayPrice,
            final String startPointStreet,
            final String startPointNumber,
            final Long neighborhoodId,
            final LocalTime checkInTime,
            final LocalTime checkOutTime,
            final ListingAvailability.Kind kind) {
        final Car carRef = em.getReference(Car.class, carId);
        final Neighborhood neighborhoodRef = neighborhoodId == null
                ? null
                : em.getReference(Neighborhood.class, neighborhoodId);
        final OffsetDateTime now = OffsetDateTime.now();
        final ListingAvailability availability = ListingAvailability.builder()
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
    public List<ListingAvailability> findByCarId(final long carId) {
        return em.createQuery(
                        "FROM ListingAvailability la WHERE la.car.id = :carId ORDER BY la.startInclusive ASC",
                        ListingAvailability.class)
                .setParameter("carId", carId)
                .getResultList();
    }

    @Override
    public List<ListingAvailability> findByCarIdsEndingOnOrAfter(final Collection<Long> carIds, final LocalDate minEndDate) {
        if (carIds == null || carIds.isEmpty()) {
            return Collections.emptyList();
        }
        return em.createQuery(
                        "FROM ListingAvailability la WHERE la.car.id IN :carIds AND la.endInclusive >= :minEndDate",
                        ListingAvailability.class)
                .setParameter("carIds", carIds)
                .setParameter("minEndDate", minEndDate)
                .getResultList();
    }

    @Override
    @Transactional
    public void deleteByCarId(final long carId) {
        final List<ListingAvailability> toRemove = em.createQuery(
                        "FROM ListingAvailability la WHERE la.car.id = :carId",
                        ListingAvailability.class)
                .setParameter("carId", carId)
                .getResultList();
        for (final ListingAvailability la : toRemove) {
            em.remove(la);
        }
    }
}
