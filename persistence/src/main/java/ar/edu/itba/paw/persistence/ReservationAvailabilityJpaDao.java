package ar.edu.itba.paw.persistence;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.itba.paw.models.domain.CarAvailability;
import ar.edu.itba.paw.models.domain.Reservation;
import ar.edu.itba.paw.models.domain.ReservationAvailabilityCoverage;
import ar.edu.itba.paw.models.util.time.AppTimezone;
import ar.edu.itba.paw.persistence.ReservationAvailabilityDao;

@Transactional(readOnly = true)
@Repository
public class ReservationAvailabilityJpaDao implements ReservationAvailabilityDao {

    @PersistenceContext
    private EntityManager em;

    @Override
    @Transactional
    public void insertCoveringAvailabilities(
            final long reservationId, final Collection<Long> availabilityIds) {
        if (availabilityIds == null || availabilityIds.isEmpty()) {
            return;
        }
        final Reservation reservationRef = em.getReference(Reservation.class, reservationId);
        final Set<Long> uniqueIds = new LinkedHashSet<>(availabilityIds);
        for (final long availabilityId : uniqueIds) {
            final CarAvailability availabilityRef =
                    em.getReference(CarAvailability.class, availabilityId);
            em.persist(new ReservationAvailabilityCoverage(reservationRef, availabilityRef));
        }
    }

    @Override
    @Transactional
    public void deleteCoveringAvailabilities(final long reservationId) {
        // The bridge table is owned by ReservationAvailabilityCoverage exclusively (no FK fan-out into
        // other domain rows), so a single JPQL DELETE is the natural way to clear it for a reservation.
        // The "dirty-checking preferred" rule (AGENTS.md) targets entity tables with rich navigation;
        // for a join-only bridge with no associations to flush we prefer the bulk DELETE.
        em.createQuery(
                        "DELETE FROM ReservationAvailabilityCoverage ra "
                                + "WHERE ra.reservation.id = :reservationId")
                .setParameter("reservationId", reservationId)
                .executeUpdate();
    }

    @Override
    public Optional<BigDecimal> sumReservationTotal(final long reservationId) {
        final List<ReservationAvailabilityCoverage> coverages = loadCoveragesForReservation(reservationId);
        if (coverages.isEmpty()) {
            return Optional.empty();
        }
        final Reservation reservation = coverages.get(0).getReservation();
        final List<CarAvailability> candidates = coverages.stream()
                .map(ReservationAvailabilityCoverage::getAvailability)
                .toList();
        final LocalDate firstDay = reservation.getStartDate()
                .atZoneSameInstant(AppTimezone.WALL_ZONE).toLocalDate();
        final LocalDate lastDay = reservation.getEndDate()
                .atZoneSameInstant(AppTimezone.WALL_ZONE).toLocalDate();
        return sumDayPricesByEffectiveCandidate(candidates, firstDay, lastDay);
    }

    @Override
    public Optional<CarAvailability> findEffectivePickupAvailabilityForReservation(final long reservationId) {
        final List<ReservationAvailabilityCoverage> coverages = loadCoveragesForReservation(reservationId);
        if (coverages.isEmpty()) {
            return Optional.empty();
        }
        final Reservation reservation = coverages.get(0).getReservation();
        final List<CarAvailability> candidates = coverages.stream()
                .map(ReservationAvailabilityCoverage::getAvailability)
                .toList();
        final LocalDate firstDay = reservation.getStartDate()
                .atZoneSameInstant(AppTimezone.WALL_ZONE).toLocalDate();
        return pickEffectiveOfferedForDay(candidates, firstDay);
    }

    /**
     * Single JPQL with JOIN FETCH hydrates both the reservation (for the date range) and its
     * covering availabilities (for per-day pricing / pickup snapshot resolution); no extra em.find
     * on a sibling DAO entity.
     */
    private List<ReservationAvailabilityCoverage> loadCoveragesForReservation(final long reservationId) {
        return em.createQuery(
                        "FROM ReservationAvailabilityCoverage ra "
                                + "JOIN FETCH ra.availability "
                                + "JOIN FETCH ra.reservation "
                                + "WHERE ra.reservation.id = :reservationId",
                        ReservationAvailabilityCoverage.class)
                .setParameter("reservationId", reservationId)
                .getResultList();
    }

    /**
     * For each day in {@code [firstDay, lastDay]}, picks the candidate availability whose date range
     * covers the day with the latest {@code createdAt} (ties broken by id) and accumulates its
     * {@code day_price}. Returns empty when any day lacks coverage among the candidates.
     */
    static Optional<BigDecimal> sumDayPricesByEffectiveCandidate(
            final List<CarAvailability> candidates,
            final LocalDate firstDay,
            final LocalDate lastDay) {
        BigDecimal total = BigDecimal.ZERO;
        for (LocalDate day = firstDay; !day.isAfter(lastDay); day = day.plusDays(1)) {
            final Optional<CarAvailability> winner = pickEffectiveOfferedForDay(candidates, day);
            if (winner.isEmpty()) {
                return Optional.empty();
            }
            total = total.add(winner.get().getDayPriceValue());
        }
        return Optional.of(total);
    }

    /**
     * Picks the OFFERED candidate whose range covers {@code day} with the latest {@code createdAt}
     * (ties broken by id). WITHDRAWN candidates are ignored — the bridge only stores rows that were
     * pricing-relevant at booking time, but a WITHDRAWN row that was inserted on top of the bridged
     * candidates should not flip the day to "no pickup info available" for an already-booked
     * reservation: the rule reflects the same semantics as the price sum.
     */
    static Optional<CarAvailability> pickEffectiveOfferedForDay(
            final List<CarAvailability> candidates, final LocalDate day) {
        return candidates.stream()
                .filter(a -> a.getKind() == CarAvailability.Kind.OFFERED)
                .filter(a -> !day.isBefore(a.getStartInclusive())
                        && !day.isAfter(a.getEndInclusive()))
                .max(Comparator
                        .comparing(CarAvailability::getCreatedAt)
                        .thenComparing(CarAvailability::getId));
    }
}
