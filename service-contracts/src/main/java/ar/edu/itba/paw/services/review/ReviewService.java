package ar.edu.itba.paw.services.review;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import ar.edu.itba.paw.models.domain.review.Review;
import ar.edu.itba.paw.models.dto.car.CarPublicReview;
import ar.edu.itba.paw.models.dto.Page;
import ar.edu.itba.paw.models.dto.profile.ReviewItemDto;

import ar.edu.itba.paw.services.file.ImageService;
import ar.edu.itba.paw.services.reservation.ReservationService;
import ar.edu.itba.paw.services.user.UserService;
/**
 * Post-rental ratings and comments tied to reservations (owner↔rider).
 * Implementations use {@code ReviewDao} only; reservation and owner rows are resolved through {@code ReservationService}
 * and {@code UserService}.
 */
public interface ReviewService {

    /** Maximum trimmed length for review comment text (configuration-backed). */
    int getReviewCommentMaxLength();

    /** Public car page: paginated reviews with reviewer display fields. */
    Page<CarPublicReview> getCarPublicReviews(long carId, int page, int pageSize);

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

    /** Total public reviews stored for the car. */
    long countReviewsForCar(long carId);

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

    /** Whether the owner already left a review for this reservation. */
    boolean hasOwnerReview(long reservationId);

    /** Whether the rider already left a review for this reservation. */
    boolean hasRiderReview(long reservationId);

    /**
     * Average rating (1–5 scale) for the given user as owner or rider, from reviews where they were rated
     * ({@code counterpartyIsOwner} selects which side of the reservation is the rated party).
     */
    BigDecimal getAverageRatingForCounterparty(long counterpartyUserId, boolean counterpartyIsOwner);

    /**
     * Recent rated reviews for the counterparty profile snippet. Comment-less reviews are included
     * (their {@link ReviewItemDto#getCommentText()} is {@code null}) so the JSP can render a
     * "no comment" placeholder for them.
     */
    List<ReviewItemDto> getRecentReviewsForCounterparty(long counterpartyUserId, boolean counterpartyIsOwner, int limit);

    /** Total rated reviews stored against the counterparty (owner side or rider side). */
    long countReviewsForCounterparty(long counterpartyUserId, boolean counterpartyIsOwner);

    /**
     * SQL-paginated feed of every rated review left to {@code userId} (both as owner and as
     * rider), ordered by date desc. Backs {@code GET /reviews?recipientUserId=…}.
     */
    Page<ReviewItemDto> getReviewsForUser(long userId, int page, int pageSize);
}
