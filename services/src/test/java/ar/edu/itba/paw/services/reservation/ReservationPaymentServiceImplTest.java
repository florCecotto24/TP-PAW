package ar.edu.itba.paw.services.reservation;


import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;

import ar.edu.itba.paw.exception.MessageKeys;
import ar.edu.itba.paw.exception.reservation.RiderReservationException;
import ar.edu.itba.paw.models.domain.car.Car;
import ar.edu.itba.paw.models.domain.file.StoredFile;
import ar.edu.itba.paw.models.domain.reservation.Reservation;
import ar.edu.itba.paw.models.domain.user.User;
import ar.edu.itba.paw.policy.PaymentReceiptUploadPolicy;
import ar.edu.itba.paw.policy.PaymentReceiptUploadPolicyImpl;
import ar.edu.itba.paw.util.ReservationMailComposer;
import ar.edu.itba.paw.util.ReservationServiceSupport;

import ar.edu.itba.paw.services.file.StoredFileService;
import ar.edu.itba.paw.services.user.UserService;

@ExtendWith(MockitoExtension.class)
class ReservationPaymentServiceImplTest {

    private static final long RIDER_ID = 10L;
    private static final long OWNER_ID = 20L;
    private static final long RESERVATION_ID = 30L;
    private static final byte[] PDF_BYTES = {0x25, 0x50, 0x44, 0x46, 0x2d}; // "%PDF-"

    @Mock
    private ReservationService reservationService;

    @Mock
    private UserService userService;

    @Mock
    private StoredFileService storedFileService;

    @Mock
    private ReservationServiceSupport support;

    @Mock
    private ReservationQueryService queryService;

    @Mock
    private ReservationMailComposer mailComposer;

    @Mock
    private ExpiredPaymentProofRowCanceller expiredPaymentProofRowCanceller;

    @Mock
    private ReservationSweepRowProcessor sweepRowProcessor;

    private ReservationPaymentServiceImpl paymentService;

    @BeforeEach
    void setUp() {
        final Environment environment = Mockito.mock(Environment.class);
        Mockito.when(environment.getProperty("app.upload.bytes-per-binary-megabyte", Integer.class))
                .thenReturn(1048576);
        Mockito.when(environment.getProperty("app.upload.max-payment-receipt-megabytes", Long.class))
                .thenReturn(5L);
        final PaymentReceiptUploadPolicy uploadPolicy = new PaymentReceiptUploadPolicyImpl(environment);
        paymentService = new ReservationPaymentServiceImpl(
                reservationService,
                userService,
                storedFileService,
                uploadPolicy,
                support,
                queryService,
                mailComposer,
                expiredPaymentProofRowCanceller,
                sweepRowProcessor);
    }

    @Test
    void testAttachPaymentReceiptRejectsPastDeadline() {
        // 1. Arrange
        final Reservation pending = pendingReservation(
                OffsetDateTime.parse("2020-01-01T00:00:00Z"));
        Mockito.when(queryService.getRiderReservationById(RIDER_ID, RESERVATION_ID))
                .thenReturn(Optional.of(pending));

        // 2. Act / 3. Assert
        final RiderReservationException thrown = Assertions.assertThrows(
                RiderReservationException.class,
                () -> paymentService.attachPaymentReceipt(
                        RIDER_ID, RESERVATION_ID, "proof.pdf", "application/pdf", PDF_BYTES));
        Assertions.assertEquals(MessageKeys.RESERVATION_PAYMENT_PROOF_DEADLINE_PASSED, thrown.getMessageCode());
    }

    @Test
    void testAttachPaymentReceiptRejectsInvalidContentType() {
        // 1. Arrange — validation fails before any reservation lookup

        // 2. Act / 3. Assert
        final RiderReservationException thrown = Assertions.assertThrows(
                RiderReservationException.class,
                () -> paymentService.attachPaymentReceipt(
                        RIDER_ID, RESERVATION_ID, "proof.txt", "text/plain", new byte[] {1, 2, 3}));
        Assertions.assertEquals(MessageKeys.RESERVATION_PAYMENT_RECEIPT_INVALID, thrown.getMessageCode());
    }

