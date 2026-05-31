package ar.edu.itba.paw.persistence;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.itba.paw.models.domain.Image;
import ar.edu.itba.paw.models.domain.Reservation;
import ar.edu.itba.paw.models.domain.Review;
import ar.edu.itba.paw.models.domain.ReviewId;
import ar.edu.itba.paw.models.domain.User;
import ar.edu.itba.paw.models.dto.listing.ListingPublicReview;
import ar.edu.itba.paw.models.dto.Page;
import ar.edu.itba.paw.models.dto.profile.ReviewItemDto;
import ar.edu.itba.paw.persistence.ReviewDao;

@Transactional(readOnly = true)
@Repository
public class ReviewJpaDao implements ReviewDao {

    @PersistenceContext
    private EntityManager em;

    @Override
    public boolean existsReview(final long reservationId, final boolean madeByRider) {
        return em.find(Review.class, new ReviewId(reservationId, madeByRider)) != null;
    }

    @Override
    @Transactional
    public void insertReview(
            final long reservationId,
            final boolean madeByRider,
            final Integer rating,
            final String comment,
            final Long imageId) {
        // The Reservation is loaded (not getReference) because Review's builder derives the car
        // from it to preserve the (car_id, review) invariant in the entity itself; the car has
        // to be a usable association, not a proxy whose load would happen later. The Image, by
        // contrast, is a pure FK and stays as a proxy.
        final Reservation reservation = em.find(Reservation.class, reservationId);
        if (reservation == null) {
            return;
        }
        final Image imageRef = imageId == null ? null : em.getReference(Image.class, imageId);
        em.persist(Review.builder()
                .reservation(reservation)
                .madeByRider(madeByRider)
                .createdAt(OffsetDateTime.now())
                .rating(rating)
                .comment(comment)
                .image(imageRef)
                .build());
    }

    @Override
    @SuppressWarnings("unchecked")
    public Page<ListingPublicReview> findCarPublicReviews(final long carId, final int page, final int pageSize) {
        final Long total = em.createQuery(
                        "SELECT COUNT(r) FROM Review r "
                                + "WHERE r.reservation.car.id = :carId AND r.rating IS NOT NULL",
                        Long.class)
                .setParameter("carId", carId)
                .getSingleResult();

        final List<Object[]> pkRows = em.createNativeQuery(
                        "SELECT r.reservation_id, r.made_by_rider "
                                + "FROM reviews r "
                                + "JOIN reservations res ON res.id = r.reservation_id "
                                + "WHERE res.car_id = :carId AND r.rating IS NOT NULL "
                                + "ORDER BY r.created_at DESC "
                                + "LIMIT :limit OFFSET :offset")
                .setParameter("carId", carId)
                .setParameter("limit", pageSize)
                .setParameter("offset", page * pageSize)
                .getResultList();

        if (pkRows.isEmpty()) {
            return new Page<>(Collections.emptyList(), page, pageSize, total != null ? total : 0L);
        }

        final List<ReviewId> ids = pkRows.stream()
                .map(row -> new ReviewId(((Number) row[0]).longValue(), Boolean.TRUE.equals(row[1])))
                .collect(Collectors.toList());
        final List<Review> reviews = em.createQuery(
                        "FROM Review r "
                                + "JOIN FETCH r.reservation res "
                                + "JOIN FETCH res.rider rider "
                                + "JOIN FETCH res.car c "
                                + "JOIN FETCH c.owner owner "
                                + "LEFT JOIN FETCH r.image img "
                                + "WHERE r.id IN :ids "
                                + "ORDER BY r.createdAt DESC",
                        Review.class)
                .setParameter("ids", ids)
                .getResultList();

        final List<ListingPublicReview> content = reviews.stream()
                .map(r -> {
                    final User reviewer = r.getId().isMadeByRider()
                            ? r.getReservation().getRider()
                            : r.getReservation().getCar().getOwner();
                    return new ListingPublicReview(
                            reviewer.getForename(),
                            reviewer.getSurname(),
                            r.getCreatedAt(),
                            r.getRating().orElse(0),
                            r.getComment().orElse(null),
                            r.getImage().map(Image::getId).orElse(null));
                })
                .collect(Collectors.toList());
        return new Page<>(content, page, pageSize, total != null ? total : 0L);
    }

    @Override
    public long countReviewsForCar(final long carId) {
        return em.createQuery(
                        "SELECT COUNT(r) FROM Review r "
                                + "WHERE r.reservation.car.id = :carId AND r.rating IS NOT NULL",
                        Long.class)
                .setParameter("carId", carId)
                .getSingleResult();
    }

