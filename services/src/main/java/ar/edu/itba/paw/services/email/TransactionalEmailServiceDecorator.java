package ar.edu.itba.paw.services.email;

import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import ar.edu.itba.paw.models.email.admin.AdminInvitationEmailPayload;
import ar.edu.itba.paw.models.email.admin.AdminPromotedEmailPayload;
import ar.edu.itba.paw.models.email.listing.CarPausedByAdminOwnerEmailPayload;
import ar.edu.itba.paw.models.email.listing.CarPausedMissingCbuOwnerEmailPayload;
import ar.edu.itba.paw.models.email.listing.CarPausedMissingIdentityOwnerEmailPayload;
import ar.edu.itba.paw.models.email.listing.CarRejectedByAdminOwnerEmailPayload;
import ar.edu.itba.paw.models.email.listing.CarValidatedByAdminOwnerEmailPayload;
import ar.edu.itba.paw.models.email.reservation.OwnerBlockedEmailPayload;
import ar.edu.itba.paw.models.email.reservation.OwnerPaymentProofReceivedEmailPayload;
import ar.edu.itba.paw.models.email.reservation.OwnerRefundProofObligationEmailPayload;
import ar.edu.itba.paw.models.email.reservation.ReservationCancellationEmailPayload;
import ar.edu.itba.paw.models.email.reservation.ReservationMailPayload;
import ar.edu.itba.paw.models.email.reservation.RiderCarReturnEmailPayload;
import ar.edu.itba.paw.models.email.reservation.RiderRefundProofReceivedEmailPayload;
import ar.edu.itba.paw.models.email.reservation.RiderReviewInviteEmailPayload;
import ar.edu.itba.paw.models.email.reservation.chat.ReservationChatDigestEmailPayload;
import ar.edu.itba.paw.models.email.user.EmailVerificationCodeEmailPayload;
import ar.edu.itba.paw.models.email.user.MigratedUserPasswordEmailPayload;
import ar.edu.itba.paw.models.email.user.PasswordResetCodeEmailPayload;

/**
 * Transaction-aware {@link EmailService} facade ({@code @Primary}) that defers every dispatch to
 * {@link TransactionSynchronization#afterCommit()} when the caller runs inside a transaction, so a
 * rolled-back write never leaves an already-sent email behind. Outside a transaction (e.g. scheduled
 * jobs that claim-then-send) the delegate is invoked immediately, preserving current behaviour.
 *
 * The delegate is the {@code @Async} {@link EmailServiceImpl}; deferral only controls <em>when</em>
 * the async dispatch is triggered, not the threading of the send itself.
 */
@Service
@Primary
public class TransactionalEmailServiceDecorator implements EmailService {

    private final EmailService delegate;

    public TransactionalEmailServiceDecorator(@Qualifier("emailServiceImpl") final EmailService delegate) {
        this.delegate = delegate;
    }

