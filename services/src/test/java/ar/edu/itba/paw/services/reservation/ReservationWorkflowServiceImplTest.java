package ar.edu.itba.paw.services.reservation;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import ar.edu.itba.paw.exception.MessageKeys;
import ar.edu.itba.paw.exception.reservation.ReservationCancelNotAllowedException;
import ar.edu.itba.paw.exception.reservation.RiderReservationException;
import ar.edu.itba.paw.models.domain.car.AvailabilityPeriod;
import ar.edu.itba.paw.models.domain.car.Car;
import ar.edu.itba.paw.models.domain.car.CarAvailability;
import ar.edu.itba.paw.models.domain.reservation.Reservation;
import ar.edu.itba.paw.models.domain.file.StoredFile;
import ar.edu.itba.paw.models.domain.user.User;
import ar.edu.itba.paw.services.reservation.ReservationPricingService.ReservationPlan;
import ar.edu.itba.paw.util.ReservationMailComposer;

import ar.edu.itba.paw.services.car.CarService;
import ar.edu.itba.paw.services.user.UserService;
/**
 * Behavioural coverage for {@link ReservationWorkflowServiceImpl}. The end-to-end car-based
 * reservation flow is exercised through controller / integration tests; this class focuses
 * on the small, branchy validation paths that benefit from isolated mock-based assertions.
 *
 * <p>Architectural rule: the workflow service no longer touches {@code ReservationDao}; tests
 * mock {@link ReservationService} (the sole DAO owner) instead.</p>
 */
@ExtendWith(MockitoExtension.class)
class ReservationWorkflowServiceImplTest {

    private static final String WALL_FROM = "2030-06-01T10:00";
    private static final String WALL_UNTIL = "2030-06-02T18:00";
    private static final OffsetDateTime INTERVAL_START =
            AvailabilityPeriod.parseWallLocalDateTimeToUtc(WALL_FROM);
    private static final OffsetDateTime INTERVAL_END =
            AvailabilityPeriod.parseWallLocalDateTimeToUtc(WALL_UNTIL);
    private static final LocalDate INTERVAL_START_DAY = LocalDate.of(2030, 6, 1);
    private static final LocalDate INTERVAL_END_DAY = LocalDate.of(2030, 6, 2);

    @Mock
    private ReservationService reservationService;

    @Mock
    private ReservationAvailabilityService reservationAvailabilityService;

    @Mock
    private ReservationQueryService queryService;

    @Mock
    private ReservationPricingService pricingService;

    @Mock
    private UserService userService;

    @Mock
    private CarService carService;

    @Mock
    private ReservationMailComposer mailComposer;

    @Mock
    private ReservationPaymentService reservationPaymentService;

    @InjectMocks
    private ReservationWorkflowServiceImpl workflowService;

    @Test
    void testSubmitRiderReservationByCarThrowsWhenOwnerIsBlocked() {
        final long carId = 1L;
        final long riderId = 20L;
        final long ownerId = 10L;
        final User blockedOwner = User.builder()
                .id(ownerId).email("o@test.com").forename("Owner").surname("Test")
                .blocked(true)
                .build();
        final Car car = Car.builder()
                .id(carId)
                .owner(blockedOwner)
                .plate("ABC123")
                .powertrain(Car.Powertrain.GASOLINE)
                .transmission(Car.Transmission.MANUAL)
                .build();
        Mockito.when(carService.getCarById(carId)).thenReturn(Optional.of(car));

        final RiderReservationException thrown = Assertions.assertThrows(
                RiderReservationException.class,
                () -> workflowService.submitRiderReservationByCar(
                        riderId, carId, 99L, "2030-06-01T10:00", "2030-06-02T18:00"));
        Assertions.assertEquals(MessageKeys.RESERVATION_OWNER_BLOCKED, thrown.getMessageCode());
    }

