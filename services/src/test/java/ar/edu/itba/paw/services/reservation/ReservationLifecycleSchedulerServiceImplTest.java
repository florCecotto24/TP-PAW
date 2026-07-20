package ar.edu.itba.paw.services.reservation;


import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
import ar.edu.itba.paw.models.domain.car.Car;
import ar.edu.itba.paw.models.domain.reservation.Reservation;
import ar.edu.itba.paw.models.domain.user.User;
import ar.edu.itba.paw.models.email.reservation.ReservationMailPayload;
import ar.edu.itba.paw.policy.ReservationTimingPolicy;
import ar.edu.itba.paw.util.ReservationMailComposer;

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
    private ReservationAvailabilityService reservationAvailabilityService;

    @Mock
    private ReservationTimingPolicy reservationTimingPolicy;

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
    void testDispatchReviewAutoSkipsProcessesRemainingRowsWhenOneRowFails() {
        // 1. Arrange — two rider candidates: the row processor fails on the first one and
        // succeeds on the second; there are no owner candidates. A per-row failure must not
        // abort the batch, so exactly one auto-skip is counted.
        final long failingReservationId = RESERVATION_ID;
        final long succeedingReservationId = RESERVATION_ID + 1;
        final Reservation failing = finishedReservation(failingReservationId);
        final Reservation succeeding = finishedReservation(succeedingReservationId);
        Mockito.when(reservationService.findReservationsForRiderReviewAutoSkip(
                Mockito.any(OffsetDateTime.class), Mockito.any(OffsetDateTime.class)))
                .thenReturn(List.of(failing, succeeding));
        Mockito.when(reservationService.findReservationsForOwnerReviewAutoSkip(
                Mockito.any(OffsetDateTime.class), Mockito.any(OffsetDateTime.class)))
                .thenReturn(List.of());
        Mockito.doThrow(new RiderReservationException("err"))
                .when(lifecycleRowProcessor).autoSkipRiderReview(RIDER_ID, failingReservationId);

        // 2. Act
        final int processed = schedulerService.dispatchReviewAutoSkips();

        // 3. Assert — the failed first row is skipped, the second row still gets processed.
        Assertions.assertEquals(1, processed);
    }

    @Test
    void testDispatchReservationReminderEmailsReturnsQueuedCountWhenPayloadPresentAndClaimed() {
        // 1. Arrange — one candidate whose payload builds fine and whose claim succeeds.
        final Reservation reservation = finishedReservation(RESERVATION_ID);
        final ReservationMailPayload payload = ReservationMailPayload.builder()
                .recipientEmail("r@test.com")
                .riderFullName("R Rider")
                .reservationId(RESERVATION_ID)
                .carId(1L)
                .vehicleLabel("Toyota Corolla")
                .startDate(START)
                .endDate(END)
                .riderMailLocale(Locale.ENGLISH)
                .ownerMailLocale(Locale.ENGLISH)
                .build();
        Mockito.when(reservationService.findReminderReservations(
                Mockito.any(OffsetDateTime.class), Mockito.any(OffsetDateTime.class)))
                .thenReturn(List.of(reservation));
        Mockito.when(reservationAvailabilityService.findEffectivePickupAvailabilitiesForReservations(
                List.of(RESERVATION_ID)))
                .thenReturn(Map.of());
        Mockito.when(mailComposer.buildReservationReminderPayload(
                Mockito.eq(reservation), Mockito.isNull()))
                .thenReturn(Optional.of(payload));
        Mockito.when(lifecycleRowProcessor.claimPickupReminder(RESERVATION_ID)).thenReturn(true);

        // 2. Act
        final int queued = schedulerService.dispatchReservationReminderEmails();

        // 3. Assert
        Assertions.assertEquals(1, queued);
    }

    @Test
    void testDispatchReservationReminderEmailsReturnsZeroWhenPayloadCannotBeBuilt() {
        // 1. Arrange — the candidate is missing rider/car/owner data, so no payload can be
        // built and nothing is claimed or queued.
        final Reservation reservation = finishedReservation(RESERVATION_ID);
        Mockito.when(reservationService.findReminderReservations(
                Mockito.any(OffsetDateTime.class), Mockito.any(OffsetDateTime.class)))
                .thenReturn(List.of(reservation));
        Mockito.when(reservationAvailabilityService.findEffectivePickupAvailabilitiesForReservations(
                List.of(RESERVATION_ID)))
                .thenReturn(Map.of());
        Mockito.when(mailComposer.buildReservationReminderPayload(
                Mockito.eq(reservation), Mockito.isNull()))
                .thenReturn(Optional.empty());

        // 2. Act
        final int queued = schedulerService.dispatchReservationReminderEmails();

        // 3. Assert
        Assertions.assertEquals(0, queued);
    }

}
