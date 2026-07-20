package ar.edu.itba.paw.services.review;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

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
import ar.edu.itba.paw.models.domain.review.Review;
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

    // In-memory fake per AGENTS.md TEST-8: writes are observed only through the DAO read
    // contract (the Review the SUT returns after re-reading), never through recorded calls.
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
        return reservation(Reservation.Status.FINISHED, carReturned, endDate, carReturnedAt);
    }

    private static Reservation reservation(
            final Reservation.Status status, final boolean carReturned,
            final OffsetDateTime endDate, final OffsetDateTime carReturnedAt) {
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
                .status(status)
                .createdAt(OffsetDateTime.of(2026, 3, 1, 10, 0, 0, 0, ZoneOffset.UTC))
                .updatedAt(OffsetDateTime.of(2026, 4, 5, 10, 0, 0, 0, ZoneOffset.UTC))
                .totalPrice(new BigDecimal("100"))
                .carReturned(carReturned)
                .carReturnedAt(carReturnedAt)
                .build();
    }

    @Test
    void testSubmitParticipantReviewAsOwnerPersistsSkipRowWhenRatingNullAndCommentBlank() {
        // 1.Arrange
        final Reservation res = reservation(true, OffsetDateTime.now(ZoneOffset.UTC).minusDays(1));
        Mockito.when(reservationService.getOwnerReservationById(OWNER_ID, RESERVATION_ID))
                .thenReturn(Optional.of(res));
        reviewDao.stubReservation(res);

        // 2.Act
        final Review skip = service.submitParticipantReview(
                OWNER_ID, RESERVATION_ID, null, "   ", null, null, null);

        // 3.Assert
        Assertions.assertFalse(skip.isMadeByRider());
        Assertions.assertTrue(skip.getRating().isEmpty());
        Assertions.assertTrue(skip.getComment().isEmpty());
    }

    @Test
    void testSubmitParticipantReviewAsRiderPersistsSkipRowWhenRatingNullAndCommentNull() {
        // 1.Arrange
        final OffsetDateTime past = OffsetDateTime.now(ZoneOffset.UTC).minusDays(1);
        final Reservation res = reservation(true, past);
        Mockito.when(reservationService.getOwnerReservationById(RIDER_ID, RESERVATION_ID))
                .thenReturn(Optional.empty());
        Mockito.when(reservationService.getRiderReservationById(RIDER_ID, RESERVATION_ID))
                .thenReturn(Optional.of(res));
        Mockito.when(carService.getCarById(CAR_ID)).thenReturn(Optional.empty());
        reviewDao.stubReservation(res);

        // 2.Act
        final Review skip = service.submitParticipantReview(
                RIDER_ID, RESERVATION_ID, null, null, null, null, null);

        // 3.Assert
        Assertions.assertTrue(skip.isMadeByRider());
        Assertions.assertTrue(skip.getRating().isEmpty());
        Assertions.assertTrue(skip.getComment().isEmpty());
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
    void testSubmitOwnerReviewOfRiderThrowsNotAllowedWhenReservationCancelled() {
        // 1.Arrange
        final OffsetDateTime past = OffsetDateTime.now(ZoneOffset.UTC).minusDays(1);
        final Reservation res = reservation(Reservation.Status.CANCELLED_BY_RIDER, true, past, past);
        Mockito.when(reservationService.getOwnerReservationById(OWNER_ID, RESERVATION_ID))
                .thenReturn(Optional.of(res));

        // 2.Act
        final RiderReservationException ex = Assertions.assertThrows(RiderReservationException.class,
                () -> service.submitOwnerReviewOfRider(OWNER_ID, RESERVATION_ID, 5, null));

        // 3.Assert
        Assertions.assertEquals(MessageKeys.REVIEW_NOT_ALLOWED, ex.getMessageCode());
    }

    @Test
    void testSubmitOwnerReviewOfRiderThrowsNotAllowedOnOmitWhenReservationCancelled() {
        // 1.Arrange
        final OffsetDateTime past = OffsetDateTime.now(ZoneOffset.UTC).minusDays(1);
        final Reservation res = reservation(Reservation.Status.CANCELLED_BY_RIDER, true, past, past);
        Mockito.when(reservationService.getOwnerReservationById(OWNER_ID, RESERVATION_ID))
                .thenReturn(Optional.of(res));

        // 2.Act
        final RiderReservationException ex = Assertions.assertThrows(RiderReservationException.class,
                () -> service.submitOwnerReviewOfRider(OWNER_ID, RESERVATION_ID, null, "   "));

        // 3.Assert
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
    void testSubmitParticipantReviewAsOwnerReturnsPersistedReviewWithTrimmedComment() {
        // 1.Arrange
        final Reservation res = reservation(true, OffsetDateTime.now(ZoneOffset.UTC).minusDays(1));
        Mockito.when(reservationService.getOwnerReservationById(OWNER_ID, RESERVATION_ID))
                .thenReturn(Optional.of(res));
        reviewDao.stubReservation(res);

        // 2.Act
        final Review created = service.submitParticipantReview(
                OWNER_ID, RESERVATION_ID, 4, "  Great rider!  ", null, null, null);

        // 3.Assert
        Assertions.assertEquals(RESERVATION_ID, created.getReservationId());
        Assertions.assertFalse(created.isMadeByRider());
        Assertions.assertEquals(4, created.getRating().orElseThrow());
        Assertions.assertEquals("Great rider!", created.getComment().orElseThrow());
        Assertions.assertTrue(created.getImage().isEmpty());
    }

    @Test
    void testSubmitParticipantReviewAsOwnerTreatsBlankCommentAsNull() {
        // 1.Arrange
        final Reservation res = reservation(true, OffsetDateTime.now(ZoneOffset.UTC).minusDays(1));
        Mockito.when(reservationService.getOwnerReservationById(OWNER_ID, RESERVATION_ID))
                .thenReturn(Optional.of(res));
        reviewDao.stubReservation(res);

        // 2.Act
        final Review created = service.submitParticipantReview(
                OWNER_ID, RESERVATION_ID, 5, "   ", null, null, null);

        // 3.Assert
        Assertions.assertTrue(created.getComment().isEmpty());
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
    void testSubmitRiderReviewOfOwnerThrowsNotAllowedWhenReservationCancelled() {
        // 1.Arrange
        final OffsetDateTime past = OffsetDateTime.now(ZoneOffset.UTC).minusDays(1);
        final Reservation res = reservation(Reservation.Status.CANCELLED_BY_OWNER, false, past, null);
        Mockito.when(reservationService.getRiderReservationById(RIDER_ID, RESERVATION_ID))
                .thenReturn(Optional.of(res));

        // 2.Act
        final RiderReservationException ex = Assertions.assertThrows(RiderReservationException.class,
                () -> service.submitRiderReviewOfOwner(RIDER_ID, RESERVATION_ID, 5, null));

        // 3.Assert
        Assertions.assertEquals(MessageKeys.REVIEW_NOT_ALLOWED, ex.getMessageCode());
    }

    @Test
    void testSubmitRiderReviewOfOwnerThrowsNotAllowedOnOmitWhenReservationCancelled() {
        // 1.Arrange
        final OffsetDateTime past = OffsetDateTime.now(ZoneOffset.UTC).minusDays(1);
        final Reservation res = reservation(Reservation.Status.CANCELLED_BY_OWNER, false, past, null);
        Mockito.when(reservationService.getRiderReservationById(RIDER_ID, RESERVATION_ID))
                .thenReturn(Optional.of(res));

        // 2.Act
        final RiderReservationException ex = Assertions.assertThrows(RiderReservationException.class,
                () -> service.submitRiderReviewOfOwner(RIDER_ID, RESERVATION_ID, null, null));

        // 3.Assert
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
    void testSubmitParticipantReviewAsRiderPersistsSkipRowAfterAutoSkipWindow() {
        // 1.Arrange
        final OffsetDateTime endDate = OffsetDateTime.now(ZoneOffset.UTC).minusDays(16);
        final Reservation res = reservation(true, endDate);
        Mockito.when(reservationService.getOwnerReservationById(RIDER_ID, RESERVATION_ID))
                .thenReturn(Optional.empty());
        Mockito.when(reservationService.getRiderReservationById(RIDER_ID, RESERVATION_ID))
                .thenReturn(Optional.of(res));
        Mockito.when(carService.getCarById(CAR_ID)).thenReturn(Optional.empty());
        reviewDao.stubReservation(res);

        // 2.Act
        final Review skip = service.submitParticipantReview(
                RIDER_ID, RESERVATION_ID, null, null, null, null, null);

        // 3.Assert
        Assertions.assertTrue(skip.isMadeByRider());
        Assertions.assertTrue(skip.getRating().isEmpty());
    }

    @Test
    void testSubmitParticipantReviewAsRiderReturnsPersistedRatedReviewOnHappyPath() {
        // 1.Arrange
        final OffsetDateTime past = OffsetDateTime.now(ZoneOffset.UTC).minusDays(1);
        final Reservation res = reservation(true, past);
        Mockito.when(reservationService.getOwnerReservationById(RIDER_ID, RESERVATION_ID))
                .thenReturn(Optional.empty());
        Mockito.when(reservationService.getRiderReservationById(RIDER_ID, RESERVATION_ID))
                .thenReturn(Optional.of(res));
        Mockito.when(carService.getCarById(CAR_ID)).thenReturn(Optional.empty());
        reviewDao.stubReservation(res);

        // 2.Act
        final Review created = service.submitParticipantReview(
                RIDER_ID, RESERVATION_ID, 5, null, null, null, null);

        // 3.Assert
        Assertions.assertTrue(created.isMadeByRider());
        Assertions.assertEquals(5, created.getRating().orElseThrow());
        Assertions.assertTrue(created.getComment().isEmpty());
    }
}
