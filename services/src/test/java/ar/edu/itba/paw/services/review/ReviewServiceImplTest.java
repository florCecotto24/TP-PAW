package ar.edu.itba.paw.services.review;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import ar.edu.itba.paw.models.dto.car.CarPublicReview;
import ar.edu.itba.paw.models.dto.Page;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import ar.edu.itba.paw.exception.MessageKeys;
import ar.edu.itba.paw.exception.reservation.RiderReservationException;
import ar.edu.itba.paw.models.domain.car.Car;
import ar.edu.itba.paw.models.domain.reservation.Reservation;
import ar.edu.itba.paw.models.domain.user.User;
import ar.edu.itba.paw.policy.ReservationTimingPolicy;
import ar.edu.itba.paw.policy.ReviewValidationPolicy;

import ar.edu.itba.paw.services.car.CarService;
import ar.edu.itba.paw.services.file.ImageService;
import ar.edu.itba.paw.services.reservation.ReservationService;
import ar.edu.itba.paw.services.support.RecordingReviewDao;
import ar.edu.itba.paw.services.user.UserService;
@ExtendWith(MockitoExtension.class)
class ReviewServiceImplTest {

    private static final long OWNER_ID = 100L;
    private static final long RIDER_ID = 200L;
    private static final long RESERVATION_ID = 300L;

    // State-based double per AGENTS.md TEST-8: tests stub read-side return values through the
    // double's stub* helpers and assert on inserted() to verify writes — no doAnswer captor.
    private RecordingReviewDao reviewDao;

    @Mock
    private ReservationService reservationService;

    @Mock
    private CarService carService;

    @Mock
    private UserService userService;

    @Mock
    private ImageService imageService;

    @Mock
    private ReservationTimingPolicy reservationTimingPolicy;

    private ReviewServiceImpl service;

    @BeforeEach
    void setUp() {
        reviewDao = new RecordingReviewDao();
        Mockito.lenient().when(reservationTimingPolicy.getReviewAutoSkipDays()).thenReturn(15);
        service = new ReviewServiceImpl(
                reviewDao,
                reservationService,
                carService,
                userService,
                imageService,
                ReviewValidationPolicy.fromValidatedCommentMaxLength(500),
                reservationTimingPolicy);
    }

    private static final long CAR_ID = 77L;

    private static Reservation reservation(final boolean carReturned, final OffsetDateTime endDate) {
        return reservation(carReturned, endDate, carReturned ? endDate : null);
    }

    private static Reservation reservation(
            final boolean carReturned, final OffsetDateTime endDate, final OffsetDateTime carReturnedAt) {
        final Car carRef = Mockito.mock(Car.class);
        // lenient(): only the happy paths reach refreshAggregatesAfter*Review which queries the car;
        // negative-path tests short-circuit before resolving the car id, so strict-stubs would flag
        // this shared helper stub as unnecessary on those tests.
        Mockito.lenient().when(carRef.getId()).thenReturn(CAR_ID);
        return Reservation.builder()
                .id(RESERVATION_ID)
                .rider(User.identities(RIDER_ID, "r@test.com", "R", "Rider"))
                .car(carRef)
                .startDate(OffsetDateTime.of(2026, 4, 1, 10, 0, 0, 0, ZoneOffset.UTC))
                .endDate(endDate)
                .status(Reservation.Status.FINISHED)
                .createdAt(OffsetDateTime.of(2026, 3, 1, 10, 0, 0, 0, ZoneOffset.UTC))
                .updatedAt(OffsetDateTime.of(2026, 4, 5, 10, 0, 0, 0, ZoneOffset.UTC))
                .totalPrice(new BigDecimal("100"))
                .carReturned(carReturned)
                .carReturnedAt(carReturnedAt)
                .build();
    }

    @Test
    void testGetReviewCommentMaxLengthReflectsPolicy() {
        final int max = service.getReviewCommentMaxLength();

        Assertions.assertEquals(500, max);
    }

    @Test
    void testSubmitOwnerReviewOfRiderPersistOmitWhenRatingNullAndCommentBlank() {
        final Reservation res = reservation(true, OffsetDateTime.now(ZoneOffset.UTC).minusDays(1));
        Mockito.when(reservationService.getOwnerReservationById(OWNER_ID, RESERVATION_ID))
                .thenReturn(Optional.of(res));

        Assertions.assertDoesNotThrow(() -> service.submitOwnerReviewOfRider(OWNER_ID, RESERVATION_ID, null, "   "));
    }

