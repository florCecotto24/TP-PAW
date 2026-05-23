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
import ar.edu.itba.paw.models.domain.Listing;
import ar.edu.itba.paw.models.domain.ListingAvailability;
import ar.edu.itba.paw.models.domain.Neighborhood;
import ar.edu.itba.paw.persistence.ListingAvailabilityDao;

@Transactional(readOnly = true)
@Repository
public class ListingAvailabilityJpaDao implements ListingAvailabilityDao {

    @PersistenceContext
    private EntityManager em;

    @Override
    @Transactional
    public ListingAvailability create(final long listingId, final LocalDate startInclusive,
            final LocalDate endInclusive, final BigDecimal dayPrice) {
        // Load the listing managed so we can copy its pricing/location/check-in-out defaults onto the
        // availability row. After V25, listing_availability carries those values itself so each row is
        // self-contained; this transitional path keeps the legacy create(...) contract working until
        // Phase 5 rewires call sites to provide all fields explicitly.
        final Listing listing = em.find(Listing.class, listingId);
        if (listing == null) {
            throw new IllegalArgumentException("Listing not found: " + listingId);
        }
        final OffsetDateTime now = OffsetDateTime.now();
        final BigDecimal effectiveDayPrice = dayPrice != null ? dayPrice : listing.getDayPrice();
        final ListingAvailability availability = ListingAvailability.builder()
                .listing(listing)
                .startInclusive(startInclusive)
                .endInclusive(endInclusive)
                .dayPrice(effectiveDayPrice)
                .startPointStreet(listing.getStartPointStreet())
                .startPointNumber(listing.getStartPointNumber().orElse(null))
                .neighborhood(listing.getNeighborhood())
                .checkInTime(listing.getCheckInTime())
                .checkOutTime(listing.getCheckOutTime())
                .kind(ListingAvailability.Kind.OFFERED)
                .createdAt(now)
                .updatedAt(now)
                .build();
        em.persist(availability);
        return availability;
    }

    @Override
    public Optional<ListingAvailability> findById(final long availabilityId) {
        return Optional.ofNullable(em.find(ListingAvailability.class, availabilityId));
    }

    @Override
    public List<ListingAvailability> findByListingId(final long listingId) {
        return em.createQuery(
                        "FROM ListingAvailability la WHERE la.listing.id = :listingId ORDER BY la.startInclusive ASC",
                        ListingAvailability.class)
                .setParameter("listingId", listingId)
                .getResultList();
    }

    @Override
    public List<ListingAvailability> findByListingIdsEndingOnOrAfter(final Collection<Long> listingIds, final LocalDate minEndDate) {
        if (listingIds == null || listingIds.isEmpty()) {
            return Collections.emptyList();
        }
        return em.createQuery(
                        "FROM ListingAvailability la WHERE la.listing.id IN :listingIds AND la.endInclusive >= :minEndDate",
                        ListingAvailability.class)
                .setParameter("listingIds", listingIds)
                .setParameter("minEndDate", minEndDate)
                .getResultList();
    }

    @Override
    @Transactional
    public void deleteByListingId(final long listingId) {
        final List<ListingAvailability> toRemove = em.createQuery(
                        "FROM ListingAvailability la WHERE la.listing.id = :listingId",
                        ListingAvailability.class)
                .setParameter("listingId", listingId)
                .getResultList();
        for (final ListingAvailability la : toRemove) {
            em.remove(la);
        }
    }

    @Override
    public Optional<ListingAvailability> findEffectiveForDay(final long listingId, final LocalDate day) {
        final List<ListingAvailability> rows = em.createQuery(
                        "FROM ListingAvailability la WHERE la.listing.id = :listingId "
                                + "AND la.startInclusive <= :day AND la.endInclusive >= :day "
                                + "ORDER BY la.createdAt DESC, la.id DESC",
                        ListingAvailability.class)
                .setParameter("listingId", listingId)
                .setParameter("day", day)
                .setMaxResults(1)
                .getResultList();
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    @Override
    @Transactional
    public ListingAvailability createFull(
            final long listingId,
            final LocalDate startInclusive,
            final LocalDate endInclusive,
            final BigDecimal dayPrice,
            final String startPointStreet,
            final String startPointNumber,
            final Long neighborhoodId,
            final LocalTime checkInTime,
            final LocalTime checkOutTime,
            final ListingAvailability.Kind kind) {
        final Listing listingRef = em.getReference(Listing.class, listingId);
        final Neighborhood neighborhoodRef = neighborhoodId == null
                ? null
                : em.getReference(Neighborhood.class, neighborhoodId);
        final OffsetDateTime now = OffsetDateTime.now();
        final ListingAvailability availability = ListingAvailability.builder()
                .listing(listingRef)
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
    public List<ListingAvailability> findOverlappingRange(final long listingId, final LocalDate from, final LocalDate to) {
        return em.createQuery(
                        "FROM ListingAvailability la WHERE la.listing.id = :listingId "
                                + "AND la.startInclusive <= :to AND la.endInclusive >= :from "
                                + "ORDER BY la.createdAt DESC, la.id DESC",
                        ListingAvailability.class)
                .setParameter("listingId", listingId)
                .setParameter("from", from)
                .setParameter("to", to)
                .getResultList();
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
        final Car carRef =
                em.getReference(Car.class, carId);
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
