package ar.edu.itba.paw.services.reservation;


import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.itba.paw.exception.MessageKeys;
import ar.edu.itba.paw.exception.reservation.RiderReservationException;
import ar.edu.itba.paw.models.domain.Reservation;
import ar.edu.itba.paw.models.domain.StoredFile;
import ar.edu.itba.paw.models.domain.User;
import ar.edu.itba.paw.models.dto.file.BinaryContent;
import ar.edu.itba.paw.models.email.ReservationMailPayload;
import ar.edu.itba.paw.policy.PaymentReceiptUploadPolicy;
import ar.edu.itba.paw.util.ReservationMailComposer;
import ar.edu.itba.paw.util.ReservationServiceSupport;

import ar.edu.itba.paw.services.file.StoredFileService;
import ar.edu.itba.paw.services.user.UserService;
/**
 * Money side of the reservation lifecycle: rider payment receipt uploads, owner refund
 * receipt uploads, the reminder jobs that chase missing receipts, the sweep that cancels
 * reservations whose payment-proof deadline lapsed, and the refund-overdue sweep that blocks
 * owners. Read queries come from {@link ReservationQueryService}; emails from
 * {@link ReservationMailComposer}.
 *
 * <p>Architectural rule: this service no longer touches {@code ReservationDao} — row reads
 * and mutations are funneled through {@link ReservationService} (the sole DAO owner).</p>
 */