    @Test
    void testSubmitRiderReviewOfOwnerPersistOmitWhenRatingNullAndCommentBlank() {
        final OffsetDateTime past = OffsetDateTime.now(ZoneOffset.UTC).minusDays(1);
        final Reservation res = reservation(true, past);
        Mockito.when(reservationService.getRiderReservationById(RIDER_ID, RESERVATION_ID))
                .thenReturn(Optional.of(res));
        Mockito.when(carService.getCarById(CAR_ID)).thenReturn(Optional.empty());

        Assertions.assertDoesNotThrow(() -> service.submitRiderReviewOfOwner(RIDER_ID, RESERVATION_ID, null, null));
    }

    @Test
    void testSubmitOwnerReviewOfRiderRequiresRatingWhenCommentPresent() {
        final RiderReservationException ex = Assertions.assertThrows(RiderReservationException.class,
                () -> service.submitOwnerReviewOfRider(OWNER_ID, RESERVATION_ID, null, "Nice ride!"));

        Assertions.assertEquals(MessageKeys.REVIEW_RATING_REQUIRED_WHEN_COMMENT, ex.getMessageCode());
    }

    @Test
    void testSubmitOwnerReviewOfRiderRejectsRatingBelowRange() {
        // Split out of the original "rating out of range" test so each @Test has a single Act
        // call (TEST-2: one behavior per test).
        final RiderReservationException ex = Assertions.assertThrows(RiderReservationException.class,
                () -> service.submitOwnerReviewOfRider(OWNER_ID, RESERVATION_ID, 0, null));
        Assertions.assertEquals(MessageKeys.REVIEW_RATING_INVALID, ex.getMessageCode());
    }

    @Test
    void testSubmitOwnerReviewOfRiderRejectsRatingAboveRange() {
        final RiderReservationException ex = Assertions.assertThrows(RiderReservationException.class,
                () -> service.submitOwnerReviewOfRider(OWNER_ID, RESERVATION_ID, 6, null));
        Assertions.assertEquals(MessageKeys.REVIEW_RATING_INVALID, ex.getMessageCode());
    }

    @Test
    void testSubmitOwnerReviewOfRiderRejectsCommentLongerThanPolicyMax() {
        final String longComment = "a".repeat(501);

        final RiderReservationException ex = Assertions.assertThrows(RiderReservationException.class,
                () -> service.submitOwnerReviewOfRider(OWNER_ID, RESERVATION_ID, 5, longComment));

        Assertions.assertEquals(MessageKeys.REVIEW_COMMENT_TOO_LONG, ex.getMessageCode());
        Assertions.assertArrayEquals(new Object[]{500}, ex.getMessageArgs());
    }

    @Test
    void testSubmitOwnerReviewOfRiderThrowsNotAllowedWhenReservationNotFound() {
        Mockito.when(reservationService.getOwnerReservationById(OWNER_ID, RESERVATION_ID))
                .thenReturn(Optional.empty());

        final RiderReservationException ex = Assertions.assertThrows(RiderReservationException.class,
                () -> service.submitOwnerReviewOfRider(OWNER_ID, RESERVATION_ID, 5, null));

        Assertions.assertEquals(MessageKeys.REVIEW_NOT_ALLOWED, ex.getMessageCode());
    }

    @Test
    void testSubmitOwnerReviewOfRiderThrowsNotAllowedWhenCarNotReturned() {
        final Reservation res = reservation(false, OffsetDateTime.now(ZoneOffset.UTC).minusDays(1));
        Mockito.when(reservationService.getOwnerReservationById(OWNER_ID, RESERVATION_ID))
                .thenReturn(Optional.of(res));

        final RiderReservationException ex = Assertions.assertThrows(RiderReservationException.class,
                () -> service.submitOwnerReviewOfRider(OWNER_ID, RESERVATION_ID, 5, null));

        Assertions.assertEquals(MessageKeys.REVIEW_NOT_ALLOWED, ex.getMessageCode());
    }

