package ar.edu.itba.paw.services;

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
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import ar.edu.itba.paw.exception.MessageKeys;
import ar.edu.itba.paw.exception.reservation.RiderReservationException;
import ar.edu.itba.paw.models.domain.Listing;
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
    private static final long LISTING_ID = 400L;

    @Mock
    private ReviewDao reviewDao;

    @Mock
    private ReservationService reservationService;

    @Mock
    private UserService userService;

    private ReviewServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ReviewServiceImpl(
                reviewDao,
                reservationService,
                userService,
                ReviewValidationPolicy.fromValidatedCommentMaxLength(500));
    }

    private static Reservation reservation(final boolean carReturned, final OffsetDateTime endDate) {
        final Listing listingRef = Mockito.mock(Listing.class);
        Mockito.when(listingRef.getId()).thenReturn(LISTING_ID);
        return Reservation.builder()
                .id(RESERVATION_ID)
                .rider(User.identities(RIDER_ID, "r@test.com", "R", "Rider"))
                .listing(listingRef)
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
        Mockito.when(userService.getListingOwner(LISTING_ID)).thenReturn(Optional.empty());

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
                Mockito.anyString());

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
                Mockito.isNull());

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
    void testSubmitRiderReviewOfOwnerCompletesOnHappyPath() {
        // 1.Arrange
        final OffsetDateTime past = OffsetDateTime.now(ZoneOffset.UTC).minusDays(1);
        final Reservation res = reservation(true, past);
        Mockito.when(reservationService.getRiderReservationById(RIDER_ID, RESERVATION_ID))
                .thenReturn(Optional.of(res));
        Mockito.when(reviewDao.existsReview(RESERVATION_ID, true)).thenReturn(false);
        Mockito.when(userService.getListingOwner(LISTING_ID)).thenReturn(Optional.empty());

        // 2.Exercise / 3.Assert
        Assertions.assertDoesNotThrow(() -> service.submitRiderReviewOfOwner(RIDER_ID, RESERVATION_ID, 5, null));
    }
}
