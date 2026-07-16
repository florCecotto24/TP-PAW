package ar.edu.itba.paw.services.review;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.itba.paw.exception.MessageKeys;
import ar.edu.itba.paw.exception.car.CarNotFoundException;
import ar.edu.itba.paw.exception.reservation.RiderReservationException;
import ar.edu.itba.paw.exception.user.UserNotFoundException;
import ar.edu.itba.paw.models.domain.review.Review;
import ar.edu.itba.paw.models.dto.car.CarPublicReview;
import ar.edu.itba.paw.models.dto.Page;
import ar.edu.itba.paw.models.dto.profile.ReviewItemDto;
import ar.edu.itba.paw.models.domain.car.Car;
import ar.edu.itba.paw.models.domain.file.Image;
import ar.edu.itba.paw.models.domain.reservation.Reservation;
import ar.edu.itba.paw.policy.ReservationTimingPolicy;
import ar.edu.itba.paw.policy.ReviewValidationPolicy;
import ar.edu.itba.paw.persistence.review.ReviewDao;

import ar.edu.itba.paw.services.car.CarService;
import ar.edu.itba.paw.services.file.ImageService;
import ar.edu.itba.paw.services.reservation.ReservationService;
import ar.edu.itba.paw.services.user.UserService;
/**
 * Uses only {@link ReviewDao} for review rows; mutations on {@code users.rating_as_*} and
 * {@code cars.rating_avg} are delegated to {@link UserService} and {@link CarService} so each
 * entity stays owned by its respective DAO.
 */
@Service
public class ReviewServiceImpl implements ReviewService {

    private final ReviewDao reviewDao;
    private final ReservationService reservationService;
    private final CarService carService;
    private final UserService userService;
    private final ImageService imageService;
    private final ReviewValidationPolicy reviewValidationPolicy;
    private final ReservationTimingPolicy reservationTimingPolicy;

    @Autowired
    public ReviewServiceImpl(
            final ReviewDao reviewDao,
            final ReservationService reservationService,
            final CarService carService,
            final UserService userService,
            final ImageService imageService,
            final ReviewValidationPolicy reviewValidationPolicy,
            final ReservationTimingPolicy reservationTimingPolicy) {
        this.reviewDao = reviewDao;
        this.reservationService = reservationService;
        this.carService = carService;
        this.userService = userService;
        this.imageService = imageService;
        this.reviewValidationPolicy = reviewValidationPolicy;
        this.reservationTimingPolicy = reservationTimingPolicy;
    }

