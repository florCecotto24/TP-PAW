package ar.edu.itba.paw.services.reservation;


import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import ar.edu.itba.paw.exception.reservation.RiderReservationException;
import ar.edu.itba.paw.models.domain.car.Car;
import ar.edu.itba.paw.models.domain.reservation.Reservation;
import ar.edu.itba.paw.models.domain.user.User;
import ar.edu.itba.paw.policy.ReservationTimingPolicy;
import ar.edu.itba.paw.util.ReservationMailComposer;

import ar.edu.itba.paw.services.review.ReviewService;
/**
 * Coverage for the review auto-skip scheduler. The other dispatch methods (return reminder,
 * checkout, review invite) are exercised through the controller and integration tests.
 */
@ExtendWith(MockitoExtension.class)
class ReservationLifecycleSchedulerServiceImplTest {

    private static final long RIDER_ID = 200L;
    private static final long OWNER_ID = 100L;
    private static final long RESERVATION_ID = 300L;

    @Mock
    // Architectural rule: this service no longer touches ReservationDao; tests mock
    // ReservationService (the sole DAO owner) instead.
    private ReservationService reservationService;

    @Mock
    private ReservationTimingPolicy reservationTimingPolicy;

    @Mock
    private ReservationPricingService pricingService;

    @Mock
    private ReviewService reviewService;

    @Mock
    private ReservationMailComposer mailComposer;

    @Mock
    private ReservationLifecycleRowProcessor lifecycleRowProcessor;

    @InjectMocks
    private ReservationLifecycleSchedulerServiceImpl schedulerService;

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
        Mockito.lenient().when(reservationTimingPolicy.getReviewAutoSkipDays()).thenReturn(15);
    }

 

    @Test
    void testDispatchReviewAutoSkipsSwallowsRiderReservationException() {
        final Reservation r = finishedReservation(RESERVATION_ID);
        Mockito.when(reservationService.findReservationsForRiderReviewAutoSkip(
                Mockito.any(OffsetDateTime.class), Mockito.any(OffsetDateTime.class)))
                .thenReturn(List.of(r));
        Mockito.when(reservationService.findReservationsForOwnerReviewAutoSkip(
                Mockito.any(OffsetDateTime.class), Mockito.any(OffsetDateTime.class)))
                .thenReturn(List.of());
        Mockito.when(reviewService.hasRiderReview(RESERVATION_ID)).thenReturn(false);
        Mockito.doThrow(new RiderReservationException("err"))
                .when(lifecycleRowProcessor).autoSkipRiderReview(RIDER_ID, RESERVATION_ID);

        Assertions.assertDoesNotThrow(() -> schedulerService.dispatchReviewAutoSkips());
    }





}
