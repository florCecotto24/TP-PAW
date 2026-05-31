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

import ar.edu.itba.paw.models.domain.AvailabilityPeriod;
import ar.edu.itba.paw.models.domain.ListingAvailability;
import ar.edu.itba.paw.models.domain.Reservation;
import ar.edu.itba.paw.models.domain.ReservationAvailabilityCoverage;
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
            final ListingAvailability availabilityRef =
                    em.getReference(ListingAvailability.class, availabilityId);
            em.persist(new ReservationAvailabilityCoverage(reservationRef, availabilityRef));
        }
    }

    @Override
    public Optional<BigDecimal> sumReservationTotal(final long reservationId) {
        final Reservation reservation = em.find(Reservation.class, reservationId);
        if (reservation == null) {
            return Optional.empty();
        }
        final List<ReservationAvailabilityCoverage> coverages = em.createQuery(
                        "FROM ReservationAvailabilityCoverage ra "
                                + "JOIN FETCH ra.availability "
                                + "WHERE ra.reservation.id = :reservationId",
                        ReservationAvailabilityCoverage.class)
                .setParameter("reservationId", reservationId)
                .getResultList();
        if (coverages.isEmpty()) {
            return Optional.empty();
        }
        final List<ListingAvailability> candidates = coverages.stream()
                .map(ReservationAvailabilityCoverage::getAvailability)
                .toList();
        final LocalDate firstDay = reservation.getStartDate()
                .atZoneSameInstant(AvailabilityPeriod.WALL_ZONE).toLocalDate();
        final LocalDate lastDay = reservation.getEndDate()
                .atZoneSameInstant(AvailabilityPeriod.WALL_ZONE).toLocalDate();
        return sumDayPricesByEffectiveCandidate(candidates, firstDay, lastDay);
    }

    /**
     * For each day in {@code [firstDay, lastDay]}, picks the candidate availability whose date range
     * covers the day with the latest {@code createdAt} (ties broken by id) and accumulates its
     * {@code day_price}. Returns empty when any day lacks coverage among the candidates.
     */
    static Optional<BigDecimal> sumDayPricesByEffectiveCandidate(
            final List<ListingAvailability> candidates,
            final LocalDate firstDay,
            final LocalDate lastDay) {
        BigDecimal total = BigDecimal.ZERO;
        for (LocalDate day = firstDay; !day.isAfter(lastDay); day = day.plusDays(1)) {
            final LocalDate currentDay = day;
            final Optional<ListingAvailability> winner = candidates.stream()
                    .filter(a -> a.getKind() == ListingAvailability.Kind.OFFERED)
                    .filter(a -> !currentDay.isBefore(a.getStartInclusive())
                            && !currentDay.isAfter(a.getEndInclusive()))
                    .max(Comparator
                            .comparing(ListingAvailability::getCreatedAt)
                            .thenComparing(ListingAvailability::getId));
            if (winner.isEmpty()) {
                return Optional.empty();
            }
            total = total.add(winner.get().getDayPriceValue());
        }
        return Optional.of(total);
    }
}
