package ar.edu.itba.paw.services;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import ar.edu.itba.paw.models.dto.listing.ListingPublicReview;
import ar.edu.itba.paw.models.dto.Page;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import ar.edu.itba.paw.exception.MessageKeys;
import ar.edu.itba.paw.exception.reservation.RiderReservationException;
import ar.edu.itba.paw.models.domain.Car;
import ar.edu.itba.paw.models.domain.Reservation;
import ar.edu.itba.paw.models.domain.User;
import ar.edu.itba.paw.persistence.ReviewDao;
import ar.edu.itba.paw.services.policy.ReviewValidationPolicy;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReviewServiceImplTest {

    private static final long OWNER_ID = 100L;
    private static final long RIDER_ID = 200L;
    private static final long RESERVATION_ID = 300L;

    @Mock
    private ReviewDao reviewDao;

    @Mock
    private ReservationService reservationService;

    @Mock
    private CarService carService;

    @Mock
    private UserService userService;

    @Mock
    private ImageService imageService;

    private ReviewServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ReviewServiceImpl(
                reviewDao,
                reservationService,
                carService,
                userService,
                imageService,
                ReviewValidationPolicy.fromValidatedCommentMaxLength(500));
    }

    private static final long CAR_ID = 77L;

    private static Reservation reservation(final boolean carReturned, final OffsetDateTime endDate) {
        final Car carRef = Mockito.mock(Car.class);
        Mockito.when(carRef.getId()).thenReturn(CAR_ID);
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
                .build();
    }

    @Test
    void testGetReviewCommentMaxLengthReflectsPolicy() {
        // 1.Arrange / 2.Exercise
        final int max = service.getReviewCommentMaxLength();

        // 3.Assert
        Assertions.assertEquals(500, max);
    }

    @Test
    void testSubmitOwnerReviewOfRiderPersistOmitWhenRatingNullAndCommentBlank() {
        // 1.Arrange
        final Reservation res = reservation(true, OffsetDateTime.now(ZoneOffset.UTC).minusDays(1));
        Mockito.when(reservationService.getOwnerReservationById(OWNER_ID, RESERVATION_ID))
                .thenReturn(Optional.of(res));
        Mockito.when(reviewDao.existsReview(RESERVATION_ID, false)).thenReturn(false);

        // 2.Exercise & 3. Assert
        Assertions.assertDoesNotThrow(() -> service.submitOwnerReviewOfRider(OWNER_ID, RESERVATION_ID, null, "   "));
    }

    @Test
    void testSubmitRiderReviewOfOwnerPersistOmitWhenRatingNullAndCommentBlank() {
        // 1.Arrange
        final OffsetDateTime past = OffsetDateTime.now(ZoneOffset.UTC).minusDays(1);
        final Reservation res = reservation(true, past);
        Mockito.when(reservationService.getRiderReservationById(RIDER_ID, RESERVATION_ID))
                .thenReturn(Optional.of(res));
        Mockito.when(reviewDao.existsReview(RESERVATION_ID, true)).thenReturn(false);
        Mockito.when(carService.getCarById(CAR_ID)).thenReturn(Optional.empty());

        // 2.Exercise & 3. Assert
        Assertions.assertDoesNotThrow(() -> service.submitRiderReviewOfOwner(RIDER_ID, RESERVATION_ID, null, null));
    }

    @Test
    void testSubmitOwnerReviewOfRiderRequiresRatingWhenCommentPresent() {
        // 1.Arrange / 2.Exercise
        final RiderReservationException ex = Assertions.assertThrows(RiderReservationException.class,
                () -> service.submitOwnerReviewOfRider(OWNER_ID, RESERVATION_ID, null, "Nice ride!"));

        // 3.Assert
        Assertions.assertEquals(MessageKeys.REVIEW_RATING_REQUIRED_WHEN_COMMENT, ex.getMessageCode());
    }

    @Test
    void testSubmitOwnerReviewOfRiderRejectsRatingOutOfRange() {
        // 1.Arrange / 2.Exercise / 3.Assert
        final RiderReservationException low = Assertions.assertThrows(RiderReservationException.class,
                () -> service.submitOwnerReviewOfRider(OWNER_ID, RESERVATION_ID, 0, null));
        final RiderReservationException high = Assertions.assertThrows(RiderReservationException.class,
                () -> service.submitOwnerReviewOfRider(OWNER_ID, RESERVATION_ID, 6, null));
        Assertions.assertEquals(MessageKeys.REVIEW_RATING_INVALID, low.getMessageCode());
        Assertions.assertEquals(MessageKeys.REVIEW_RATING_INVALID, high.getMessageCode());
    }

    @Test
    void testSubmitOwnerReviewOfRiderRejectsCommentLongerThanPolicyMax() {
        // 1.Arrange
        final String longComment = "a".repeat(501);

        // 2.Exercise
        final RiderReservationException ex = Assertions.assertThrows(RiderReservationException.class,
                () -> service.submitOwnerReviewOfRider(OWNER_ID, RESERVATION_ID, 5, longComment));

        // 3.Assert
        Assertions.assertEquals(MessageKeys.REVIEW_COMMENT_TOO_LONG, ex.getMessageCode());
        Assertions.assertArrayEquals(new Object[]{500}, ex.getMessageArgs());
    }

    @Test
    void testSubmitOwnerReviewOfRiderThrowsNotAllowedWhenReservationNotFound() {
        // 1.Arrange
        Mockito.when(reservationService.getOwnerReservationById(OWNER_ID, RESERVATION_ID))
                .thenReturn(Optional.empty());

        // 2.Exercise
        final RiderReservationException ex = Assertions.assertThrows(RiderReservationException.class,
                () -> service.submitOwnerReviewOfRider(OWNER_ID, RESERVATION_ID, 5, null));

        // 3.Assert
        Assertions.assertEquals(MessageKeys.REVIEW_NOT_ALLOWED, ex.getMessageCode());
    }

    @Test
    void testSubmitOwnerReviewOfRiderThrowsNotAllowedWhenCarNotReturned() {
        // 1.Arrange
        final Reservation res = reservation(false, OffsetDateTime.now(ZoneOffset.UTC).minusDays(1));
        Mockito.when(reservationService.getOwnerReservationById(OWNER_ID, RESERVATION_ID))
                .thenReturn(Optional.of(res));

        // 2.Exercise
        final RiderReservationException ex = Assertions.assertThrows(RiderReservationException.class,
                () -> service.submitOwnerReviewOfRider(OWNER_ID, RESERVATION_ID, 5, null));

        // 3.Assert
        Assertions.assertEquals(MessageKeys.REVIEW_NOT_ALLOWED, ex.getMessageCode());
    }

    @Test
    void testSubmitOwnerReviewOfRiderThrowsAlreadySubmittedWhenReviewExists() {
        // 1.Arrange
        final Reservation res = reservation(true, OffsetDateTime.now(ZoneOffset.UTC).minusDays(1));
        Mockito.when(reservationService.getOwnerReservationById(OWNER_ID, RESERVATION_ID))
                .thenReturn(Optional.of(res));
        Mockito.when(reviewDao.existsReview(RESERVATION_ID, false)).thenReturn(true);

        // 2.Exercise
        final RiderReservationException ex = Assertions.assertThrows(RiderReservationException.class,
                () -> service.submitOwnerReviewOfRider(OWNER_ID, RESERVATION_ID, 5, null));

        // 3.Assert
        Assertions.assertEquals(MessageKeys.REVIEW_ALREADY_SUBMITTED, ex.getMessageCode());
    }

    @Test
    void testSubmitOwnerReviewOfRiderInsertsReviewWithTrimmedComment() {
        // 1.Arrange
        final Reservation res = reservation(true, OffsetDateTime.now(ZoneOffset.UTC).minusDays(1));
        Mockito.when(reservationService.getOwnerReservationById(OWNER_ID, RESERVATION_ID))
                .thenReturn(Optional.of(res));
        Mockito.when(reviewDao.existsReview(RESERVATION_ID, false)).thenReturn(false);
        final String[] insertedComment = new String[1];
        Mockito.doAnswer(inv -> {
            insertedComment[0] = inv.getArgument(3);
            return null;
        }).when(reviewDao).insertReview(Mockito.eq(RESERVATION_ID), Mockito.eq(false), Mockito.eq(4),
                Mockito.anyString(), Mockito.isNull());

        // 2.Exercise
        service.submitOwnerReviewOfRider(OWNER_ID, RESERVATION_ID, 4, "  Great rider!  ");

        // 3.Assert
        Assertions.assertEquals("Great rider!", insertedComment[0]);
    }

    @Test
    void testSubmitOwnerReviewOfRiderTreatsBlankCommentAsNull() {
        // 1.Arrange
        final Reservation res = reservation(true, OffsetDateTime.now(ZoneOffset.UTC).minusDays(1));
        Mockito.when(reservationService.getOwnerReservationById(OWNER_ID, RESERVATION_ID))
                .thenReturn(Optional.of(res));
        Mockito.when(reviewDao.existsReview(RESERVATION_ID, false)).thenReturn(false);
        final Object[] capturedComment = new Object[]{"sentinel"};
        Mockito.doAnswer(inv -> {
            capturedComment[0] = inv.getArgument(3);
            return null;
        }).when(reviewDao).insertReview(Mockito.eq(RESERVATION_ID), Mockito.eq(false), Mockito.eq(5),
                Mockito.isNull(), Mockito.isNull());

        // 2.Exercise
        service.submitOwnerReviewOfRider(OWNER_ID, RESERVATION_ID, 5, "   ");

        // 3.Assert
        Assertions.assertNull(capturedComment[0]);
    }

    @Test
    void testSubmitRiderReviewOfOwnerThrowsWhenReservationNotFound() {
        // 1.Arrange
        Mockito.when(reservationService.getRiderReservationById(RIDER_ID, RESERVATION_ID))
                .thenReturn(Optional.empty());

        // 2.Exercise / 3.Assert
        final RiderReservationException ex = Assertions.assertThrows(RiderReservationException.class,
                () -> service.submitRiderReviewOfOwner(RIDER_ID, RESERVATION_ID, 5, null));
        Assertions.assertEquals(MessageKeys.REVIEW_NOT_ALLOWED, ex.getMessageCode());
    }

    @Test
    void testSubmitRiderReviewOfOwnerThrowsWhenEndDateInFuture() {
        // 1.Arrange
        final OffsetDateTime future = OffsetDateTime.now(ZoneOffset.UTC).plusDays(7);
        final Reservation res = reservation(true, future);
        Mockito.when(reservationService.getRiderReservationById(RIDER_ID, RESERVATION_ID))
                .thenReturn(Optional.of(res));

        // 2.Exercise
        final RiderReservationException ex = Assertions.assertThrows(RiderReservationException.class,
                () -> service.submitRiderReviewOfOwner(RIDER_ID, RESERVATION_ID, 5, null));

        // 3.Assert
        Assertions.assertEquals(MessageKeys.REVIEW_NOT_ALLOWED, ex.getMessageCode());
    }

    @Test
    void testSubmitRiderReviewOfOwnerThrowsAlreadySubmittedWhenReviewExists() {
        // 1.Arrange
        final OffsetDateTime past = OffsetDateTime.now(ZoneOffset.UTC).minusDays(1);
        final Reservation res = reservation(true, past);
        Mockito.when(reservationService.getRiderReservationById(RIDER_ID, RESERVATION_ID))
                .thenReturn(Optional.of(res));
        Mockito.when(reviewDao.existsReview(RESERVATION_ID, true)).thenReturn(true);

        // 2.Exercise
        final RiderReservationException ex = Assertions.assertThrows(RiderReservationException.class,
                () -> service.submitRiderReviewOfOwner(RIDER_ID, RESERVATION_ID, 5, null));

        // 3.Assert
        Assertions.assertEquals(MessageKeys.REVIEW_ALREADY_SUBMITTED, ex.getMessageCode());
    }

    @Test
    void testGetCarPublicReviewsReturnsWhateverTheDaoProvides() {
        // 1.Arrange
        final ListingPublicReview oneReview = new ListingPublicReview(
                "Ada", "Lovelace",
                OffsetDateTime.of(2026, 5, 1, 12, 0, 0, 0, ZoneOffset.UTC),
                5, "Loved the car", null);
        final Page<ListingPublicReview> daoPage = new Page<>(
                List.of(oneReview), 0, 6, 1L);
        Mockito.when(reviewDao.findCarPublicReviews(CAR_ID, 0, 6)).thenReturn(daoPage);

        // 2.Exercise
        final Page<ListingPublicReview> actual = service.getCarPublicReviews(CAR_ID, 0, 6);

        // 3.Assert
        Assertions.assertSame(daoPage, actual);
        Assertions.assertEquals(1, actual.getContent().size());
        Assertions.assertEquals(1L, actual.getTotalItems());
    }

    @Test
    void testGetCarPublicReviewsReturnsEmptyPageWhenCarHasNoReviews() {
        // 1.Arrange
        final Page<ListingPublicReview> emptyPage = new Page<>(Collections.emptyList(), 0, 6, 0L);
        Mockito.when(reviewDao.findCarPublicReviews(CAR_ID, 0, 6)).thenReturn(emptyPage);

        // 2.Exercise
        final Page<ListingPublicReview> actual = service.getCarPublicReviews(CAR_ID, 0, 6);

        // 3.Assert
        Assertions.assertTrue(actual.getContent().isEmpty());
        Assertions.assertEquals(0L, actual.getTotalItems());
    }

    @Test
    void testCountReviewsForCarDelegatesToDao() {
        // 1.Arrange
        Mockito.when(reviewDao.countReviewsForCar(CAR_ID)).thenReturn(42L);

        // 2.Exercise
        final long count = service.countReviewsForCar(CAR_ID);

        // 3.Assert
        Assertions.assertEquals(42L, count);
    }

    @Test
    void testSubmitRiderReviewOfOwnerCompletesOnHappyPath() {
        // 1.Arrange
        final OffsetDateTime past = OffsetDateTime.now(ZoneOffset.UTC).minusDays(1);
        final Reservation res = reservation(true, past);
        Mockito.when(reservationService.getRiderReservationById(RIDER_ID, RESERVATION_ID))
                .thenReturn(Optional.of(res));
        Mockito.when(reviewDao.existsReview(RESERVATION_ID, true)).thenReturn(false);
        Mockito.when(carService.getCarById(CAR_ID)).thenReturn(Optional.empty());

        // 2.Exercise / 3.Assert
        Assertions.assertDoesNotThrow(() -> service.submitRiderReviewOfOwner(RIDER_ID, RESERVATION_ID, 5, null));
    }
}
