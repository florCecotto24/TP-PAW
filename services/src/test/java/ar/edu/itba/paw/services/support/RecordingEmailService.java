package ar.edu.itba.paw.services.support;

import java.util.ArrayList;
import java.util.List;

import ar.edu.itba.paw.models.email.admin.AdminInvitationEmailPayload;
import ar.edu.itba.paw.models.email.admin.AdminPromotedEmailPayload;
import ar.edu.itba.paw.models.email.listing.CarPausedByAdminOwnerEmailPayload;
import ar.edu.itba.paw.models.email.listing.CarPausedMissingCbuOwnerEmailPayload;
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
import ar.edu.itba.paw.services.email.EmailService;

/**
 * State-based test double for {@link EmailService}. Each {@code send*} method appends the payload
 * to a per-kind list that tests inspect directly, instead of stubbing the corresponding mock
 * method with {@code doAnswer} + a captor variable (AGENTS.md rule TEST-8).
 *
 * Replace per-call mocking with a single shared collaborator: the service under test calls the
 * normal {@code send...} entry point, this class records the payload, and the test asserts on the
 * list. Returning state-based assertions also avoids the {@code Mockito.verify} family that the
 * same rule forbids.
 */
public class RecordingEmailService implements EmailService {

    private final List<ReservationMailPayload> reservationConfirmations = new ArrayList<>();
    private final List<ReservationMailPayload> riderReservationConfirmedAfterPaymentProof = new ArrayList<>();
    private final List<ReservationCancellationEmailPayload> reservationCancellations = new ArrayList<>();
    private final List<EmailVerificationCodeEmailPayload> emailVerificationCodes = new ArrayList<>();
    private final List<MigratedUserPasswordEmailPayload> migratedUserPasswords = new ArrayList<>();
    private final List<AdminInvitationEmailPayload> adminInvitations = new ArrayList<>();
    private final List<AdminPromotedEmailPayload> adminPromotions = new ArrayList<>();
    private final List<PasswordResetCodeEmailPayload> passwordResetCodes = new ArrayList<>();
    private final List<ReservationMailPayload> reservationReminders = new ArrayList<>();
    private final List<List<ReservationMailPayload>> listingDeletions = new ArrayList<>();
    private final List<RiderCarReturnEmailPayload> riderReturnReminders = new ArrayList<>();
    private final List<RiderCarReturnEmailPayload> riderReturnCheckouts = new ArrayList<>();
    private final List<RiderReviewInviteEmailPayload> riderReviewInvites = new ArrayList<>();
    private final List<OwnerPaymentProofReceivedEmailPayload> ownerPaymentProofReceived = new ArrayList<>();
    private final List<ReservationMailPayload> riderDuePaymentProofs = new ArrayList<>();
    private final List<OwnerRefundProofObligationEmailPayload> ownerRefundProofObligations = new ArrayList<>();
    private final List<OwnerBlockedEmailPayload> ownerBlocked = new ArrayList<>();
    private final List<RiderRefundProofReceivedEmailPayload> riderRefundProofReceived = new ArrayList<>();
    private final List<CarPausedMissingCbuOwnerEmailPayload> listingPausedMissingCbu = new ArrayList<>();
    private final List<CarPausedByAdminOwnerEmailPayload> carPausedByAdmin = new ArrayList<>();
    private final List<CarRejectedByAdminOwnerEmailPayload> carRejectedByAdmin = new ArrayList<>();
    private final List<CarValidatedByAdminOwnerEmailPayload> carValidatedByAdmin = new ArrayList<>();
    private final List<ReservationChatDigestEmailPayload> reservationChatDigests = new ArrayList<>();