    private void dispatch(final Runnable send) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    send.run();
                }
            });
        } else {
            send.run();
        }
    }

    @Override
    public void sendReservationConfirmationEmail(final ReservationMailPayload payload) {
        dispatch(() -> delegate.sendReservationConfirmationEmail(payload));
    }

    @Override
    public void sendRiderReservationConfirmedAfterPaymentProof(final ReservationMailPayload payload) {
        dispatch(() -> delegate.sendRiderReservationConfirmedAfterPaymentProof(payload));
    }

    @Override
    public void sendReservationCancellationEmail(final ReservationCancellationEmailPayload payload) {
        dispatch(() -> delegate.sendReservationCancellationEmail(payload));
    }

    @Override
    public void sendEmailVerificationCode(final EmailVerificationCodeEmailPayload payload) {
        dispatch(() -> delegate.sendEmailVerificationCode(payload));
    }

    @Override
    public void sendMigratedUserPassword(final MigratedUserPasswordEmailPayload payload) {
        dispatch(() -> delegate.sendMigratedUserPassword(payload));
    }

    @Override
    public void sendAdminInvitation(final AdminInvitationEmailPayload payload) {
        dispatch(() -> delegate.sendAdminInvitation(payload));
    }

    @Override
    public void sendAdminPromoted(final AdminPromotedEmailPayload payload) {
        dispatch(() -> delegate.sendAdminPromoted(payload));
    }

    @Override
    public void sendPasswordResetCode(final PasswordResetCodeEmailPayload payload) {
        dispatch(() -> delegate.sendPasswordResetCode(payload));
    }

    @Override
    public void sendReservationReminderEmail(final ReservationMailPayload payload) {
        dispatch(() -> delegate.sendReservationReminderEmail(payload));
    }

    @Override
    public void sendListingDeletionEmail(final List<ReservationMailPayload> reservationsToCancel) {
        dispatch(() -> delegate.sendListingDeletionEmail(reservationsToCancel));
    }

    @Override
    public void sendRiderReturnReminderEmail(final RiderCarReturnEmailPayload payload) {
        dispatch(() -> delegate.sendRiderReturnReminderEmail(payload));
    }

    @Override
    public void sendRiderReturnCheckoutEmail(final RiderCarReturnEmailPayload payload) {
        dispatch(() -> delegate.sendRiderReturnCheckoutEmail(payload));
    }

    @Override
    public void sendRiderReviewInviteEmail(final RiderReviewInviteEmailPayload payload) {
        dispatch(() -> delegate.sendRiderReviewInviteEmail(payload));
    }

    @Override
    public void sendOwnerPaymentProofReceivedEmail(final OwnerPaymentProofReceivedEmailPayload payload) {
        dispatch(() -> delegate.sendOwnerPaymentProofReceivedEmail(payload));
    }

    @Override
    public void sendRiderDuePaymentProofEmail(final ReservationMailPayload payload) {
        dispatch(() -> delegate.sendRiderDuePaymentProofEmail(payload));
    }

    @Override
    public void sendOwnerRefundProofObligationEmail(final OwnerRefundProofObligationEmailPayload payload) {
        dispatch(() -> delegate.sendOwnerRefundProofObligationEmail(payload));
    }

    @Override
    public void sendOwnerBlockedEmail(final OwnerBlockedEmailPayload payload) {
        dispatch(() -> delegate.sendOwnerBlockedEmail(payload));
    }

    @Override
    public void sendRiderRefundProofReceivedEmail(final RiderRefundProofReceivedEmailPayload payload) {
        dispatch(() -> delegate.sendRiderRefundProofReceivedEmail(payload));
    }

    @Override
    public void sendListingPausedDueToMissingCbu(final CarPausedMissingCbuOwnerEmailPayload payload) {
        dispatch(() -> delegate.sendListingPausedDueToMissingCbu(payload));
    }

    @Override
    public void sendListingPausedDueToMissingIdentity(final CarPausedMissingIdentityOwnerEmailPayload payload) {
        dispatch(() -> delegate.sendListingPausedDueToMissingIdentity(payload));
    }

    @Override
    public void sendCarPausedByAdmin(final CarPausedByAdminOwnerEmailPayload payload) {
        dispatch(() -> delegate.sendCarPausedByAdmin(payload));
    }

    @Override
    public void sendCarRejectedByAdmin(final CarRejectedByAdminOwnerEmailPayload payload) {
        dispatch(() -> delegate.sendCarRejectedByAdmin(payload));
    }

    @Override
    public void sendCarValidatedByAdmin(final CarValidatedByAdminOwnerEmailPayload payload) {
        dispatch(() -> delegate.sendCarValidatedByAdmin(payload));
    }

    @Override
    public void sendReservationChatDigestEmail(final ReservationChatDigestEmailPayload payload) {
        dispatch(() -> delegate.sendReservationChatDigestEmail(payload));
    }
}
