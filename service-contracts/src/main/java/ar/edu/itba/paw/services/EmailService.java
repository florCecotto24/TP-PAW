package ar.edu.itba.paw.services;

import java.util.List;
import java.util.Locale;

import ar.edu.itba.paw.models.email.ReservationConfirmationEmailPayload;
import ar.edu.itba.paw.models.email.RiderCarReturnEmailPayload;
import ar.edu.itba.paw.models.email.OwnerPaymentProofReceivedEmailPayload;
import ar.edu.itba.paw.models.email.RiderReviewInviteEmailPayload;

public interface EmailService {

    void sendReservationConfirmationEmail(ReservationConfirmationEmailPayload payload);

    /** Rider only: after payment proof upload; includes full pickup address (street number). */
    void sendRiderReservationConfirmedAfterPaymentProof(ReservationConfirmationEmailPayload payload);

    void sendReservationCancellationEmail(ReservationConfirmationEmailPayload payload);

    void sendEmailVerificationCode(String to, String code, Locale locale);

    void sendMigratedUserPassword(String to, String plainPassword, Locale locale);

    void sendPasswordResetCode(String to, String code, Locale locale);

    void sendReservationReminderEmail(final ReservationConfirmationEmailPayload payload);

    void sendListingDeletionEmail(List<ReservationConfirmationEmailPayload> reservationsToCancel);

    void sendRiderReturnReminderEmail(RiderCarReturnEmailPayload payload);

    void sendRiderReturnCheckoutEmail(RiderCarReturnEmailPayload payload);

    void sendRiderReviewInviteEmail(RiderReviewInviteEmailPayload payload);

    void sendOwnerPaymentProofReceivedEmail(OwnerPaymentProofReceivedEmailPayload payload);
}