    @Test
    void testSubmitOwnerReviewOfRiderThrowsAlreadySubmittedWhenReviewExists() {
        final Reservation res = reservation(true, OffsetDateTime.now(ZoneOffset.UTC).minusDays(1));
        Mockito.when(reservationService.getOwnerReservationById(OWNER_ID, RESERVATION_ID))
                .thenReturn(Optional.of(res));
        reviewDao.stubExistsReview(RESERVATION_ID, false, true);

        final RiderReservationException ex = Assertions.assertThrows(RiderReservationException.class,
                () -> service.submitOwnerReviewOfRider(OWNER_ID, RESERVATION_ID, 5, null));

        Assertions.assertEquals(MessageKeys.REVIEW_ALREADY_SUBMITTED, ex.getMessageCode());
    }

    @Test
    void testSubmitOwnerReviewOfRiderInsertsReviewWithTrimmedComment() {
        final Reservation res = reservation(true, OffsetDateTime.now(ZoneOffset.UTC).minusDays(1));
        Mockito.when(reservationService.getOwnerReservationById(OWNER_ID, RESERVATION_ID))
                .thenReturn(Optional.of(res));

        service.submitOwnerReviewOfRider(OWNER_ID, RESERVATION_ID, 4, "  Great rider!  ");

        Assertions.assertEquals(1, reviewDao.inserted().size());
        final RecordingReviewDao.InsertedReview row = reviewDao.inserted().get(0);
        Assertions.assertEquals(RESERVATION_ID, row.reservationId());
        Assertions.assertFalse(row.madeByRider());
        Assertions.assertEquals(4, row.rating());
        Assertions.assertEquals("Great rider!", row.comment());
        Assertions.assertNull(row.imageId());
    }

    @Test
    void testSubmitOwnerReviewOfRiderTreatsBlankCommentAsNull() {
        final Reservation res = reservation(true, OffsetDateTime.now(ZoneOffset.UTC).minusDays(1));
        Mockito.when(reservationService.getOwnerReservationById(OWNER_ID, RESERVATION_ID))
                .thenReturn(Optional.of(res));

        service.submitOwnerReviewOfRider(OWNER_ID, RESERVATION_ID, 5, "   ");

        Assertions.assertEquals(1, reviewDao.inserted().size());
        Assertions.assertNull(reviewDao.inserted().get(0).comment());
    }

    @Test
    void testSubmitRiderReviewOfOwnerThrowsWhenReservationNotFound() {
        Mockito.when(reservationService.getRiderReservationById(RIDER_ID, RESERVATION_ID))
                .thenReturn(Optional.empty());

        final RiderReservationException ex = Assertions.assertThrows(RiderReservationException.class,
                () -> service.submitRiderReviewOfOwner(RIDER_ID, RESERVATION_ID, 5, null));
        Assertions.assertEquals(MessageKeys.REVIEW_NOT_ALLOWED, ex.getMessageCode());
    }

    @Test
    void testSubmitRiderReviewOfOwnerThrowsWhenEndDateInFuture() {
        final OffsetDateTime future = OffsetDateTime.now(ZoneOffset.UTC).plusDays(7);
        final Reservation res = reservation(true, future);
        Mockito.when(reservationService.getRiderReservationById(RIDER_ID, RESERVATION_ID))
                .thenReturn(Optional.of(res));

        final RiderReservationException ex = Assertions.assertThrows(RiderReservationException.class,
                () -> service.submitRiderReviewOfOwner(RIDER_ID, RESERVATION_ID, 5, null));

        Assertions.assertEquals(MessageKeys.REVIEW_NOT_ALLOWED, ex.getMessageCode());
    }

    @Test
    void testSubmitRiderReviewOfOwnerThrowsAlreadySubmittedWhenReviewExists() {
        final OffsetDateTime past = OffsetDateTime.now(ZoneOffset.UTC).minusDays(1);
        final Reservation res = reservation(true, past);
        Mockito.when(reservationService.getRiderReservationById(RIDER_ID, RESERVATION_ID))
                .thenReturn(Optional.of(res));
        reviewDao.stubExistsReview(RESERVATION_ID, true, true);

        final RiderReservationException ex = Assertions.assertThrows(RiderReservationException.class,
                () -> service.submitRiderReviewOfOwner(RIDER_ID, RESERVATION_ID, 5, null));

        Assertions.assertEquals(MessageKeys.REVIEW_ALREADY_SUBMITTED, ex.getMessageCode());
    }

