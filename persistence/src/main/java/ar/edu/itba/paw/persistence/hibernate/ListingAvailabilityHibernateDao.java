package ar.edu.itba.paw.persistence.hibernate;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.itba.paw.models.domain.ListingAvailability;
import ar.edu.itba.paw.persistence.ListingAvailabilityDao;

@Transactional
@Repository
public class ListingAvailabilityHibernateDao implements ListingAvailabilityDao {

    @PersistenceContext
    private EntityManager em;

    @Override
    public ListingAvailability create(final long listingId, final LocalDate startInclusive, final LocalDate endInclusive) {
        final OffsetDateTime now = OffsetDateTime.now();
        final ListingAvailability availability = new ListingAvailability(listingId, startInclusive, endInclusive, now, now);
        em.persist(availability);
        return availability;
    }

    @Override
    public List<ListingAvailability> findByListingId(final long listingId) {
        return em.createQuery(
                        "FROM ListingAvailability la WHERE la.listingId = :listingId ORDER BY la.startInclusive ASC",
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
                        "FROM ListingAvailability la WHERE la.listingId IN :listingIds AND la.endInclusive >= :minEndDate",
                        ListingAvailability.class)
                .setParameter("listingIds", listingIds)
                .setParameter("minEndDate", minEndDate)
                .getResultList();
    }

    @Override
    public void deleteByListingId(final long listingId) {
        em.createQuery("DELETE FROM ListingAvailability la WHERE la.listingId = :listingId")
                .setParameter("listingId", listingId)
                .executeUpdate();
    }
}
