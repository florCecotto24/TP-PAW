package ar.edu.itba.paw.services;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import ar.edu.itba.paw.exception.reservation.RiderReservationException;
import ar.edu.itba.paw.models.domain.Car;
import ar.edu.itba.paw.models.domain.Reservation;
import ar.edu.itba.paw.models.domain.User;
import ar.edu.itba.paw.persistence.ReservationDao;
import ar.edu.itba.paw.services.policy.ReservationTimingPolicy;

@ExtendWith(MockitoExtension.class)
class ReservationServiceDispatchReviewAutoSkipsTest {

    private static final long RIDER_ID = 200L;
    private static final long OWNER_ID = 100L;
    private static final long RESERVATION_ID = 300L;

    @Mock
    private ReservationDao reservationDao;

    @Mock
    private ReservationTimingPolicy reservationTimingPolicy;

    @Mock
    private ReviewService reviewService;

    @Mock
    private CarService carService;

    @Mock
    private UserService userService;

    @Mock
    private EmailService emailService;

    @Mock
    private StoredFileService storedFileService;

    @Mock
    private ar.edu.itba.paw.services.policy.PaymentReceiptUploadPolicy paymentReceiptUploadPolicy;

    @Mock
    private ReservationAvailabilityService reservationAvailabilityService;

    @Mock
    private CarAvailabilityService carAvailabilityService;

    @Mock
    private ar.edu.itba.paw.services.util.CarAvailabilityAddressFormatter carAvailabilityAddressFormatter;

    @Mock
    private ar.edu.itba.paw.services.policy.PaginationPolicy paginationPolicy;

    @InjectMocks
    private ReservationServiceImpl reservationService;

    private static final OffsetDateTime START = OffsetDateTime.parse("2026-04-01T10:00:00Z");
    private static final OffsetDateTime END = OffsetDateTime.parse("2026-04-05T18:00:00Z");
    private static final OffsetDateTime CREATED = OffsetDateTime.parse("2026-03-01T10:00:00Z");
    private static final OffsetDateTime UPDATED = OffsetDateTime.parse("2026-04-05T10:00:00Z");

    private static Reservation finishedReservation(final long reservationId) {
        final Car car = Mockito.mock(Car.class);
        final User owner = Mockito.mock(User.class);
        Mockito.lenient().when(car.getOwner()).thenReturn(owner);
        Mockito.lenient().when(owner.getId()).thenReturn(OWNER_ID);
        return Reservation.builder()
                .id(reservationId)
                .rider(User.identities(RIDER_ID, "r@test.com", "R", "Rider"))
                .car(car)
                .startDate(START)
                .endDate(END)
                .status(Reservation.Status.FINISHED)
                .createdAt(CREATED)
                .updatedAt(UPDATED)
                .totalPrice(new BigDecimal("100"))
                .carReturned(true)
                .build();
    }

    @BeforeEach
    void defaultPolicy() {
        Mockito.when(reservationTimingPolicy.getReviewAutoSkipDays()).thenReturn(15);
    }

    @Test
    void testDispatchReviewAutoSkipsClosesRiderReviewWhenNoReviewExists() {
        // 1.Arrange
        final Reservation r = finishedReservation(RESERVATION_ID);
        Mockito.when(reservationDao.findReservationsForRiderReviewAutoSkip(
                Mockito.any(OffsetDateTime.class), Mockito.any(OffsetDateTime.class)))
                .thenReturn(List.of(r));
        Mockito.when(reservationDao.findReservationsForOwnerReviewAutoSkip(
                Mockito.any(OffsetDateTime.class), Mockito.any(OffsetDateTime.class)))
                .thenReturn(List.of());
        Mockito.when(reviewService.hasRiderReview(RESERVATION_ID)).thenReturn(false);

        // 2.Exercise
        reservationService.dispatchReviewAutoSkips();

        // 3.Assert
        Mockito.verify(reviewService).submitRiderReviewOfOwner(RIDER_ID, RESERVATION_ID, null, null);
        Mockito.verify(reviewService, Mockito.never()).submitOwnerReviewOfRider(
                Mockito.anyLong(), Mockito.anyLong(), Mockito.any(), Mockito.any());
    }

    @Test
    void testDispatchReviewAutoSkipsSkipsRiderWhenReviewAlreadyExists() {
        // 1.Arrange
        final Reservation r = finishedReservation(RESERVATION_ID);
        Mockito.when(reservationDao.findReservationsForRiderReviewAutoSkip(
                Mockito.any(OffsetDateTime.class), Mockito.any(OffsetDateTime.class)))
                .thenReturn(List.of(r));
        Mockito.when(reservationDao.findReservationsForOwnerReviewAutoSkip(
                Mockito.any(OffsetDateTime.class), Mockito.any(OffsetDateTime.class)))
                .thenReturn(List.of());
        Mockito.when(reviewService.hasRiderReview(RESERVATION_ID)).thenReturn(true);

        // 2.Exercise
        reservationService.dispatchReviewAutoSkips();

        // 3.Assert
        Mockito.verify(reviewService, Mockito.never()).submitRiderReviewOfOwner(
                Mockito.anyLong(), Mockito.anyLong(), Mockito.any(), Mockito.any());
    }