    @Test
    void testSubmitRiderReviewOfOwnerRejectsRatedReviewAfterAutoSkipWindow() {
        // 1.Arrange
        final OffsetDateTime endDate = OffsetDateTime.now(ZoneOffset.UTC).minusDays(16);
        final Reservation res = reservation(true, endDate);
        Mockito.when(reservationService.getRiderReservationById(RIDER_ID, RESERVATION_ID))
                .thenReturn(Optional.of(res));

        // 2.Act
        final RiderReservationException ex = Assertions.assertThrows(RiderReservationException.class,
                () -> service.submitRiderReviewOfOwner(RIDER_ID, RESERVATION_ID, 5, "Late"));

        // 3.Assert
        Assertions.assertEquals(MessageKeys.REVIEW_NOT_ALLOWED, ex.getMessageCode());
    }

    @Test
    void testSubmitOwnerReviewOfRiderRejectsRatedReviewAfterAutoSkipWindow() {
        // 1.Arrange
        final OffsetDateTime returnedAt = OffsetDateTime.now(ZoneOffset.UTC).minusDays(16);
        final Reservation res = reservation(true, returnedAt.minusDays(1), returnedAt);
        Mockito.when(reservationService.getOwnerReservationById(OWNER_ID, RESERVATION_ID))
                .thenReturn(Optional.of(res));

        // 2.Act
        final RiderReservationException ex = Assertions.assertThrows(RiderReservationException.class,
                () -> service.submitOwnerReviewOfRider(OWNER_ID, RESERVATION_ID, 4, "Late"));

        // 3.Assert
        Assertions.assertEquals(MessageKeys.REVIEW_NOT_ALLOWED, ex.getMessageCode());
    }

    @Test
    void testSubmitRiderReviewOfOwnerAllowsOmitAfterAutoSkipWindow() {
        // 1.Arrange
        final OffsetDateTime endDate = OffsetDateTime.now(ZoneOffset.UTC).minusDays(16);
        final Reservation res = reservation(true, endDate);
        Mockito.when(reservationService.getRiderReservationById(RIDER_ID, RESERVATION_ID))
                .thenReturn(Optional.of(res));
        Mockito.when(carService.getCarById(CAR_ID)).thenReturn(Optional.empty());

        // 2.Act / 3.Assert
        Assertions.assertDoesNotThrow(() -> service.submitRiderReviewOfOwner(RIDER_ID, RESERVATION_ID, null, null));
    }

    @Test
    void testGetCarPublicReviewsReturnsWhateverTheDaoProvides() {
        final CarPublicReview oneReview = new CarPublicReview(
                "Ada", "Lovelace",
                OffsetDateTime.of(2026, 5, 1, 12, 0, 0, 0, ZoneOffset.UTC),
                5, "Loved the car", null);
        final Page<CarPublicReview> daoPage = new Page<>(List.of(oneReview), 0, 6, 1L);
        reviewDao.stubCarPublicReviews(CAR_ID, 0, 6, daoPage);

        final Page<CarPublicReview> actual = service.getCarPublicReviews(CAR_ID, 0, 6);

        Assertions.assertSame(daoPage, actual);
        Assertions.assertEquals(1, actual.getContent().size());
        Assertions.assertEquals(1L, actual.getTotalItems());
    }

    @Test
    void testGetCarPublicReviewsReturnsEmptyPageWhenCarHasNoReviews() {
        final Page<CarPublicReview> emptyPage = new Page<>(Collections.emptyList(), 0, 6, 0L);
        reviewDao.stubCarPublicReviews(CAR_ID, 0, 6, emptyPage);

        final Page<CarPublicReview> actual = service.getCarPublicReviews(CAR_ID, 0, 6);

        Assertions.assertTrue(actual.getContent().isEmpty());
        Assertions.assertEquals(0L, actual.getTotalItems());
    }

    @Test
    void testSubmitRiderReviewOfOwnerCompletesOnHappyPath() {
        final OffsetDateTime past = OffsetDateTime.now(ZoneOffset.UTC).minusDays(1);
        final Reservation res = reservation(true, past);
        Mockito.when(reservationService.getRiderReservationById(RIDER_ID, RESERVATION_ID))
                .thenReturn(Optional.of(res));
        Mockito.when(carService.getCarById(CAR_ID)).thenReturn(Optional.empty());

        Assertions.assertDoesNotThrow(() -> service.submitRiderReviewOfOwner(RIDER_ID, RESERVATION_ID, 5, null));
    }
}