    @Test
    void testSubmitRiderReservationByCarWhenBillableDaysBelowMinimumThrowsException() {
        // 2 wall days (June 1–2, 2030) < minimumRentalDays = 5
        final long carId = 1L;
        final long riderId = 20L;
        final long ownerId = 10L;
        final long availabilityId = 99L;
        final String fromDateTime = "2030-06-01T10:00";
        final String untilDateTime = "2030-06-02T18:00";

        final User owner = User.identities(ownerId, "owner@test.com", "Owner", "Test");
        final User rider = User.identities(riderId, "rider@test.com", "Rider", "Test");
        final Car car = Car.builder()
                .id(carId)
                .owner(owner)
                .plate("ABC123")
                .powertrain(Car.Powertrain.GASOLINE)
                .transmission(Car.Transmission.MANUAL)
                .minimumRentalDays(5)
                .build();
        final CarAvailability avRow = CarAvailability.builder()
                .id(availabilityId)
                .car(car)
                .startInclusive(LocalDate.of(2030, 5, 1))
                .endInclusive(LocalDate.of(2030, 7, 31))
                .dayPrice(new BigDecimal("100.00"))
                .startPointStreet("Av. Test")
                .kind(CarAvailability.Kind.OFFERED)
                .build();

        Mockito.when(carService.getCarById(carId)).thenReturn(Optional.of(car));
        Mockito.when(userService.getUserById(riderId)).thenReturn(Optional.of(rider));
        Mockito.doThrow(new RiderReservationException(MessageKeys.RESERVATION_RIDER_BELOW_MINIMUM_DAYS, 5))
                .when(pricingService).validateRiderReservationPricingInterval(
                        Mockito.eq(carId),
                        Mockito.eq(availabilityId),
                        Mockito.any(),
                        Mockito.any(),
                        Mockito.eq(5));

        final RiderReservationException thrown = Assertions.assertThrows(
                RiderReservationException.class,
                () -> workflowService.submitRiderReservationByCar(
                        riderId, carId, availabilityId, fromDateTime, untilDateTime));
        Assertions.assertEquals(MessageKeys.RESERVATION_RIDER_BELOW_MINIMUM_DAYS, thrown.getMessageCode());
    }

    @Test
    void testSubmitRiderReservationByCarThrowsWhenRiderIsTheOwner() {
        final long carId = 1L;
        final long sharedId = 7L;
        final User user = User.identities(sharedId, "self@test.com", "Self", "User");
        final Car car = Car.builder()
                .id(carId)
                .owner(user)
                .plate("ABC123")
                .powertrain(Car.Powertrain.GASOLINE)
                .transmission(Car.Transmission.MANUAL)
                .minimumRentalDays(1)
                .build();
        Mockito.when(carService.getCarById(carId)).thenReturn(Optional.of(car));
        Mockito.when(userService.getUserById(sharedId)).thenReturn(Optional.of(user));
        Mockito.doNothing().when(pricingService).validateRiderReservationPricingInterval(
                Mockito.eq(carId), Mockito.isNull(), Mockito.any(), Mockito.any(), Mockito.eq(1));

        final RiderReservationException thrown = Assertions.assertThrows(
                RiderReservationException.class,
                () -> workflowService.submitRiderReservationByCar(
                        sharedId, carId, null, "2030-06-01T10:00", "2030-06-02T18:00"));
        Assertions.assertEquals(MessageKeys.RESERVATION_RIDER_CANNOT_RESERVE_OWN_LISTING, thrown.getMessageCode());
    }

    // ---------------------------------------------------------------------------------------
    // Lifecycle happy-paths. All assertions read state from the returned aggregate or from a
    // mock that simulates the post-DAO-update read.
    // ---------------------------------------------------------------------------------------