    public List<ReservationMailPayload> reservationConfirmations() { return reservationConfirmations; }
    public List<ReservationMailPayload> riderReservationConfirmedAfterPaymentProof() {
        return riderReservationConfirmedAfterPaymentProof;
    }
    public List<ReservationCancellationEmailPayload> reservationCancellations() { return reservationCancellations; }
    public List<EmailVerificationCodeEmailPayload> emailVerificationCodes() { return emailVerificationCodes; }
    public List<MigratedUserPasswordEmailPayload> migratedUserPasswords() { return migratedUserPasswords; }
    public List<AdminInvitationEmailPayload> adminInvitations() { return adminInvitations; }
    public List<AdminPromotedEmailPayload> adminPromotions() { return adminPromotions; }
    public List<PasswordResetCodeEmailPayload> passwordResetCodes() { return passwordResetCodes; }
    public List<ReservationMailPayload> reservationReminders() { return reservationReminders; }
    public List<List<ReservationMailPayload>> listingDeletions() { return listingDeletions; }
    public List<RiderCarReturnEmailPayload> riderReturnReminders() { return riderReturnReminders; }
    public List<RiderCarReturnEmailPayload> riderReturnCheckouts() { return riderReturnCheckouts; }
    public List<RiderReviewInviteEmailPayload> riderReviewInvites() { return riderReviewInvites; }
    public List<OwnerPaymentProofReceivedEmailPayload> ownerPaymentProofReceived() { return ownerPaymentProofReceived; }
    public List<ReservationMailPayload> riderDuePaymentProofs() { return riderDuePaymentProofs; }
    public List<OwnerRefundProofObligationEmailPayload> ownerRefundProofObligations() { return ownerRefundProofObligations; }
    public List<OwnerBlockedEmailPayload> ownerBlocked() { return ownerBlocked; }
    public List<RiderRefundProofReceivedEmailPayload> riderRefundProofReceived() { return riderRefundProofReceived; }
    public List<CarPausedMissingCbuOwnerEmailPayload> listingPausedMissingCbu() { return listingPausedMissingCbu; }
    public List<CarPausedByAdminOwnerEmailPayload> carPausedByAdmin() { return carPausedByAdmin; }
    public List<CarRejectedByAdminOwnerEmailPayload> carRejectedByAdmin() { return carRejectedByAdmin; }
    public List<CarValidatedByAdminOwnerEmailPayload> carValidatedByAdmin() { return carValidatedByAdmin; }
    public List<ReservationChatDigestEmailPayload> reservationChatDigests() { return reservationChatDigests; }

    @Override
    public void sendReservationConfirmationEmail(final ReservationMailPayload p) { reservationConfirmations.add(p); }

    @Override
    public void sendRiderReservationConfirmedAfterPaymentProof(final ReservationMailPayload p) {
        riderReservationConfirmedAfterPaymentProof.add(p);
    }

    @Override
    public void sendReservationCancellationEmail(final ReservationCancellationEmailPayload p) {
        reservationCancellations.add(p);
    }

    @Override
    public void sendEmailVerificationCode(final EmailVerificationCodeEmailPayload p) {
        emailVerificationCodes.add(p);
    }

    @Override
    public void sendMigratedUserPassword(final MigratedUserPasswordEmailPayload p) { migratedUserPasswords.add(p); }

    @Override
    public void sendAdminInvitation(final AdminInvitationEmailPayload p) { adminInvitations.add(p); }

    @Override
    public void sendAdminPromoted(final AdminPromotedEmailPayload p) { adminPromotions.add(p); }

    @Override
    public void sendPasswordResetCode(final PasswordResetCodeEmailPayload p) { passwordResetCodes.add(p); }

    @Override
    public void sendReservationReminderEmail(final ReservationMailPayload p) { reservationReminders.add(p); }

    @Override
    public void sendListingDeletionEmail(final List<ReservationMailPayload> reservationsToCancel) {
        listingDeletions.add(reservationsToCancel);
    }

    @Override
    public void sendRiderReturnReminderEmail(final RiderCarReturnEmailPayload p) { riderReturnReminders.add(p); }

    @Override
    public void sendRiderReturnCheckoutEmail(final RiderCarReturnEmailPayload p) { riderReturnCheckouts.add(p); }

    @Override
    public void sendRiderReviewInviteEmail(final RiderReviewInviteEmailPayload p) { riderReviewInvites.add(p); }

    @Override
    public void sendOwnerPaymentProofReceivedEmail(final OwnerPaymentProofReceivedEmailPayload p) {
        ownerPaymentProofReceived.add(p);
    }

    @Override
    public void sendRiderDuePaymentProofEmail(final ReservationMailPayload p) { riderDuePaymentProofs.add(p); }

    @Override
    public void sendOwnerRefundProofObligationEmail(final OwnerRefundProofObligationEmailPayload p) {
        ownerRefundProofObligations.add(p);
    }

    @Override
    public void sendOwnerBlockedEmail(final OwnerBlockedEmailPayload p) { ownerBlocked.add(p); }

    @Override
    public void sendRiderRefundProofReceivedEmail(final RiderRefundProofReceivedEmailPayload p) {
        riderRefundProofReceived.add(p);
    }

    @Override
    public void sendListingPausedDueToMissingCbu(final CarPausedMissingCbuOwnerEmailPayload p) {
        listingPausedMissingCbu.add(p);
    }

    @Override
    public void sendCarPausedByAdmin(final CarPausedByAdminOwnerEmailPayload p) { carPausedByAdmin.add(p); }

    @Override
    public void sendCarRejectedByAdmin(final CarRejectedByAdminOwnerEmailPayload p) { carRejectedByAdmin.add(p); }

    @Override
    public void sendCarValidatedByAdmin(final CarValidatedByAdminOwnerEmailPayload p) { carValidatedByAdmin.add(p); }

    @Override
    public void sendReservationChatDigestEmail(final ReservationChatDigestEmailPayload p) {
        reservationChatDigests.add(p);
    }
}