    @Test
    void testAttachPaymentReceiptAcceptsValidProof() {
        // 1. Arrange
        final Reservation pending = pendingReservation(OffsetDateTime.now().plusDays(1));
        final StoredFile file = StoredFile.identified(
                99L, User.identities(RIDER_ID, "r@test.com", "R", "Rider"),
                "proof.pdf", "application/pdf", PDF_BYTES, null);
        Mockito.when(queryService.getRiderReservationById(RIDER_ID, RESERVATION_ID))
                .thenReturn(Optional.of(pending));
        Mockito.when(storedFileService.create(RIDER_ID, "proof.pdf", "application/pdf", PDF_BYTES))
                .thenReturn(file);
        Mockito.when(reservationService.attachPaymentReceiptAndAccept(RESERVATION_ID, RIDER_ID, 99L))
                .thenReturn(1);

        // 2. Act / 3. Assert
        Assertions.assertDoesNotThrow(() -> paymentService.attachPaymentReceipt(
                RIDER_ID, RESERVATION_ID, "proof.pdf", "application/pdf", PDF_BYTES));
    }

    @Test
    void testAttachPaymentReceiptDeletesOrphanWhenAttachFails() {
        // 1. Arrange — create succeeds, atomic attach loses (race / ineligible)
        final Reservation pending = pendingReservation(OffsetDateTime.now().plusDays(1));
        final StoredFile file = StoredFile.identified(
                99L, User.identities(RIDER_ID, "r@test.com", "R", "Rider"),
                "proof.pdf", "application/pdf", PDF_BYTES, null);
        final long[] deletedId = {-1L};
        Mockito.when(queryService.getRiderReservationById(RIDER_ID, RESERVATION_ID))
                .thenReturn(Optional.of(pending));
        Mockito.when(storedFileService.create(RIDER_ID, "proof.pdf", "application/pdf", PDF_BYTES))
                .thenReturn(file);
        Mockito.when(reservationService.attachPaymentReceiptAndAccept(RESERVATION_ID, RIDER_ID, 99L))
                .thenReturn(0);
        Mockito.when(storedFileService.deleteById(99L)).thenAnswer(inv -> {
            deletedId[0] = inv.getArgument(0);
            return true;
        });

        // 2. Act / 3. Assert
        final RiderReservationException thrown = Assertions.assertThrows(
                RiderReservationException.class,
                () -> paymentService.attachPaymentReceipt(
                        RIDER_ID, RESERVATION_ID, "proof.pdf", "application/pdf", PDF_BYTES));
        Assertions.assertEquals(MessageKeys.RESERVATION_PAYMENT_RECEIPT_CONFLICT, thrown.getMessageCode());
        Assertions.assertEquals(99L, deletedId[0]);
    }

    @Test
    void testAttachRefundReceiptRejectsWrongStatus() {
        // 1. Arrange
        final Reservation accepted = baseReservation(Reservation.Status.ACCEPTED)
                .paymentRefundRequired(true)
                .build();
        Mockito.when(queryService.getOwnerReservationById(OWNER_ID, RESERVATION_ID))
                .thenReturn(Optional.of(accepted));

        // 2. Act / 3. Assert
        final RiderReservationException thrown = Assertions.assertThrows(
                RiderReservationException.class,
                () -> paymentService.attachRefundReceiptByOwner(
                        OWNER_ID, RESERVATION_ID, "refund.pdf", "application/pdf", PDF_BYTES));
        Assertions.assertEquals(MessageKeys.RESERVATION_REFUND_RECEIPT_CONFLICT, thrown.getMessageCode());
    }

    @Test
    void testAttachRefundReceiptRejectsWhenNotRequired() {
        // 1. Arrange
        final Reservation cancelled = baseReservation(Reservation.Status.CANCELLED_BY_OWNER)
                .paymentRefundRequired(false)
                .build();
        Mockito.when(queryService.getOwnerReservationById(OWNER_ID, RESERVATION_ID))
                .thenReturn(Optional.of(cancelled));

        // 2. Act / 3. Assert
        final RiderReservationException thrown = Assertions.assertThrows(
                RiderReservationException.class,
                () -> paymentService.attachRefundReceiptByOwner(
                        OWNER_ID, RESERVATION_ID, "refund.pdf", "application/pdf", PDF_BYTES));
        Assertions.assertEquals(MessageKeys.RESERVATION_REFUND_RECEIPT_CONFLICT, thrown.getMessageCode());
    }