    @Override
    @Transactional(readOnly = true)
    public int getReviewCommentMaxLength() {
        return reviewValidationPolicy.getCommentMaxLength();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CarPublicReview> getCarPublicReviews(final long carId, final int page, final int pageSize) {
        return reviewDao.findCarPublicReviews(carId, page, pageSize);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Review> getCarPublicReviewEntities(final long carId, final int page, final int pageSize) {
        carService.getCarById(carId).orElseThrow(() -> new CarNotFoundException(carId));
        return reviewDao.findPublicReviewsForCar(carId, page, pageSize);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Review> getReviewsForReservation(final long reservationId) {
        return reviewDao.findReviewsForReservation(reservationId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Review> getReviewById(final long reviewId) {
        return reviewDao.findById(reviewId);
    }

    @Override
    @Transactional(readOnly = true)
    public long countReviewsForCar(final long carId) {
        return reviewDao.countReviewsForCar(carId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasOwnerReview(final long reservationId) {
        return reviewDao.existsReview(reservationId, false);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasRiderReview(final long reservationId) {
        return reviewDao.existsReview(reservationId, true);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getAverageRatingForCounterparty(final long counterpartyUserId, final boolean counterpartyIsOwner) {
        return reviewDao.findAverageRatingForCounterparty(counterpartyUserId, counterpartyIsOwner);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, BigDecimal> getAverageRatingsAsOwnerForUserIds(final Collection<Long> userIds) {
        return reviewDao.findAverageRatingsAsOwnerForUserIds(userIds);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, BigDecimal> getAverageRatingsAsRiderForUserIds(final Collection<Long> userIds) {
        return reviewDao.findAverageRatingsAsRiderForUserIds(userIds);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewItemDto> getRecentReviewsForCounterparty(
            final long counterpartyUserId,
            final boolean counterpartyIsOwner,
            final int limit) {
        return reviewDao.findRecentReviewsForCounterparty(counterpartyUserId, counterpartyIsOwner, limit);
    }

    @Override
    @Transactional(readOnly = true)
    public long countReviewsForCounterparty(final long counterpartyUserId, final boolean counterpartyIsOwner) {
        return reviewDao.countReviewsForCounterparty(counterpartyUserId, counterpartyIsOwner);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReviewItemDto> getReviewsForUser(final long userId, final int page, final int pageSize) {
        return reviewDao.findReviewsForUserPage(userId, page, pageSize);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Review> getReviewsReceivedByUserEntities(
            final long userId, final int page, final int pageSize) {
        userService.getUserById(userId)
                .orElseThrow(() -> new UserNotFoundException(MessageKeys.USER_ACCOUNT_NOT_FOUND));
        return reviewDao.findReviewsReceivedByUser(userId, page, pageSize);
    }

    @Override
    @Transactional
    public Review submitParticipantReview(
            final long actorUserId,
            final long reservationId,
            final Integer rating,
            final String comment,
            final String imageName,
            final String imageContentType,
            final byte[] imageBytes) {
        if (reservationService.getOwnerReservationById(actorUserId, reservationId).isPresent()) {
            submitOwnerReviewOfRider(
                    actorUserId, reservationId, rating, comment, imageName, imageContentType, imageBytes);
            return requireParticipantReview(reservationId, false);
        }
        if (reservationService.getRiderReservationById(actorUserId, reservationId).isPresent()) {
            submitRiderReviewOfOwner(
                    actorUserId, reservationId, rating, comment, imageName, imageContentType, imageBytes);
            return requireParticipantReview(reservationId, true);
        }
        throw new RiderReservationException(MessageKeys.REVIEW_NOT_ALLOWED);
    }

    private Review requireParticipantReview(final long reservationId, final boolean madeByRider) {
        return getReviewsForReservation(reservationId).stream()
                .filter(review -> review.isMadeByRider() == madeByRider)
                .findFirst()
                .orElseThrow(() -> new RiderReservationException(MessageKeys.REVIEW_NOT_ALLOWED));
    }

    @Override
    @Transactional
    public void submitOwnerReviewOfRider(
            final long ownerUserId,
            final long reservationId,
            final Integer rating,
            final String comment,
            final String imageName,
            final String imageContentType,
            final byte[] imageBytes) {
        final String trimmed = comment == null ? "" : comment.trim();
        if (rating == null) {
            if (!trimmed.isEmpty()) {
                throw new RiderReservationException(MessageKeys.REVIEW_RATING_REQUIRED_WHEN_COMMENT);
            }
            final Reservation r = reservationService.getOwnerReservationById(ownerUserId, reservationId)
                    .orElseThrow(() -> new RiderReservationException(MessageKeys.REVIEW_NOT_ALLOWED));
            if (!r.isCarReturned()) {
                throw new RiderReservationException(MessageKeys.REVIEW_NOT_ALLOWED);
            }
            if (reviewDao.existsReview(reservationId, false)) {
                throw new RiderReservationException(MessageKeys.REVIEW_ALREADY_SUBMITTED);
            }
            insertReviewCatchingDuplicate(reservationId, false, null, null, null);
            refreshAggregatesAfterOwnerReview(r);
            return;
        }

        final int maxCommentLength = reviewValidationPolicy.getCommentMaxLength();
        if (trimmed.length() > maxCommentLength) {
            throw new RiderReservationException(MessageKeys.REVIEW_COMMENT_TOO_LONG, maxCommentLength);
        }
        if (rating < 1 || rating > 5) {
            throw new RiderReservationException(MessageKeys.REVIEW_RATING_INVALID);
        }
        final String storedComment = trimmed.isEmpty() ? null : trimmed;
        final Reservation r = reservationService.getOwnerReservationById(ownerUserId, reservationId)
                .orElseThrow(() -> new RiderReservationException(MessageKeys.REVIEW_NOT_ALLOWED));
        if (!r.isCarReturned()) {
            throw new RiderReservationException(MessageKeys.REVIEW_NOT_ALLOWED);
        }
        // Rated reviews must arrive inside the grace window; null-rating omit/skip may run after
        // the window (scheduler). Same rule as the SPA form gate.
        requireOwnerReviewWindowOpen(r);
        if (reviewDao.existsReview(reservationId, false)) {
            throw new RiderReservationException(MessageKeys.REVIEW_ALREADY_SUBMITTED);
        }
        final Long imageId = persistOptionalReviewImage(imageName, imageContentType, imageBytes);
        insertReviewCatchingDuplicate(reservationId, false, rating, storedComment, imageId);
        refreshAggregatesAfterOwnerReview(r);
    }

    @Override
    @Transactional
    public void submitRiderReviewOfOwner(
            final long riderUserId,
            final long reservationId,
            final Integer rating,
            final String comment,
            final String imageName,
            final String imageContentType,
            final byte[] imageBytes) {
        final String trimmed = comment == null ? "" : comment.trim();
        if (rating == null) {
            if (!trimmed.isEmpty()) {
                throw new RiderReservationException(MessageKeys.REVIEW_RATING_REQUIRED_WHEN_COMMENT);
            }
            final Reservation r = reservationService.getRiderReservationById(riderUserId, reservationId)
                    .orElseThrow(() -> new RiderReservationException(MessageKeys.REVIEW_NOT_ALLOWED));
            if (!OffsetDateTime.now(ZoneOffset.UTC).isAfter(r.getEndDate())) {
                throw new RiderReservationException(MessageKeys.REVIEW_NOT_ALLOWED);
            }
            if (reviewDao.existsReview(reservationId, true)) {
                throw new RiderReservationException(MessageKeys.REVIEW_ALREADY_SUBMITTED);
            }
            insertReviewCatchingDuplicate(reservationId, true, null, null, null);
            refreshAggregatesAfterRiderReview(r);
            return;
        }

        final int maxCommentLength = reviewValidationPolicy.getCommentMaxLength();
        if (trimmed.length() > maxCommentLength) {
            throw new RiderReservationException(MessageKeys.REVIEW_COMMENT_TOO_LONG, maxCommentLength);
        }
        if (rating < 1 || rating > 5) {
            throw new RiderReservationException(MessageKeys.REVIEW_RATING_INVALID);
        }
        final String storedComment = trimmed.isEmpty() ? null : trimmed;
        final Reservation r = reservationService.getRiderReservationById(riderUserId, reservationId)
                .orElseThrow(() -> new RiderReservationException(MessageKeys.REVIEW_NOT_ALLOWED));
        if (!OffsetDateTime.now(ZoneOffset.UTC).isAfter(r.getEndDate())) {
            throw new RiderReservationException(MessageKeys.REVIEW_NOT_ALLOWED);
        }
        requireRiderReviewWindowOpen(r);
        if (reviewDao.existsReview(reservationId, true)) {
            throw new RiderReservationException(MessageKeys.REVIEW_ALREADY_SUBMITTED);
        }
        final Long imageId = persistOptionalReviewImage(imageName, imageContentType, imageBytes);
        insertReviewCatchingDuplicate(reservationId, true, rating, storedComment, imageId);
        refreshAggregatesAfterRiderReview(r);
    }

    private void insertReviewCatchingDuplicate(
            final long reservationId,
            final boolean madeByRider,
            final Integer rating,
            final String comment,
            final Long imageId) {
        try {
            reviewDao.insertReview(reservationId, madeByRider, rating, comment, imageId);
        } catch (final DataIntegrityViolationException ex) {
            throw new RiderReservationException(MessageKeys.REVIEW_ALREADY_SUBMITTED);
        }
    }

    /**
     * Rejects a rated owner review after {@code review-auto-skip-days} from {@code carReturnedAt}
     * (falls back to {@code endDate} when the timestamp is missing on legacy rows).
     */
    private void requireOwnerReviewWindowOpen(final Reservation r) {
        final int days = reservationTimingPolicy.getReviewAutoSkipDays();
        if (days < 1) {
            return;
        }
        final OffsetDateTime windowStart = r.getCarReturnedAt().orElse(r.getEndDate());
        if (!OffsetDateTime.now(ZoneOffset.UTC).isBefore(windowStart.plusDays(days))) {
            throw new RiderReservationException(MessageKeys.REVIEW_NOT_ALLOWED);
        }
    }

    /**
     * Rejects a rated rider review after {@code review-auto-skip-days} from {@code endDate}.
     */
    private void requireRiderReviewWindowOpen(final Reservation r) {
        final int days = reservationTimingPolicy.getReviewAutoSkipDays();
        if (days < 1) {
            return;
        }
        if (!OffsetDateTime.now(ZoneOffset.UTC).isBefore(r.getEndDate().plusDays(days))) {
            throw new RiderReservationException(MessageKeys.REVIEW_NOT_ALLOWED);
        }
    }

    /**
     * After an owner-side review (owner reviews rider): recompute rider's average and the car
     * average. The persistence of each side stays in its own DAO via the corresponding service.
     *
     * Transactional contract: must be called from a method that already opened a
     * transaction (typically {@code @Transactional} on the calling submit method). The helper is
     * {@code private} so Spring's proxy-based AOP cannot decorate it, and the two reads + two
     * writes here need to share that ambient transaction to keep the rating and the just-inserted
     * review row consistent. Callers in this class ({@link #submitOwnerReviewOfRider}) are already
     * {@code @Transactional}; do not invoke from outside that context.
     */
    private void refreshAggregatesAfterOwnerReview(final Reservation r) {
        final BigDecimal riderAvg = reviewDao.findAverageRatingForCounterparty(r.getRiderId(), false);
        userService.updateRatingAsRider(r.getRiderId(), riderAvg);
        final BigDecimal carAvg = reviewDao.findAverageRatingForCar(r.getCarId());
        carService.updateRatingAvg(r.getCarId(), carAvg);
    }

    /**
     * After a rider-side review (rider reviews owner): recompute owner's average (resolved via
     * the car's owner) and the car average.
     *
     * Transactional contract: see {@link #refreshAggregatesAfterOwnerReview} - same rule
     * applies. Private helper, no proxy interception, must execute inside the caller's transaction
     * ({@link #submitRiderReviewOfOwner}).
     */
    private void refreshAggregatesAfterRiderReview(final Reservation r) {
        final long carId = r.getCarId();
        carService.getCarById(carId)
                .map(Car::getOwner)
                .ifPresent(owner -> {
                    final BigDecimal ownerAvg = reviewDao.findAverageRatingForCounterparty(owner.getId(), true);
                    userService.updateRatingAsOwner(owner.getId(), ownerAvg);
                });
        final BigDecimal carAvg = reviewDao.findAverageRatingForCar(carId);
        carService.updateRatingAvg(carId, carAvg);
    }

    /**
     * Validates and persists the optional review image via {@link ImageService} (content type and
     * byte length are checked there, raising {@link ar.edu.itba.paw.exception.image.ImageValidationException}).
     * Returns {@code null} when no bytes are provided.
     */
    private Long persistOptionalReviewImage(final String name, final String contentType, final byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        final String safeName = name != null && !name.isBlank() ? name : "review";
        final String safeType = contentType != null && !contentType.isBlank() ? contentType : "application/octet-stream";
        final Image created = imageService.createImage(safeName, safeType, bytes);
        return created.getId();
    }
}
