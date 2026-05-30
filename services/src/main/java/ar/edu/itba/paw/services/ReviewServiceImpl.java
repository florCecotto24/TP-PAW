package ar.edu.itba.paw.services;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.math.BigDecimal;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.itba.paw.exception.MessageKeys;
import ar.edu.itba.paw.exception.reservation.RiderReservationException;
import ar.edu.itba.paw.models.dto.ListingPublicReview;
import ar.edu.itba.paw.models.dto.Page;
import ar.edu.itba.paw.models.dto.profile.ReviewItemDto;
import ar.edu.itba.paw.models.domain.Image;
import ar.edu.itba.paw.models.domain.Reservation;
import ar.edu.itba.paw.services.policy.ReviewValidationPolicy;
import ar.edu.itba.paw.persistence.ReviewDao;

/** Uses only {@link ReviewDao}; reservation, car reads and image persistence go through peer services. */
@Service
public final class ReviewServiceImpl implements ReviewService {

    private final ReviewDao reviewDao;
    private final ReservationService reservationService;
    private final CarService carService;
    private final ImageService imageService;
    private final ReviewValidationPolicy reviewValidationPolicy;

    @Autowired
    public ReviewServiceImpl(
            final ReviewDao reviewDao,
            final ReservationService reservationService,
            final CarService carService,
            final ImageService imageService,
            final ReviewValidationPolicy reviewValidationPolicy) {
        this.reviewDao = reviewDao;
        this.reservationService = reservationService;
        this.carService = carService;
        this.imageService = imageService;
        this.reviewValidationPolicy = reviewValidationPolicy;
    }

    @Override
    @Transactional(readOnly = true)
    public int getReviewCommentMaxLength() {
        return reviewValidationPolicy.getCommentMaxLength();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ListingPublicReview> getCarPublicReviews(final long carId, final int page, final int pageSize) {
        return reviewDao.findCarPublicReviews(carId, page, pageSize);
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
    public List<ReviewItemDto> getRecentCommentReviewsForCounterparty(
            final long counterpartyUserId,
            final boolean counterpartyIsOwner,
            final int limit) {
        return reviewDao.findRecentCommentReviewsForCounterparty(counterpartyUserId, counterpartyIsOwner, limit);
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
            reviewDao.insertReview(reservationId, false, null, null, null);
            reviewDao.refreshRiderAverageRating(r.getRiderId());
            reviewDao.refreshCarRatingAvg(r.getCarId());
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
        if (reviewDao.existsReview(reservationId, false)) {
            throw new RiderReservationException(MessageKeys.REVIEW_ALREADY_SUBMITTED);
        }
        final Long imageId = persistOptionalReviewImage(imageName, imageContentType, imageBytes);
        reviewDao.insertReview(reservationId, false, rating, storedComment, imageId);
        reviewDao.refreshRiderAverageRating(r.getRiderId());
        reviewDao.refreshCarRatingAvg(r.getCarId());
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
            reviewDao.insertReview(reservationId, true, null, null, null);
            final long carId = r.getCarId();
            carService.getCarById(carId)
                    .map(ar.edu.itba.paw.models.domain.Car::getOwner)
                    .ifPresent(owner -> reviewDao.refreshOwnerAverageRating(owner.getId()));
            reviewDao.refreshCarRatingAvg(carId);
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
        if (reviewDao.existsReview(reservationId, true)) {
            throw new RiderReservationException(MessageKeys.REVIEW_ALREADY_SUBMITTED);
        }
        final Long imageId = persistOptionalReviewImage(imageName, imageContentType, imageBytes);
        reviewDao.insertReview(reservationId, true, rating, storedComment, imageId);
        final long carId = r.getCarId();
        carService.getCarById(carId)
                .map(ar.edu.itba.paw.models.domain.Car::getOwner)
                .ifPresent(owner -> reviewDao.refreshOwnerAverageRating(owner.getId()));
        reviewDao.refreshCarRatingAvg(carId);
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