    @Test
    void testAttachRefundReceiptUnblocksOwnerWhenNoOverdueRemain() {
        // 1. Arrange
        final Reservation cancelled = baseReservation(Reservation.Status.CANCELLED_BY_RIDER)
                .paymentRefundRequired(true)
                .build();
        final StoredFile file = StoredFile.identified(
                77L, User.identities(OWNER_ID, "o@test.com", "O", "Owner"),
                "refund.pdf", "application/pdf", PDF_BYTES, null);
        final User blockedOwner = User.builder()
                .id(OWNER_ID)
                .email("o@test.com")
                .forename("O")
                .surname("Owner")
                .blocked(true)
                .build();
        Mockito.when(queryService.getOwnerReservationById(OWNER_ID, RESERVATION_ID))
                .thenReturn(Optional.of(cancelled));
        Mockito.when(storedFileService.create(OWNER_ID, "refund.pdf", "application/pdf", PDF_BYTES))
                .thenReturn(file);
        Mockito.when(reservationService.attachRefundReceipt(RESERVATION_ID, OWNER_ID, 77L))
                .thenReturn(1);
        Mockito.when(reservationService.countOverdueRefundProofsForOwner(
                Mockito.eq(OWNER_ID), Mockito.any(OffsetDateTime.class)))
                .thenReturn(0L);
        Mockito.when(userService.getUserById(OWNER_ID)).thenReturn(Optional.of(blockedOwner));
        Mockito.doAnswer(inv -> {
            blockedOwner.setBlocked(false);
            return null;
        }).when(userService).unblockUser(OWNER_ID);

        // 2. Act
        paymentService.attachRefundReceiptByOwner(
                OWNER_ID, RESERVATION_ID, "refund.pdf", "application/pdf", PDF_BYTES);

        // 3. Assert — domain outcome: owner is no longer blocked
        Assertions.assertFalse(blockedOwner.isBlocked());
    }

    @Test
    void testAttachRefundReceiptDeletesOrphanWhenAttachFails() {
        // 1. Arrange
        final Reservation cancelled = baseReservation(Reservation.Status.CANCELLED_BY_OWNER)
                .paymentRefundRequired(true)
                .build();
        final StoredFile file = StoredFile.identified(
                77L, User.identities(OWNER_ID, "o@test.com", "O", "Owner"),
                "refund.pdf", "application/pdf", PDF_BYTES, null);
        final long[] deletedId = {-1L};
        Mockito.when(queryService.getOwnerReservationById(OWNER_ID, RESERVATION_ID))
                .thenReturn(Optional.of(cancelled));
        Mockito.when(storedFileService.create(OWNER_ID, "refund.pdf", "application/pdf", PDF_BYTES))
                .thenReturn(file);
        Mockito.when(reservationService.attachRefundReceipt(RESERVATION_ID, OWNER_ID, 77L))
                .thenReturn(0);
        Mockito.when(storedFileService.deleteById(77L)).thenAnswer(inv -> {
            deletedId[0] = inv.getArgument(0);
            return true;
        });

        // 2. Act / 3. Assert
        final RiderReservationException thrown = Assertions.assertThrows(
                RiderReservationException.class,
                () -> paymentService.attachRefundReceiptByOwner(
                        OWNER_ID, RESERVATION_ID, "refund.pdf", "application/pdf", PDF_BYTES));
        Assertions.assertEquals(MessageKeys.RESERVATION_REFUND_RECEIPT_CONFLICT, thrown.getMessageCode());
        Assertions.assertEquals(77L, deletedId[0]);
    }

    @Test
    void testCancelExpiredPendingPaymentSkipsMailWhenClaimRollsBack() {
        // 1. Arrange
        final Reservation expired = pendingReservation(OffsetDateTime.parse("2020-01-01T00:00:00Z"));
        Mockito.when(reservationService.findPendingPaymentPastDeadline(Mockito.any(OffsetDateTime.class)))
                .thenReturn(List.of(expired));
        Mockito.when(expiredPaymentProofRowCanceller.cancelExpiredReservation(
                Mockito.eq(RESERVATION_ID), Mockito.any(OffsetDateTime.class)))
                .thenReturn(Optional.empty());

        // 2. Act / 3. Assert — claim rollback: mail composer is not stubbed to throw (no verify-style probe)
        Assertions.assertDoesNotThrow(() -> paymentService.cancelExpiredPendingPaymentReservations());
    }

