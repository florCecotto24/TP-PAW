package ar.edu.itba.paw.services;

import java.util.List;
import ar.edu.itba.paw.models.email.EmailVerificationCodeEmailPayload;
import ar.edu.itba.paw.models.email.MigratedUserPasswordEmailPayload;
import ar.edu.itba.paw.models.email.PasswordResetCodeEmailPayload;
import ar.edu.itba.paw.models.email.ReservationCancellationEmailPayload;
import ar.edu.itba.paw.models.email.ReservationMailPayload;
import ar.edu.itba.paw.models.email.RiderCarReturnEmailPayload;
import ar.edu.itba.paw.models.email.OwnerPaymentProofReceivedEmailPayload;
import ar.edu.itba.paw.models.email.OwnerRefundProofObligationEmailPayload;
import ar.edu.itba.paw.models.email.OwnerBlockedEmailPayload;
import ar.edu.itba.paw.models.email.RiderRefundProofReceivedEmailPayload;
import ar.edu.itba.paw.models.email.CarPausedMissingCbuOwnerEmailPayload;
import ar.edu.itba.paw.models.email.RiderReviewInviteEmailPayload;
import ar.edu.itba.paw.models.email.ReservationChatDigestEmailPayload;

/**
 * HTML mail dispatch (implementation is typically asynchronous). Callers supply fully-built payloads; this layer
 * renders templates and sends through JavaMail.
 * Implementations do not use application DAOs; delivery is JavaMail plus Thymeleaf templates and message bundles.
 */
public interface EmailService {

    /** Rider and owner copies after a new reservation is created. */
    void sendReservationConfirmationEmail(ReservationMailPayload payload);

    /** Rider only: after uploading payment proof; includes full pickup address (street number). */
    void sendRiderReservationConfirmedAfterPaymentProof(ReservationMailPayload payload);

    /** Rider and owner copies when a reservation is cancelled. */
    void sendReservationCancellationEmail(ReservationCancellationEmailPayload payload);

    /** Account registration / email verification code. */
    void sendEmailVerificationCode(EmailVerificationCodeEmailPayload payload);

    /** One-time plain password for legacy migrated accounts. */
    void sendMigratedUserPassword(MigratedUserPasswordEmailPayload payload);

    /** Password reset flow: code and link. */
    void sendPasswordResetCode(PasswordResetCodeEmailPayload payload);

    /** Pickup reminder to the rider before the rental starts. */
    void sendReservationReminderEmail(ReservationMailPayload payload);

    /** Cancels each rider in the list, then notifies the owner that the listing was removed. */
    void sendListingDeletionEmail(List<ReservationMailPayload> reservationsToCancel);

    /** Reminds the rider to return the vehicle before checkout. */
    void sendRiderReturnReminderEmail(RiderCarReturnEmailPayload payload);

    /** Notifies the rider at checkout when the car was not marked returned. */
    void sendRiderReturnCheckoutEmail(RiderCarReturnEmailPayload payload);

    /** Post-rental optional review invitation to the rider. */
    void sendRiderReviewInviteEmail(RiderReviewInviteEmailPayload payload);

    /** Owner notified that the rider uploaded a payment receipt. */
    void sendOwnerPaymentProofReceivedEmail(OwnerPaymentProofReceivedEmailPayload payload);

    /** Rider reminded to upload payment proof before the deadline. */
    void sendRiderDuePaymentProofEmail(ReservationMailPayload payload);

    /** Owner must upload refund transfer proof after cancelling a confirmed paid reservation (initial or reminder). */
    void sendOwnerRefundProofObligationEmail(OwnerRefundProofObligationEmailPayload payload);
    /** Notifies the owner that their account was blocked because at least one refund-proof deadline lapsed. */
    void sendOwnerBlockedEmail(OwnerBlockedEmailPayload payload);

    /** Rider notified that the host uploaded a refund receipt for a cancelled reservation. */
    void sendRiderRefundProofReceivedEmail(RiderRefundProofReceivedEmailPayload payload);

    /** Owner notified when an active listing was paused because CBU was removed or invalid. */
    void sendListingPausedDueToMissingCbu(CarPausedMissingCbuOwnerEmailPayload payload);

    /** Owner notified when a platform administrator pauses their listing. */
    void sendCarPausedByAdmin(ar.edu.itba.paw.models.email.CarPausedByAdminOwnerEmailPayload payload);

    /** Owner notified when a platform administrator rejects the catalog entry (brand, model, or both) used by their car. */
    void sendCarRejectedByAdmin(ar.edu.itba.paw.models.email.CarRejectedByAdminOwnerEmailPayload payload);

    /** Owner notified when a platform administrator validates the catalog entry (brand, model, or both) used by their car. */
    void sendCarValidatedByAdmin(ar.edu.itba.paw.models.email.CarValidatedByAdminOwnerEmailPayload payload);

    /** Hourly digest of reservation chat messages for one recipient. */
    void sendReservationChatDigestEmail(ReservationChatDigestEmailPayload payload);
}
