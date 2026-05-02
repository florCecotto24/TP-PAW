package ar.edu.itba.paw.services;

import java.util.List;
import ar.edu.itba.paw.models.email.EmailVerificationCodeEmailPayload;
import ar.edu.itba.paw.models.email.MigratedUserPasswordEmailPayload;
import ar.edu.itba.paw.models.email.PasswordResetCodeEmailPayload;
import ar.edu.itba.paw.models.email.ReservationCancellationEmailPayload;
import ar.edu.itba.paw.models.email.ReservationMailPayload;
import ar.edu.itba.paw.models.email.RiderCarReturnEmailPayload;
import ar.edu.itba.paw.models.email.OwnerPaymentProofReceivedEmailPayload;
import ar.edu.itba.paw.models.email.ListingPausedMissingCbuOwnerEmailPayload;
import ar.edu.itba.paw.models.email.RiderReviewInviteEmailPayload;

public interface EmailService {

    void sendReservationConfirmationEmail(ReservationMailPayload payload);

    /** Rider only: after payment proof upload; includes full pickup address (street number). */
    void sendRiderReservationConfirmedAfterPaymentProof(ReservationMailPayload payload);

    void sendReservationCancellationEmail(ReservationCancellationEmailPayload payload);

    void sendEmailVerificationCode(EmailVerificationCodeEmailPayload payload);

    void sendMigratedUserPassword(MigratedUserPasswordEmailPayload payload);

    void sendPasswordResetCode(PasswordResetCodeEmailPayload payload);

    void sendReservationReminderEmail(ReservationMailPayload payload);

    void sendListingDeletionEmail(List<ReservationMailPayload> reservationsToCancel);

    void sendRiderReturnReminderEmail(RiderCarReturnEmailPayload payload);

    void sendRiderReturnCheckoutEmail(RiderCarReturnEmailPayload payload);

    void sendRiderReviewInviteEmail(RiderReviewInviteEmailPayload payload);

    void sendOwnerPaymentProofReceivedEmail(OwnerPaymentProofReceivedEmailPayload payload);

    void sendRiderDuePaymentProofEmail(ReservationMailPayload payload);

    /** Owner notified when an active listing was paused because CBU was removed or invalid. */
    void sendListingPausedDueToMissingCbu(ListingPausedMissingCbuOwnerEmailPayload payload);
}
