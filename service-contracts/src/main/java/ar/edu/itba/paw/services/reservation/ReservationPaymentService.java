package ar.edu.itba.paw.services.reservation;


import java.util.Optional;

import ar.edu.itba.paw.models.domain.Reservation;
import ar.edu.itba.paw.models.domain.StoredFile;
import ar.edu.itba.paw.models.dto.file.BinaryContent;

/**
 * Money side of the reservation lifecycle: rider payment receipts, owner refund proofs,
 * the scheduled jobs that chase missing payment / refund evidence, and the owner blocking
 * sweep triggered by overdue refunds. The rest of the rental flow lives in
 * {@link ReservationWorkflowService}.
 *
 * <p>The legacy {@link ReservationService} facade still exposes this surface to existing
 * callers; new consumers should depend on this interface directly.
 */
public interface ReservationPaymentService {

    /**
     * Rider uploads a payment receipt file; validates rider, state, and size policy, persists
     * file metadata, and notifies the owner.
     */
    void attachPaymentReceipt(long riderId, long reservationId, String originalFilename, String contentType, byte[] data);

    /**
     * Payment proof if the viewer is the rider or the owner of the car and the reservation has
     * an associated file.
     */
    Optional<StoredFile> findPaymentReceiptForParticipant(long userId, long reservationId);

    /**
     * Same scoping as {@link #findPaymentReceiptForParticipant} but returns a detached
     * {@link BinaryContent} value object so download endpoints don't leak the JPA entity.
     */
    Optional<BinaryContent> findPaymentReceiptContentForParticipant(long userId, long reservationId);

    /**
     * Owner uploads refund transfer proof for a cancelled confirmed reservation that requires
     * refund documentation.
     */
    void attachRefundReceiptByOwner(long ownerUserId, long reservationId, String originalFilename, String contentType, byte[] data);

    /** Refund receipt file when the viewer is rider or owner on a cancelled reservation that has one. */
    Optional<StoredFile> findRefundReceiptForParticipant(long userId, long reservationId);

    /**
     * Same scoping as {@link #findRefundReceiptForParticipant} but returns a detached
     * {@link BinaryContent} value object so download endpoints don't leak the JPA entity.
     */
    Optional<BinaryContent> findRefundReceiptContentForParticipant(long userId, long reservationId);

    /**
     * Batch job: cancels pending reservations whose payment-proof deadline passed without a
     * receipt, and notifies.
     */
    void cancelExpiredPendingPaymentReservations();

    /**
     * Scheduled job: sends payment-proof deadline reminders to riders whose deadline falls
     * within the configured lead window.
     */
    void dispatchDuePaymentProofReminderEmails();

    /**
     * Scheduled job: reminds the host to upload refund transfer proof before the deadline
     * (same lead window as payment proof reminders).
     */
    void dispatchDueRefundProofReminderEmails();

    /**
     * Scheduled job: blocks owners whose refund-proof deadlines have already lapsed without an
     * uploaded receipt. Idempotent — owners already blocked are skipped. A single email is
     * enqueued per newly-blocked owner with the full list of overdue reservations.
     */
    void sweepRefundOverdueAndBlockOwners();

    /**
     * Queues the "you must upload a refund receipt within X" email for the owner of
     * {@code reservation}. Called by {@link ReservationWorkflowService#cancelReservationAsParticipant}
     * the first time a confirmed reservation flips to a refund-required cancellation. Pass
     * {@code dueReminder=true} from the reminder sweep to render the reminder copy variant.
     */
    void sendOwnerRefundProofObligationEmail(Reservation reservation, boolean dueReminder);
}