    @Test
    void testSubmitRiderReservationByCarReturnsCreatedReservationWhenHappyPath() {
        // 1. Arrange
        final long carId = 1L;
        final long riderId = 20L;
        final long ownerId = 10L;
        final long availabilityId = 99L;
        final long createdReservationId = 555L;
        final User owner = User.identities(ownerId, "owner@test.com", "Owner", "Test");
        final User rider = User.identities(riderId, "rider@test.com", "Rider", "Test");
        final Car car = Car.builder()
                .id(carId).owner(owner).plate("ABC123")
                .powertrain(Car.Powertrain.GASOLINE).transmission(Car.Transmission.MANUAL)
                .minimumRentalDays(1)
                .build();
        final CarAvailability avRow = CarAvailability.builder()
                .id(availabilityId).car(car)
                .startInclusive(LocalDate.of(2030, 5, 1))
                .endInclusive(LocalDate.of(2030, 7, 31))
                .dayPrice(new BigDecimal("100.00"))
                .startPointStreet("Av. Test")
                .kind(CarAvailability.Kind.OFFERED)
                .build();
        final BigDecimal expectedTotal = new BigDecimal("200.00");
        final OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        final Reservation persisted = Reservation.builder()
                .id(createdReservationId)
                .rider(rider).car(car)
                .status(Reservation.Status.PENDING)
                .totalPrice(expectedTotal)
                .startDate(OffsetDateTime.parse("2030-06-01T10:00:00Z"))
                .endDate(OffsetDateTime.parse("2030-06-02T18:00:00Z"))
                .createdAt(now).updatedAt(now)
                .build();

        Mockito.when(carService.getCarById(carId)).thenReturn(Optional.of(car));
        Mockito.when(userService.getUserById(riderId)).thenReturn(Optional.of(rider));
        Mockito.doNothing().when(pricingService).validateRiderReservationPricingInterval(
                Mockito.eq(carId), Mockito.eq(availabilityId), Mockito.any(), Mockito.any(), Mockito.eq(1));
        Mockito.when(userService.hasUploadedLicenseAndIdentity(rider)).thenReturn(true);
        Mockito.when(userService.getUserCbu(ownerId)).thenReturn("12345678901234567890123");
        Mockito.when(pricingService.getConfiguredPaymentProofDeadlineHours()).thenReturn(24);
        Mockito.when(reservationService.hasActiveOverlapByCar(
                Mockito.eq(carId), Mockito.eq(INTERVAL_START), Mockito.eq(INTERVAL_END))).thenReturn(false);
        Mockito.when(pricingService.planReservationByCar(
                        Mockito.eq(carId), Mockito.eq(INTERVAL_START_DAY), Mockito.eq(INTERVAL_END_DAY)))
                .thenReturn(Optional.of(new ReservationPlan(
                        expectedTotal,
                        new LinkedHashSet<>(Set.of(availabilityId)),
                        avRow)));
        Mockito.when(reservationService.createReservationForCar(
                        Mockito.eq(riderId), Mockito.eq(carId),
                        Mockito.eq(INTERVAL_START), Mockito.eq(INTERVAL_END),
                        Mockito.eq(Reservation.Status.PENDING),
                        Mockito.eq(expectedTotal),
                        Mockito.isNotNull()))
                .thenReturn(persisted);

        // 2. Act
        final Reservation result = workflowService.submitRiderReservationByCar(
                riderId, carId, availabilityId, "2030-06-01T10:00", "2030-06-02T18:00");

        // 3. Assert — the returned aggregate reflects the persisted Reservation produced by the DAO.
        Assertions.assertEquals(createdReservationId, result.getId());
        Assertions.assertEquals(Reservation.Status.PENDING, result.getStatus());
        Assertions.assertEquals(0, expectedTotal.compareTo(result.getTotalPrice()));
        Assertions.assertEquals(riderId, result.getRiderId());
        Assertions.assertEquals(carId, result.getCarId());
    }

    @Test
    void testCancelReservationReturnsReservationInMissingPaymentProofStatus() {
        // 1. Arrange — system-driven cancel for expired payment-proof window.
        final long reservationId = 77L;
        final User owner = User.identities(10L, "owner@test.com", "Owner", "Test");
        final User rider = User.identities(20L, "rider@test.com", "Rider", "Test");
        final Car car = Car.builder()
                .id(1L).owner(owner).plate("ABC123")
                .powertrain(Car.Powertrain.GASOLINE).transmission(Car.Transmission.MANUAL)
                .build();
        final OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        final Reservation cancelled = Reservation.builder()
                .id(reservationId).rider(rider).car(car)
                .status(Reservation.Status.CANCELLED_DUE_TO_MISSING_PAYMENT_PROOF)
                .totalPrice(new BigDecimal("100.00"))
                .startDate(OffsetDateTime.parse("2099-01-01T10:00:00Z"))
                .endDate(OffsetDateTime.parse("2099-01-02T18:00:00Z"))
                .createdAt(now).updatedAt(now)
                .build();
        Mockito.when(reservationService.getReservationById(reservationId)).thenReturn(Optional.of(cancelled));

        // 2. Act
        final Optional<Reservation> result = workflowService.cancelReservation(reservationId);

        // 3. Assert
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(Reservation.Status.CANCELLED_DUE_TO_MISSING_PAYMENT_PROOF, result.get().getStatus());
        Assertions.assertEquals(reservationId, result.get().getId());
    }