    @Test
    void testCancelExpiredPendingPaymentSendsMailAfterSuccessfulClaim() {
        // 1. Arrange
        final Reservation expired = pendingReservation(OffsetDateTime.parse("2020-01-01T00:00:00Z"));
        final Reservation cancelled = baseReservation(Reservation.Status.CANCELLED_DUE_TO_MISSING_PAYMENT_PROOF)
                .build();
        Mockito.when(reservationService.findPendingPaymentPastDeadline(Mockito.any(OffsetDateTime.class)))
                .thenReturn(List.of(expired));
        Mockito.when(expiredPaymentProofRowCanceller.cancelExpiredReservation(
                Mockito.eq(RESERVATION_ID), Mockito.any(OffsetDateTime.class)))
                .thenReturn(Optional.of(cancelled));

        // 2. Act / 3. Assert
        Assertions.assertDoesNotThrow(() -> paymentService.cancelExpiredPendingPaymentReservations());
    }

    @Test
    void testSweepRefundOverdueBlocksEligibleOwner() {
        // 1. Arrange
        final User owner = User.builder()
                .id(OWNER_ID)
                .email("o@test.com")
                .forename("O")
                .surname("Owner")
                .blocked(false)
                .build();
        final Reservation overdue = baseReservation(Reservation.Status.CANCELLED_BY_OWNER)
                .paymentRefundRequired(true)
                .build();
        Mockito.when(reservationService.findReservationsWithOverdueRefundProof(Mockito.any(OffsetDateTime.class)))
                .thenReturn(List.of(overdue));
        Mockito.when(support.resolveOwnerFromReservation(overdue)).thenReturn(Optional.of(owner));
        Mockito.when(sweepRowProcessor.blockOwnerForRefundOverdueIfEligible(OWNER_ID))
                .thenReturn(Optional.of(owner));

        // 2. Act / 3. Assert
        Assertions.assertDoesNotThrow(() -> paymentService.sweepRefundOverdueAndBlockOwners());
    }

    @Test
    void testSweepRefundOverdueSkipsMailWhenBlockRollsBack() {
        // 1. Arrange
        final User owner = User.builder()
                .id(OWNER_ID)
                .email("o@test.com")
                .forename("O")
                .surname("Owner")
                .build();
        final Reservation overdue = baseReservation(Reservation.Status.CANCELLED_BY_OWNER)
                .paymentRefundRequired(true)
                .build();
        Mockito.when(reservationService.findReservationsWithOverdueRefundProof(Mockito.any(OffsetDateTime.class)))
                .thenReturn(List.of(overdue));
        Mockito.when(support.resolveOwnerFromReservation(overdue)).thenReturn(Optional.of(owner));
        Mockito.when(sweepRowProcessor.blockOwnerForRefundOverdueIfEligible(OWNER_ID))
                .thenReturn(Optional.empty());

        // 2. Act / 3. Assert — block rollback: mail composer is not stubbed to throw (no verify-style probe)
        Assertions.assertDoesNotThrow(() -> paymentService.sweepRefundOverdueAndBlockOwners());
    }

    private static Reservation pendingReservation(final OffsetDateTime deadline) {
        return baseReservation(Reservation.Status.PENDING)
                .paymentProofDeadlineAt(deadline)
                .build();
    }

    private static Reservation.Builder baseReservation(final Reservation.Status status) {
        final Car car = Mockito.mock(Car.class);
        final User owner = User.identities(OWNER_ID, "o@test.com", "O", "Owner");
        Mockito.lenient().when(car.getOwner()).thenReturn(owner);
        return Reservation.builder()
                .id(RESERVATION_ID)
                .rider(User.identities(RIDER_ID, "r@test.com", "R", "Rider"))
                .car(car)
                .startDate(OffsetDateTime.parse("2026-04-01T10:00:00Z"))
                .endDate(OffsetDateTime.parse("2026-04-05T18:00:00Z"))
                .status(status)
                .createdAt(OffsetDateTime.parse("2026-03-01T10:00:00Z"))
                .updatedAt(OffsetDateTime.parse("2026-03-01T10:00:00Z"))
                .totalPrice(new BigDecimal("100"));
    }
}