@Service
public final class ReservationPaymentServiceImpl implements ReservationPaymentService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReservationPaymentServiceImpl.class);

    private final ReservationService reservationService;
    private final UserService userService;
    private final StoredFileService storedFileService;
    private final PaymentReceiptUploadPolicy paymentReceiptUploadPolicy;
    private final ReservationServiceSupport support;
    private final ReservationQueryService queryService;
    private final ReservationMailComposer mailComposer;

    @Autowired
    public ReservationPaymentServiceImpl(
            @Lazy final ReservationService reservationService,
            final UserService userService,
            final StoredFileService storedFileService,
            final PaymentReceiptUploadPolicy paymentReceiptUploadPolicy,
            final ReservationServiceSupport support,
            final ReservationQueryService queryService,
            final ReservationMailComposer mailComposer) {
        this.reservationService = reservationService;
        this.userService = userService;
        this.storedFileService = storedFileService;
        this.paymentReceiptUploadPolicy = paymentReceiptUploadPolicy;
        this.support = support;
        this.queryService = queryService;
        this.mailComposer = mailComposer;
    }

    // ---------------------------------------------------------------------------------------
    // Payment proof expiration sweep
    // ---------------------------------------------------------------------------------------

    /**
     * Batch cancellation for expired proof deadlines. Loops in this method's single
     * {@code @Transactional} boundary so all per-reservation cancellations participate in the
     * same transaction.
     */
    @Override
    @Transactional
    public void cancelExpiredPendingPaymentReservations() {
        final Reservation.Status status = Reservation.Status.CANCELLED_DUE_TO_MISSING_PAYMENT_PROOF;
        int cancelled = 0;
        for (final Reservation r : reservationService.findPendingPaymentPastDeadline(OffsetDateTime.now(ZoneOffset.UTC))) {
            reservationService.updateReservationStatus(r.getId(), status.name().toLowerCase(Locale.ROOT));
            reservationService.getReservationById(r.getId()).ifPresent(
                    refreshed -> mailComposer.sendCancellationEmail(refreshed, true));
            cancelled++;
        }
        LOGGER.atInfo().addArgument(cancelled).log("Payment proof deadline sweep: cancelled {} pending reservation(s)");
    }

    // ---------------------------------------------------------------------------------------
    // Payment receipt upload
    // ---------------------------------------------------------------------------------------

    @Override
    @Transactional
    public void attachPaymentReceipt(
            final long riderId,
            final long reservationId,
            final String originalFilename,
            final String contentType,
            final byte[] data) {
        validateReceiptUpload(originalFilename, contentType, data, MessageKeys.RESERVATION_PAYMENT_RECEIPT_INVALID,
                MessageKeys.RESERVATION_PAYMENT_RECEIPT_TOO_LARGE);
        final Reservation r = queryService.getRiderReservationById(riderId, reservationId)
                .orElseThrow(() -> new RiderReservationException(MessageKeys.RESERVATION_RIDER_LISTING_NOT_FOUND));
        if (r.getStatus() != Reservation.Status.PENDING) {
            throw new RiderReservationException(MessageKeys.RESERVATION_PAYMENT_RECEIPT_INVALID);
        }
        final StoredFile file = storedFileService.create(riderId, originalFilename, contentType, data);
        final int updated = reservationService.attachPaymentReceiptAndAccept(reservationId, riderId, file.getId());
        if (updated == 0) {
            throw new RiderReservationException(MessageKeys.RESERVATION_PAYMENT_RECEIPT_INVALID);
        }
        final Reservation afterAttach = queryService.getRiderReservationById(riderId, reservationId).orElse(r);
        mailComposer.sendOwnerPaymentProofReceived(reservationId, riderId, afterAttach);
        mailComposer.sendRiderReservationConfirmedAfterPaymentProof(riderId, afterAttach);
        LOGGER.atInfo().addArgument(riderId).addArgument(reservationId)
                .log("Rider riderId={} attached payment receipt for reservation id={}");
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<StoredFile> findPaymentReceiptForParticipant(final long userId, final long reservationId) {
        final Optional<Reservation> resOpt = findReservationForParticipant(userId, reservationId);
        if (resOpt.isEmpty()) {
            return Optional.empty();
        }
        final Reservation r = resOpt.get();
        return r.getPaymentReceiptFileId()
                .flatMap(storedFileService::findById)
                .filter(sf -> sf.getUploaderUserId() == r.getRiderId());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<BinaryContent> findPaymentReceiptContentForParticipant(
            final long userId, final long reservationId) {
        // Map to a detached value object inside this transaction so the controller doesn't
        // depend on the JPA entity (issue #16).
        return findPaymentReceiptForParticipant(userId, reservationId)
                .map(sf -> new BinaryContent(sf.getData(), sf.getContentType(), sf.getFileName()));
    }

    // ---------------------------------------------------------------------------------------
    // Refund receipt upload and owner auto-unblock
    // ---------------------------------------------------------------------------------------

    @Override
    @Transactional
    public void attachRefundReceiptByOwner(
            final long ownerUserId,
            final long reservationId,
            final String originalFilename,
            final String contentType,
            final byte[] data) {
        validateReceiptUpload(originalFilename, contentType, data, MessageKeys.RESERVATION_REFUND_RECEIPT_INVALID,
                MessageKeys.RESERVATION_REFUND_RECEIPT_TOO_LARGE);
        final Reservation r = queryService.getOwnerReservationById(ownerUserId, reservationId)
                .orElseThrow(() -> new RiderReservationException(MessageKeys.RESERVATION_REFUND_RECEIPT_INVALID));
        if (r.getStatus() != Reservation.Status.CANCELLED_BY_OWNER
                && r.getStatus() != Reservation.Status.CANCELLED_BY_RIDER) {
            throw new RiderReservationException(MessageKeys.RESERVATION_REFUND_RECEIPT_INVALID);
        }
        if (!r.isPaymentRefundRequired() || r.getPaymentRefundReceiptFileId().isPresent()) {
            throw new RiderReservationException(MessageKeys.RESERVATION_REFUND_RECEIPT_INVALID);
        }
        final StoredFile file = storedFileService.create(ownerUserId, originalFilename, contentType, data);
        final int updated = reservationService.attachRefundReceipt(reservationId, ownerUserId, file.getId());
        if (updated == 0) {
            throw new RiderReservationException(MessageKeys.RESERVATION_REFUND_RECEIPT_INVALID);
        }
        final Reservation after = queryService.getOwnerReservationById(ownerUserId, reservationId).orElse(r);
        mailComposer.sendRiderRefundProofReceived(reservationId, after);
        // Owner becomes eligible for auto-unblock as soon as no overdue refund proof remains.
        unblockOwnerIfNoMoreRefundOverdue(ownerUserId);
        LOGGER.atInfo().addArgument(ownerUserId).addArgument(reservationId)
                .log("Owner ownerUserId={} attached refund receipt for reservation id={}");
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<StoredFile> findRefundReceiptForParticipant(final long userId, final long reservationId) {
        final Optional<Reservation> resOpt = findReservationForParticipant(userId, reservationId);
        if (resOpt.isEmpty()) {
            return Optional.empty();
        }
        final Reservation r = resOpt.get();
        final Optional<Long> fileIdOpt = r.getPaymentRefundReceiptFileId();
        if (fileIdOpt.isEmpty()) {
            return Optional.empty();
        }
        final Optional<StoredFile> fileOpt = storedFileService.findById(fileIdOpt.get());
        if (fileOpt.isEmpty()) {
            return Optional.empty();
        }
        final Optional<User> ownerOpt = support.resolveOwnerFromReservation(r);
        if (ownerOpt.isEmpty() || fileOpt.get().getUploaderUserId() != ownerOpt.get().getId()) {
            return Optional.empty();
        }
        return fileOpt;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<BinaryContent> findRefundReceiptContentForParticipant(
            final long userId, final long reservationId) {
        return findRefundReceiptForParticipant(userId, reservationId)
                .map(sf -> new BinaryContent(sf.getData(), sf.getContentType(), sf.getFileName()));
    }

    private Optional<Reservation> findReservationForParticipant(final long userId, final long reservationId) {
        final Optional<Reservation> asRider = queryService.getRiderReservationById(userId, reservationId);
        return asRider.isPresent() ? asRider : queryService.getOwnerReservationById(userId, reservationId);
    }

    private void validateReceiptUpload(
            final String originalFilename,
            final String contentType,
            final byte[] data,
            final String invalidKey,
            final String tooLargeKey) {
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new RiderReservationException(invalidKey);
        }
        if (!StoredFile.isAllowedPaymentReceiptContentType(contentType)) {
            throw new RiderReservationException(invalidKey);
        }
        final int len = data == null ? 0 : data.length;
        if (len == 0) {
            throw new RiderReservationException(invalidKey);
        }
        if (len > paymentReceiptUploadPolicy.getMaxBytes()) {
            throw new RiderReservationException(tooLargeKey, paymentReceiptUploadPolicy.getMaxMegabytesRoundedUp());
        }
    }

    /**
     * Unblocks {@code ownerUserId} when no refund-proof deadline remains overdue. Safe to call
     * after every refund-proof upload; idempotent if the owner is not blocked.
     */
    private void unblockOwnerIfNoMoreRefundOverdue(final long ownerUserId) {
        try {
            final OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
            final long remaining = reservationService.countOverdueRefundProofsForOwner(ownerUserId, now);
            if (remaining > 0L) {
                return;
            }
            final Optional<User> ownerOpt = userService.getUserById(ownerUserId);
            if (ownerOpt.isEmpty() || !ownerOpt.get().isBlocked()) {
                return;
            }
            userService.unblockUser(ownerUserId);
            LOGGER.atInfo().addArgument(ownerUserId)
                    .log("Auto-unblocked ownerId={} after all refund-proof debts cleared");
        } catch (final RuntimeException e) {
            LOGGER.atError().setCause(e).addArgument(ownerUserId)
                    .log("Failed to evaluate owner auto-unblock after refund-proof upload (ownerId={})");
        }
    }

    // ---------------------------------------------------------------------------------------
    // Reminder jobs
    // ---------------------------------------------------------------------------------------

    @Override
    @Transactional
    public void dispatchDuePaymentProofReminderEmails() {
        final OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        final List<Reservation> candidates = reservationService.findReservationsWithDuePendingPaymentProof(now);
        LOGGER.atInfo().addArgument(candidates.size()).log("Due payment proof reminder run: {} candidate reservation(s)");
        int queued = 0;
        for (final Reservation reservation : candidates) {
            final Optional<ReservationMailPayload> payload = mailComposer.buildDuePaymentProofReminder(reservation);
            if (payload.isEmpty()) {
                LOGGER.atWarn().addArgument(reservation.getId())
                        .log("Skipping due payment proof reminder: missing data (reservation id={})");
                continue;
            }
            if (reservationService.claimPendingPaymentProofEmailSent(reservation.getId()) == 0) {
                continue;
            }
            try {
                mailComposer.sendDuePaymentProofReminder(payload.get());
                queued++;
            } catch (final RuntimeException e) {
                LOGGER.atError().setCause(e).addArgument(reservation.getId())
                        .log("Failed to queue due payment proof reminder email (reservation id={})");
            }
        }
        LOGGER.atInfo().addArgument(queued).log("Due payment proof reminder run: queued {} email(s)");
    }

    @Override
    @Transactional
    public void dispatchDueRefundProofReminderEmails() {
        final OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        final List<Reservation> candidates = reservationService.findReservationsWithDuePendingRefundProof(now);
        LOGGER.atInfo().addArgument(candidates.size()).log("Due refund proof reminder run: {} candidate reservation(s)");
        int queued = 0;
        for (final Reservation reservation : candidates) {
            if (reservationService.claimPendingRefundEmailSent(reservation.getId()) == 0) {
                continue;
            }
            try {
                sendOwnerRefundProofObligationEmail(reservation, true);
                queued++;
            } catch (final RuntimeException e) {
                LOGGER.atError().setCause(e).addArgument(reservation.getId())
                        .log("Failed to queue due refund proof reminder email (reservation id={})");
            }
        }
        LOGGER.atInfo().addArgument(queued).log("Due refund proof reminder run: queued {} email(s)");
    }

    @Override
    @Transactional
    public void sendOwnerRefundProofObligationEmail(final Reservation reservation, final boolean dueReminder) {
        mailComposer.sendOwnerRefundProofObligation(reservation, dueReminder);
    }

    // ---------------------------------------------------------------------------------------
    // Refund overdue sweep
    // ---------------------------------------------------------------------------------------

    @Override
    @Transactional
    public void sweepRefundOverdueAndBlockOwners() {
        final OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        final List<Reservation> overdue = reservationService.findReservationsWithOverdueRefundProof(now);
        LOGGER.atInfo().addArgument(overdue.size()).log("Refund-overdue sweep: {} reservation(s) with lapsed deadline");
        if (overdue.isEmpty()) {
            return;
        }
        // Group reservations by owner so we issue a single block + single email per owner.
        final Map<Long, List<Reservation>> byOwner = new LinkedHashMap<>();
        for (final Reservation r : overdue) {
            support.resolveOwnerFromReservation(r)
                    .ifPresent(o -> byOwner.computeIfAbsent(o.getId(), k -> new ArrayList<>()).add(r));
        }
        int blockedCount = 0;
        for (final Map.Entry<Long, List<Reservation>> entry : byOwner.entrySet()) {
            final long ownerId = entry.getKey();
            try {
                final Optional<User> ownerOpt = userService.getUserById(ownerId);
                if (ownerOpt.isEmpty()) {
                    continue;
                }
                final User owner = ownerOpt.get();
                if (owner.isBlocked()) {
                    continue;
                }
                userService.blockUser(ownerId);
                blockedCount++;
                mailComposer.sendOwnerBlockedEmail(owner, entry.getValue());
            } catch (final RuntimeException e) {
                LOGGER.atError().setCause(e).addArgument(ownerId)
                        .log("Failed to block ownerId={} during refund-overdue sweep");
            }
        }
        LOGGER.atInfo().addArgument(blockedCount).log("Refund-overdue sweep: blocked {} owner(s)");
    }
}