    @Test
    void testCancelReservationAsParticipantByRiderOnPendingReturnsCancelledByRider() {
        // 1. Arrange — rider cancels their PENDING reservation that has no payment receipt yet.
        final long reservationId = 88L;
        final long riderId = 20L;
        final long ownerId = 10L;
        final User owner = User.identities(ownerId, "owner@test.com", "Owner", "Test");
        final User rider = User.identities(riderId, "rider@test.com", "Rider", "Test");
        final Car car = Car.builder()
                .id(1L).owner(owner).plate("ABC123")
                .powertrain(Car.Powertrain.GASOLINE).transmission(Car.Transmission.MANUAL)
                .build();
        final OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        final Reservation pending = Reservation.builder()
                .id(reservationId).rider(rider).car(car)
                .status(Reservation.Status.PENDING)
                .totalPrice(new BigDecimal("100.00"))
                .startDate(OffsetDateTime.parse("2099-01-01T10:00:00Z"))
                .endDate(OffsetDateTime.parse("2099-01-02T18:00:00Z"))
                .createdAt(now).updatedAt(now)
                .build();
        final Reservation cancelledByRider = Reservation.builder()
                .id(reservationId).rider(rider).car(car)
                .status(Reservation.Status.CANCELLED_BY_RIDER)
                .totalPrice(new BigDecimal("100.00"))
                .startDate(pending.getStartDate())
                .endDate(pending.getEndDate())
                .createdAt(now).updatedAt(now)
                .build();
        // First lookup returns the PENDING source; second lookup (post-update) returns the cancelled row.
        Mockito.when(reservationService.getReservationById(reservationId))
                .thenReturn(Optional.of(pending))
                .thenReturn(Optional.of(cancelledByRider));
        Mockito.when(reservationService.updateParticipantCancellationWithRefundMeta(
                reservationId, "cancelled_by_rider", false, null)).thenReturn(1);

        // 2. Act
        final Optional<Reservation> result = workflowService.cancelReservationAsParticipant(riderId, reservationId);

        // 3. Assert
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(Reservation.Status.CANCELLED_BY_RIDER, result.get().getStatus());
    }

    @Test
    void testCancelReservationAsParticipantByOwnerOnAcceptedReturnsCancelledByOwner() {
        // 1. Arrange — owner cancels an ACCEPTED reservation before pickup; rider already paid so
        //              the cancel goes through the refund-required branch.
        final long reservationId = 91L;
        final long riderId = 20L;
        final long ownerId = 10L;
        final User owner = User.identities(ownerId, "owner@test.com", "Owner", "Test");
        final User rider = User.identities(riderId, "rider@test.com", "Rider", "Test");
        final Car car = Car.builder()
                .id(1L).owner(owner).plate("ABC123")
                .powertrain(Car.Powertrain.GASOLINE).transmission(Car.Transmission.MANUAL)
                .build();
        final OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        final StoredFile receipt = StoredFile.identified(
                500L, rider, "receipt.pdf", "application/pdf", new byte[] {1}, null);
        final Reservation accepted = Reservation.builder()
                .id(reservationId).rider(rider).car(car)
                .status(Reservation.Status.ACCEPTED)
                .totalPrice(new BigDecimal("200.00"))
                .startDate(OffsetDateTime.parse("2099-01-01T10:00:00Z"))
                .endDate(OffsetDateTime.parse("2099-01-02T18:00:00Z"))
                .paymentReceiptFile(receipt)
                .createdAt(now).updatedAt(now)
                .build();
        final Reservation cancelledByOwner = Reservation.builder()
                .id(reservationId).rider(rider).car(car)
                .status(Reservation.Status.CANCELLED_BY_OWNER)
                .totalPrice(new BigDecimal("200.00"))
                .startDate(accepted.getStartDate())
                .endDate(accepted.getEndDate())
                .paymentReceiptFile(receipt)
                .paymentRefundRequired(true)
                .createdAt(now).updatedAt(now)
                .build();
        Mockito.when(reservationService.getReservationById(reservationId))
                .thenReturn(Optional.of(accepted))
                .thenReturn(Optional.of(cancelledByOwner));
        Mockito.when(pricingService.getConfiguredRefundProofDeadlineHours()).thenReturn(24);
        Mockito.when(reservationService.updateParticipantCancellationWithRefundMeta(
                Mockito.eq(reservationId),
                Mockito.eq("cancelled_by_owner"),
                Mockito.eq(true),
                ArgumentMatchers.any(OffsetDateTime.class))).thenReturn(1);

        // 2. Act
        final Optional<Reservation> result = workflowService.cancelReservationAsParticipant(ownerId, reservationId);

        // 3. Assert — owner cancellation succeeded and the post-update read reports the refund-required state.
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(Reservation.Status.CANCELLED_BY_OWNER, result.get().getStatus());
        Assertions.assertTrue(result.get().isPaymentRefundRequired());
    }