    @Test
    void testDispatchReviewAutoSkipsSwallowsRiderReservationException() {
        // 1.Arrange
        final Reservation r = finishedReservation(RESERVATION_ID);
        Mockito.when(reservationDao.findReservationsForRiderReviewAutoSkip(
                Mockito.any(OffsetDateTime.class), Mockito.any(OffsetDateTime.class)))
                .thenReturn(List.of(r));
        Mockito.when(reservationDao.findReservationsForOwnerReviewAutoSkip(
                Mockito.any(OffsetDateTime.class), Mockito.any(OffsetDateTime.class)))
                .thenReturn(List.of());
        Mockito.when(reviewService.hasRiderReview(RESERVATION_ID)).thenReturn(false);
        Mockito.doThrow(new RiderReservationException("err"))
                .when(reviewService).submitRiderReviewOfOwner(RIDER_ID, RESERVATION_ID, null, null);

        // 2.Exercise + 3.Assert (no exception)
        Assertions.assertDoesNotThrow(() -> reservationService.dispatchReviewAutoSkips());
    }

    @Test
    void testDispatchReviewAutoSkipsDisabledWhenDaysIsZero() {
        // 1.Arrange
        Mockito.when(reservationTimingPolicy.getReviewAutoSkipDays()).thenReturn(0);

        // 2.Exercise
        reservationService.dispatchReviewAutoSkips();

        // 3.Assert
        Mockito.verify(reservationDao, Mockito.never()).findReservationsForRiderReviewAutoSkip(
                Mockito.any(OffsetDateTime.class), Mockito.any(OffsetDateTime.class));
        Mockito.verify(reservationDao, Mockito.never()).findReservationsForOwnerReviewAutoSkip(
                Mockito.any(OffsetDateTime.class), Mockito.any(OffsetDateTime.class));
        Mockito.verifyNoInteractions(reviewService);
    }

    @Test
    void testDispatchReviewAutoSkipsClosesOwnerReviewWhenNoReviewExists() {
        // 1.Arrange
        final Reservation r = finishedReservation(RESERVATION_ID);
        Mockito.when(reservationDao.findReservationsForRiderReviewAutoSkip(
                Mockito.any(OffsetDateTime.class), Mockito.any(OffsetDateTime.class)))
                .thenReturn(List.of());
        Mockito.when(reservationDao.findReservationsForOwnerReviewAutoSkip(
                Mockito.any(OffsetDateTime.class), Mockito.any(OffsetDateTime.class)))
                .thenReturn(List.of(r));
        Mockito.when(reviewService.hasOwnerReview(RESERVATION_ID)).thenReturn(false);

        // 2.Exercise
        reservationService.dispatchReviewAutoSkips();

        // 3.Assert
        Mockito.verify(reviewService).submitOwnerReviewOfRider(OWNER_ID, RESERVATION_ID, null, null);
        Mockito.verify(reviewService, Mockito.never()).submitRiderReviewOfOwner(
                Mockito.anyLong(), Mockito.anyLong(), Mockito.any(), Mockito.any());
    }

    @Test
    void testDispatchReviewAutoSkipsSkipsOwnerWithoutResolvedOwnerUserId() {
        // 1.Arrange — car exists but owner is missing (defensive guard).
        final Car car = Mockito.mock(Car.class);
        Mockito.when(car.getOwner()).thenReturn(null);
        final Reservation r = Reservation.builder()
                .id(RESERVATION_ID)
                .rider(User.identities(RIDER_ID, "r@test.com", "R", "Rider"))
                .car(car)
                .startDate(START)
                .endDate(END)
                .status(Reservation.Status.FINISHED)
                .createdAt(CREATED)
                .updatedAt(UPDATED)
                .totalPrice(new BigDecimal("100"))
                .carReturned(true)
                .build();
        Mockito.when(reservationDao.findReservationsForRiderReviewAutoSkip(
                Mockito.any(OffsetDateTime.class), Mockito.any(OffsetDateTime.class)))
                .thenReturn(List.of());
        Mockito.when(reservationDao.findReservationsForOwnerReviewAutoSkip(
                Mockito.any(OffsetDateTime.class), Mockito.any(OffsetDateTime.class)))
                .thenReturn(List.of(r));
        Mockito.when(reviewService.hasOwnerReview(RESERVATION_ID)).thenReturn(false);

        // 2.Exercise
        reservationService.dispatchReviewAutoSkips();

        // 3.Assert
        Mockito.verify(reviewService, Mockito.never()).submitOwnerReviewOfRider(
                Mockito.anyLong(), Mockito.anyLong(), Mockito.any(), Mockito.any());
    }
}
