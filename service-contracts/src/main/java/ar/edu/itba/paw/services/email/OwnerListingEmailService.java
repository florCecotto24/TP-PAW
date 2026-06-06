package ar.edu.itba.paw.services.email;


import java.util.List;

import ar.edu.itba.paw.models.email.CarPausedByAdminOwnerEmailPayload;
import ar.edu.itba.paw.models.email.CarPausedMissingCbuOwnerEmailPayload;
import ar.edu.itba.paw.models.email.CarRejectedByAdminOwnerEmailPayload;
import ar.edu.itba.paw.models.email.CarValidatedByAdminOwnerEmailPayload;
import ar.edu.itba.paw.models.email.OwnerBlockedEmailPayload;
import ar.edu.itba.paw.models.email.OwnerPaymentProofReceivedEmailPayload;
import ar.edu.itba.paw.models.email.OwnerRefundProofObligationEmailPayload;
import ar.edu.itba.paw.models.email.ReservationMailPayload;

/**
 * Owner-facing transactional emails (listing lifecycle, payment-proof receipts, refund-proof
 * obligations, blocked notices, and bulk listing deletion). Extracted from {@link EmailService}.
 *
 * <p>{@link EmailService} keeps these methods declared (back-compat) and delegates here.</p>
 */
public interface OwnerListingEmailService {

    /** See {@link EmailService#sendListingDeletionEmail(List)}. */
    void sendListingDeletionEmail(List<ReservationMailPayload> reservationsToCancel);

    /** See {@link EmailService#sendOwnerPaymentProofReceivedEmail(OwnerPaymentProofReceivedEmailPayload)}. */
    void sendOwnerPaymentProofReceivedEmail(OwnerPaymentProofReceivedEmailPayload payload);

    /** See {@link EmailService#sendOwnerRefundProofObligationEmail(OwnerRefundProofObligationEmailPayload)}. */
    void sendOwnerRefundProofObligationEmail(OwnerRefundProofObligationEmailPayload payload);

    /** See {@link EmailService#sendOwnerBlockedEmail(OwnerBlockedEmailPayload)}. */
    void sendOwnerBlockedEmail(OwnerBlockedEmailPayload payload);

    /** See {@link EmailService#sendListingPausedDueToMissingCbu(CarPausedMissingCbuOwnerEmailPayload)}. */
    void sendListingPausedDueToMissingCbu(CarPausedMissingCbuOwnerEmailPayload payload);

    /** See {@link EmailService#sendCarPausedByAdmin(CarPausedByAdminOwnerEmailPayload)}. */
    void sendCarPausedByAdmin(CarPausedByAdminOwnerEmailPayload payload);

    /** See {@link EmailService#sendCarRejectedByAdmin(CarRejectedByAdminOwnerEmailPayload)}. */
    void sendCarRejectedByAdmin(CarRejectedByAdminOwnerEmailPayload payload);

    /** See {@link EmailService#sendCarValidatedByAdmin(CarValidatedByAdminOwnerEmailPayload)}. */
    void sendCarValidatedByAdmin(CarValidatedByAdminOwnerEmailPayload payload);
}