    @Override
    public BigDecimal findAverageRatingForCar(final long carId) {
        final Double avg = em.createQuery(
                        "SELECT AVG(r.rating) FROM Review r "
                                + "WHERE r.reservation.car.id = :carId AND r.rating IS NOT NULL",
                        Double.class)
                .setParameter("carId", carId)
                .getSingleResult();
        return round2(avg);
    }

    @Override
    public BigDecimal findAverageRatingForCounterparty(final long counterpartyUserId, final boolean counterpartyIsOwner) {
        final String jpql = counterpartyIsOwner
                ? "SELECT AVG(r.rating) FROM Review r "
                        + "WHERE r.id.madeByRider = true AND r.reservation.car.owner.id = :userId "
                        + "AND r.rating IS NOT NULL"
                : "SELECT AVG(r.rating) FROM Review r "
                        + "WHERE r.id.madeByRider = false AND r.reservation.rider.id = :userId "
                        + "AND r.rating IS NOT NULL";
        final Double avg = em.createQuery(jpql, Double.class)
                .setParameter("userId", counterpartyUserId)
                .getSingleResult();
        return round2(avg);
    }

    @Override
    public List<ReviewItemDto> findRecentCommentReviewsForCounterparty(
            final long counterpartyUserId,
            final boolean counterpartyIsOwner,
            final int limit) {
        /*
         * Two JPQL branches to avoid a CASE WHEN over different association paths: in each branch
         * the reviewer side (rider vs owner) is statically known, so we JOIN FETCH only that side
         * plus its (nullable) profilePicture. Mapping to {@link ReviewItemDto} happens in code
         * because {@code LocalDate} is not derivable from {@code OffsetDateTime} inside a JPQL
         * constructor expression on every provider.
         *
         * Note on LEFT JOIN FETCH on profilePicture: navigating {@code rider.profilePicture.id}
         * directly in JPQL would generate an implicit INNER JOIN and silently drop reviews whose
         * reviewer has no profile picture (the association is nullable). The explicit LEFT JOIN
         * FETCH preserves those rows AND eagerly hydrates the image so the mapping loop does not
         * trigger a per-row N+1.
         *
         * Joins are all *-to-one (rider, car, owner, profilePicture), so {@code setMaxResults}
         * does not collide with the JOIN FETCH multiplicity limitation.
         */
        final String jpql;
        if (counterpartyIsOwner) {
            // counterparty is the owner -> reviews written by riders -> reviewer = rider
            jpql = "FROM Review r "
                    + "JOIN FETCH r.reservation res "
                    + "JOIN FETCH res.rider rider "
                    + "LEFT JOIN FETCH rider.profilePicture "
                    + "JOIN res.car c "
                    + "JOIN c.owner owner "
                    + "WHERE r.id.madeByRider = TRUE AND owner.id = :userId "
                    + "AND r.rating IS NOT NULL AND TRIM(r.comment) <> '' "
                    + "ORDER BY r.createdAt DESC";
        } else {
            // counterparty is the rider -> reviews written by owners -> reviewer = owner
            jpql = "FROM Review r "
                    + "JOIN FETCH r.reservation res "
                    + "JOIN res.rider rider "
                    + "JOIN FETCH res.car c "
                    + "JOIN FETCH c.owner owner "
                    + "LEFT JOIN FETCH owner.profilePicture "
                    + "WHERE r.id.madeByRider = FALSE AND rider.id = :userId "
                    + "AND r.rating IS NOT NULL AND TRIM(r.comment) <> '' "
                    + "ORDER BY r.createdAt DESC";
        }
        final List<Review> reviews = em.createQuery(jpql, Review.class)
                .setParameter("userId", counterpartyUserId)
                .setMaxResults(limit)
                .getResultList();

        final List<ReviewItemDto> result = new ArrayList<>(reviews.size());
        for (final Review r : reviews) {
            final User reviewer = r.getId().isMadeByRider()
                    ? r.getReservation().getRider()
                    : r.getReservation().getCar().getOwner();
            result.add(new ReviewItemDto(
                    reviewer.getId(),
                    reviewer.getForename() + " " + reviewer.getSurname(),
                    reviewer.getProfilePicture().map(Image::getId).orElse(null),
                    r.getRating().orElse(0),
                    r.getCreatedAt().toLocalDate(),
                    r.getComment().orElse(null)));
        }
        return result;
    }

    private static BigDecimal round2(final Double avg) {
        return avg == null ? null : BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP);
    }
}
