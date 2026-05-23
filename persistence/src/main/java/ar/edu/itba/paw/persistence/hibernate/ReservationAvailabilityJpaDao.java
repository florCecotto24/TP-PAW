package ar.edu.itba.paw.persistence.hibernate;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.itba.paw.models.domain.ListingAvailability;
import ar.edu.itba.paw.models.domain.Reservation;
import ar.edu.itba.paw.models.domain.ReservationAvailabilityCoverage;
import ar.edu.itba.paw.models.domain.ReservationAvailabilityLink;
import ar.edu.itba.paw.persistence.ReservationAvailabilityDao;

@Transactional(readOnly = true)
@Repository
public class ReservationAvailabilityJpaDao implements ReservationAvailabilityDao {

    private static final String CHUNK_TOTAL_JPQL =
            "SELECT la.dayPrice * :coveredDays FROM ListingAvailability la WHERE la.id = :availabilityId";

    @PersistenceContext
    private EntityManager em;

    @Override
    @Transactional
    public void insertLinks(final long reservationId, final List<ReservationAvailabilityLink> links) {
        if (links == null || links.isEmpty()) {
            return;
        }
        final Reservation reservationRef = em.getReference(Reservation.class, reservationId);
        for (final ReservationAvailabilityLink link : links) {
            final ListingAvailability availabilityRef =
                    em.getReference(ListingAvailability.class, link.getAvailabilityId());
            em.persist(new ReservationAvailabilityCoverage(
                    reservationRef,
                    availabilityRef,
                    link.getCoveredStartDate(),
                    link.getCoveredEndDate()));
        }
    }

    @Override
    public Optional<BigDecimal> sumReservationTotal(final long reservationId) {
        final List<ReservationAvailabilityCoverage> coverages = em.createQuery(
                        "FROM ReservationAvailabilityCoverage ra WHERE ra.reservation.id = :reservationId",
                        ReservationAvailabilityCoverage.class)
                .setParameter("reservationId", reservationId)
                .getResultList();
        return sumChunkTotals(coverages.stream()
                .map(row -> new ChunkSpec(row.getId().getAvailabilityId(), row.coveredDaysInclusive()))
                .toList());
    }

    @Override
    public Optional<BigDecimal> quoteTotalFromLinks(final List<ReservationAvailabilityLink> links) {
        if (links == null || links.isEmpty()) {
            return Optional.empty();
        }
        return sumChunkTotals(links.stream()
                .map(link -> new ChunkSpec(
                        link.getAvailabilityId(),
                        ChronoUnit.DAYS.between(
                                link.getCoveredStartDate(), link.getCoveredEndDate().plusDays(1))))
                .toList());
    }

    private Optional<BigDecimal> sumChunkTotals(final List<ChunkSpec> chunks) {
        BigDecimal total = BigDecimal.ZERO;
        for (final ChunkSpec chunk : chunks) {
            final BigDecimal chunkTotal = em.createQuery(CHUNK_TOTAL_JPQL, BigDecimal.class)
                    .setParameter("availabilityId", chunk.availabilityId())
                    .setParameter("coveredDays", BigDecimal.valueOf(chunk.coveredDays()))
                    .getSingleResult();
            if (chunkTotal == null) {
                return Optional.empty();
            }
            total = total.add(chunkTotal);
        }
        return Optional.of(total);
    }

    private record ChunkSpec(long availabilityId, long coveredDays) {
    }
}
