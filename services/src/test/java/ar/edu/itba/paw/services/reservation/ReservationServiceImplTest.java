package ar.edu.itba.paw.services.reservation;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import ar.edu.itba.paw.models.domain.car.Car;
import ar.edu.itba.paw.models.domain.reservation.Reservation;
import ar.edu.itba.paw.models.domain.user.User;
import ar.edu.itba.paw.models.dto.reservation.BlockingReservationProjection;
import ar.edu.itba.paw.persistence.reservation.ReservationDao;

/**
 * Behavioural coverage for {@link ReservationServiceImpl}, the facade that is the sole owner of
 * {@code ReservationDao}. Most of its surface is thin delegation to the DAO or to the focused
 * sub-services and is deliberately NOT tested here (TEST-4: pass-throughs carry no logic of
 * their own); this class covers only the methods that add behaviour on top of the DAO row:
 * participant scoping, grouping, and projection-to-id mapping.
 */
@ExtendWith(MockitoExtension.class)
class ReservationServiceImplTest {

    private static final long RIDER_ID = 20L;
    private static final long OWNER_ID = 10L;
    private static final long RESERVATION_ID = 300L;

    @Mock
    private ReservationDao reservationDao;

    @Mock
    private ReservationQueryService queryService;

    @Mock
    private ReservationPricingService pricingService;

    @Mock
    private ReservationWorkflowService workflowService;

    @Mock
    private ReservationLifecycleSchedulerService schedulerService;

    @Mock
    private ReservationPaymentService paymentService;

    @InjectMocks
    private ReservationServiceImpl reservationService;

    private static Car car(final long carId) {
        final User owner = User.identities(OWNER_ID, "owner@test.com", "Owner", "Test");
        return Car.builder()
                .id(carId).owner(owner).plate("ABC123")
                .powertrain(Car.Powertrain.GASOLINE).transmission(Car.Transmission.MANUAL)
                .build();
    }

    private static Reservation reservation(final long reservationId, final long riderId, final Car car) {
        final User rider = User.identities(riderId, "rider@test.com", "Rider", "Test");
        final OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        return Reservation.builder()
                .id(reservationId).rider(rider).car(car)
                .status(Reservation.Status.PENDING)
                .totalPrice(new BigDecimal("100.00"))
                .startDate(OffsetDateTime.parse("2030-06-01T10:00:00Z"))
                .endDate(OffsetDateTime.parse("2030-06-02T18:00:00Z"))
                .createdAt(now).updatedAt(now)
                .build();
    }

    @Test
    void testGetRiderReservationByIdReturnsReservationWhenViewerIsTheRider() {
        // 1. Arrange — the DAO has no rider-scoped read; the facade applies the filter itself.
        final Reservation owned = reservation(RESERVATION_ID, RIDER_ID, car(1L));
        Mockito.when(reservationDao.getReservationById(RESERVATION_ID)).thenReturn(Optional.of(owned));

        // 2. Act
        final Optional<Reservation> result = reservationService.getRiderReservationById(RIDER_ID, RESERVATION_ID);

        // 3. Assert
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(RESERVATION_ID, result.get().getId());
        Assertions.assertEquals(RIDER_ID, result.get().getRiderId());
    }

    @Test
    void testGetRiderReservationByIdReturnsEmptyWhenReservationBelongsToAnotherRider() {
        // 1. Arrange — the row exists but is owned by a different rider, so the scope filter hides it.
        final Reservation someoneElses = reservation(RESERVATION_ID, RIDER_ID, car(1L));
        Mockito.when(reservationDao.getReservationById(RESERVATION_ID)).thenReturn(Optional.of(someoneElses));

        // 2. Act
        final Optional<Reservation> result = reservationService.getRiderReservationById(99L, RESERVATION_ID);

        // 3. Assert
        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    void testFindBlockingReservationsByCarIdsReturnsEmptyMapWhenNoCarIdsGiven() {
        // 1. Arrange — nothing to stub: the empty-input guard must answer without touching the DAO.

        // 2. Act
        final Map<Long, List<BlockingReservationProjection>> result =
                reservationService.findBlockingReservationsByCarIds(List.of());

        // 3. Assert
        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    void testFindBlockingReservationsByCarIdsGroupsProjectionsByCarId() {
        // 1. Arrange — the DAO returns a flat list across cars; the facade groups it per car.
        final OffsetDateTime start = OffsetDateTime.parse("2030-06-01T10:00:00Z");
        final OffsetDateTime end = OffsetDateTime.parse("2030-06-02T18:00:00Z");
        final BlockingReservationProjection car1First =
                new BlockingReservationProjection(301L, 1L, start, end, Reservation.Status.ACCEPTED);
        final BlockingReservationProjection car1Second =
                new BlockingReservationProjection(302L, 1L, start.plusDays(5), end.plusDays(5),
                        Reservation.Status.PENDING);
        final BlockingReservationProjection car2Only =
                new BlockingReservationProjection(303L, 2L, start, end, Reservation.Status.STARTED);
        Mockito.when(reservationDao.findBlockingByCarIds(List.of(1L, 2L)))
                .thenReturn(List.of(car1First, car1Second, car2Only));

        // 2. Act
        final Map<Long, List<BlockingReservationProjection>> result =
                reservationService.findBlockingReservationsByCarIds(List.of(1L, 2L));

        // 3. Assert — one entry per car, each holding exactly its own projections.
        Assertions.assertEquals(Set.of(1L, 2L), result.keySet());
        Assertions.assertEquals(List.of(car1First, car1Second), result.get(1L));
        Assertions.assertEquals(List.of(car2Only), result.get(2L));
    }

    @Test
    void testFindOverdueRefundProofReservationIdsForOwnerMapsReservationsToIds() {
        // 1. Arrange — the DAO returns full reservations; the facade exposes only their ids.
        final Car sharedCar = car(1L);
        final Reservation first = reservation(301L, RIDER_ID, sharedCar);
        final Reservation second = reservation(302L, RIDER_ID, sharedCar);
        Mockito.when(reservationDao.findOverdueRefundProofReservationsForOwner(
                        Mockito.eq(OWNER_ID), Mockito.any(OffsetDateTime.class)))
                .thenReturn(List.of(first, second));

        // 2. Act
        final List<Long> result = reservationService.findOverdueRefundProofReservationIdsForOwner(OWNER_ID);

        // 3. Assert — same order as the DAO rows, reduced to ids.
        Assertions.assertEquals(List.of(301L, 302L), result);
    }

    @Test
    void testFindOwnerCarIdsWithReservationRequiringRefundProofReturnsDistinctCarIds() {
        // 1. Arrange — two pending-refund reservations on car 1 and one on car 2: the facade
        // collapses them into the distinct set of car ids.
        final Car carOne = car(1L);
        final Car carTwo = car(2L);
        Mockito.when(reservationDao.findReservationsRequiringRefundProofForOwner(OWNER_ID))
                .thenReturn(List.of(
                        reservation(301L, RIDER_ID, carOne),
                        reservation(302L, RIDER_ID, carOne),
                        reservation(303L, RIDER_ID, carTwo)));

        // 2. Act
        final Set<Long> result = reservationService.findOwnerCarIdsWithReservationRequiringRefundProof(OWNER_ID);

        // 3. Assert
        Assertions.assertEquals(Set.of(1L, 2L), result);
    }
}