    @Test
    void testMarkCarReturnedByOwnerDoesNotThrowWhenAcceptedReservationIsCheckedOut() {
        // 1. Arrange — endDate is in the past and the reservation is ACCEPTED, so the owner is
        //              allowed to mark the car returned. The post-mark reload reports
        //              carReturned=true so the sync step advances the status to FINISHED.
        final long reservationId = 102L;
        final long ownerId = 10L;
        final long riderId = 20L;
        final User owner = User.identities(ownerId, "owner@test.com", "Owner", "Test");
        final User rider = User.identities(riderId, "rider@test.com", "Rider", "Test");
        final Car car = Car.builder()
                .id(1L).owner(owner).plate("ABC123")
                .powertrain(Car.Powertrain.GASOLINE).transmission(Car.Transmission.MANUAL)
                .build();
        final OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        final OffsetDateTime startInPast = OffsetDateTime.parse("2000-01-01T10:00:00Z");
        final OffsetDateTime endInPast = OffsetDateTime.parse("2000-01-02T18:00:00Z");
        final Reservation acceptedBeforeReturn = Reservation.builder()
                .id(reservationId).rider(rider).car(car)
                .status(Reservation.Status.ACCEPTED)
                .totalPrice(new BigDecimal("200.00"))
                .startDate(startInPast).endDate(endInPast)
                .createdAt(now).updatedAt(now)
                .build();
        final Reservation afterReturn = Reservation.builder()
                .id(reservationId).rider(rider).car(car)
                .status(Reservation.Status.ACCEPTED)
                .totalPrice(new BigDecimal("200.00"))
                .startDate(startInPast).endDate(endInPast)
                .carReturned(true)
                .carReturnedAt(now)
                .createdAt(now).updatedAt(now)
                .build();
        Mockito.when(queryService.getOwnerReservationById(ownerId, reservationId))
                .thenReturn(Optional.of(acceptedBeforeReturn))
                .thenReturn(Optional.of(afterReturn));
        Mockito.when(reservationService.markCarReturned(reservationId, ownerId)).thenReturn(1);
        Mockito.when(reservationService.updateReservationStatus(
                ArgumentMatchers.eq(reservationId), ArgumentMatchers.anyString())).thenReturn(1);

        // 2. Act and 3. Assert — orchestration completes without surfacing an exception.
        Assertions.assertDoesNotThrow(() -> workflowService.markCarReturnedByOwner(ownerId, reservationId));
    }

    @Test
    void testPatchReservationThrowsWhenCancellationStatusIsNotParticipantCancel() {
        Assertions.assertThrows(
                ReservationCancelNotAllowedException.class,
                () -> workflowService.patchReservation(
                        20L, 100L, Reservation.Status.ACCEPTED, null, null, null));
    }

    @Test
    void testPatchReservationReturnsReloadedReservationAfterParticipantCancel() {
        // 1. Arrange
        final long reservationId = 200L;
        final long riderId = 20L;
        final long ownerId = 10L;
        final User owner = User.identities(ownerId, "owner@test.com", "Owner", "Test");
        final User rider = User.identities(riderId, "rider@test.com", "Rider", "Test");
        final Car car = Car.builder()
                .id(1L).owner(owner).plate("ABC123")
                .powertrain(Car.Powertrain.GASOLINE).transmission(Car.Transmission.MANUAL)
                .build();
        final OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        final Reservation pending = Reservation.builder()
                .id(reservationId).rider(rider).car(car)
                .status(Reservation.Status.PENDING)
                .totalPrice(new BigDecimal("100.00"))
                .startDate(now.plusDays(5))
                .endDate(now.plusDays(7))
                .createdAt(now).updatedAt(now)
                .build();
        final Reservation cancelled = Reservation.builder()
                .id(reservationId).rider(rider).car(car)
                .status(Reservation.Status.CANCELLED_BY_RIDER)
                .totalPrice(new BigDecimal("100.00"))
                .startDate(pending.getStartDate())
                .endDate(pending.getEndDate())
                .createdAt(now).updatedAt(now)
                .build();
        Mockito.when(queryService.getRiderReservationById(riderId, reservationId))
                .thenReturn(Optional.of(pending));
        Mockito.when(reservationService.updateParticipantCancellationWithRefundMeta(
                Mockito.eq(reservationId),
                Mockito.eq("cancelled_by_rider"),
                Mockito.eq(false),
                ArgumentMatchers.isNull())).thenReturn(1);
        Mockito.when(reservationService.getReservationById(reservationId))
                .thenReturn(Optional.of(pending))
                .thenReturn(Optional.of(cancelled));

        // 2. Act
        final Reservation result = workflowService.patchReservation(
                riderId, reservationId, Reservation.Status.CANCELLED_BY_RIDER, null, null, null);

        // 3. Assert
        Assertions.assertEquals(Reservation.Status.CANCELLED_BY_RIDER, result.getStatus());
    }
}
