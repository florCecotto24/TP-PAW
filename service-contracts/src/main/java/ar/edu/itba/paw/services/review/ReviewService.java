package ar.edu.itba.paw.services.review;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import ar.edu.itba.paw.models.domain.review.Review;
import ar.edu.itba.paw.models.dto.Page;

import ar.edu.itba.paw.services.file.ImageService;
import ar.edu.itba.paw.services.reservation.ReservationService;
import ar.edu.itba.paw.services.user.UserService;
/**
 * Post-rental ratings and comments tied to reservations (owner↔rider).
 * Implementations use {@code ReviewDao} only; reservation and owner rows are resolved through {@code ReservationService}
 * and {@code UserService}.
 */
public interface ReviewService {

    /** Hydrated review entities for REST {@code GET /reviews?carId=…}. */
    Page<Review> getCarPublicReviewEntities(long carId, int page, int pageSize);

    /**
     * SQL-paginated reviews received by {@code userId} (owner and rider directions merged),
     * as hydrated entities for REST {@code GET /reviews?recipientUserId=}.
     */
    Page<Review> getReviewsReceivedByUserEntities(long userId, int page, int pageSize);

    /** Reviews attached to a single reservation (participants/admin). */
    List<Review> getReviewsForReservation(long reservationId);

    /** Single review by its own surrogate id, backing {@code GET /reviews/{id}}. */
    Optional<Review> getReviewById(long reviewId);

    /**
     * Owner rates the rider after a completed rental; enforces reservation state and one review per side.
     *
     * @throws ar.edu.itba.paw.exception.RydenException when validation or business rules fail
     */
    default void submitOwnerReviewOfRider(long ownerUserId, long reservationId, Integer rating, String comment) {
        submitOwnerReviewOfRider(ownerUserId, reservationId, rating, comment, null, null, null);
    }

    /**
     * Owner rates the rider after a completed rental with an optional attached image.
     * Pass {@code null} for {@code imageBytes} to skip the image; otherwise the service validates content type and
     * size via {@link ImageService} and persists the {@link ar.edu.itba.paw.models.domain.file.Image} together with the
     * {@link ar.edu.itba.paw.models.domain.review.Review} (cascade-persisted from the Review aggregate root).
     *
     * @throws ar.edu.itba.paw.exception.RydenException when validation or business rules fail
     */
    void submitOwnerReviewOfRider(long ownerUserId, long reservationId, Integer rating, String comment,
                                   String imageName, String imageContentType, byte[] imageBytes);

    /**
     * Rider rates the owner after a completed rental; enforces reservation state and one review per side.
     *
     * @throws ar.edu.itba.paw.exception.RydenException when validation or business rules fail
     */
    default void submitRiderReviewOfOwner(long riderUserId, long reservationId, Integer rating, String comment) {
        submitRiderReviewOfOwner(riderUserId, reservationId, rating, comment, null, null, null);
    }

    /**
     * Rider rates the owner after a completed rental with an optional attached image. See
     * {@link #submitOwnerReviewOfRider(long, long, Integer, String, String, String, byte[])} for the image semantics.
     *
     * @throws ar.edu.itba.paw.exception.RydenException when validation or business rules fail
     */
    void submitRiderReviewOfOwner(long riderUserId, long reservationId, Integer rating, String comment,
                                   String imageName, String imageContentType, byte[] imageBytes);

    /**
     * Submits the caller's review for a reservation (owner→rider or rider→owner) and returns the
     * persisted row for the participant's side.
     *
     * @throws ar.edu.itba.paw.exception.RydenException when the caller is not a participant or rules fail
     */
    Review submitParticipantReview(
            long actorUserId,
            long reservationId,
            Integer rating,
            String comment,
            String imageName,
            String imageContentType,
            byte[] imageBytes);

    /**
     * Average rating (1–5 scale) for the given user as owner or rider, from reviews where they were rated
     * ({@code counterpartyIsOwner} selects which side of the reservation is the rated party).
     */
    BigDecimal getAverageRatingForCounterparty(long counterpartyUserId, boolean counterpartyIsOwner);

    /** Batch owner-side ratings for admin user listings. Missing keys mean no rated reviews. */
    Map<Long, BigDecimal> getAverageRatingsAsOwnerForUserIds(Collection<Long> userIds);

    /** Batch rider-side ratings for admin user listings. Missing keys mean no rated reviews. */
    Map<Long, BigDecimal> getAverageRatingsAsRiderForUserIds(Collection<Long> userIds);
}
