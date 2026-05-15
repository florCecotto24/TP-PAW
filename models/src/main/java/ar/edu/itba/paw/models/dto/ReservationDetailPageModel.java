package ar.edu.itba.paw.models.dto;

import java.util.Optional;
import java.util.function.BiConsumer;

import ar.edu.itba.paw.models.domain.Car;
import ar.edu.itba.paw.models.domain.Listing;
import ar.edu.itba.paw.models.domain.Reservation;
import ar.edu.itba.paw.models.domain.User;

/**
 * Model attributes for the {@code myReservationDetail} JSP; built in the service layer and applied via
 * {@link #populateModel(BiConsumer)}.
 */
public final class ReservationDetailPageModel {

    private final Reservation reservation;
    private final Listing listing;
    private final String reservationPickupLocationDisplay;
    private final Car car;
    private final User owner;
    private final User counterparty;
    private final Long counterpartyProfileImageId;
    private final String counterpartyPhoneDisplay;
    private final String cbu;
    private final String pickupDateTime;
    private final String returnDateTime;
    private final String statusKey;
    private final String totalPrice;
    private final long carImageId;
    private final String reservationRole;
    private final long uploadMaxImageBytes;
    private final long uploadMaxImageMegabytes;
    private final int uploadMaxPaymentReceiptBytes;
    private final int uploadMaxPaymentReceiptMegabytes;
    private final int reviewCommentMaxLength;
    private final boolean reservationPeriodEnded;
    private final boolean canOwnerMarkCarReturned;
    private final boolean canOwnerUnmarkCarReturned;
    private final boolean canOwnerReviewRider;
    private final boolean canRiderReviewOwner;
    private final Optional<String> paymentProofDeadlineDisplay;
    private final boolean hasPaymentReceipt;
    private final boolean paymentReceiptApproved;
    private final boolean canCancelReservation;
    private final boolean hasRefundReceipt;
    private final boolean refundReceiptApproved;
    private final Optional<String> refundProofDeadlineDisplay;
    private final boolean chatAvailable;
    private final int chatMessageMaxLength;
    private final long chatViewerUserId;

    public ReservationDetailPageModel(
            final Reservation reservation,
            final Listing listing,
            final String reservationPickupLocationDisplay,
            final Car car,
            final User owner,
            final User counterparty,
            final Long counterpartyProfileImageId,
            final String counterpartyPhoneDisplay,
            final String cbu,
            final String pickupDateTime,
            final String returnDateTime,
            final String statusKey,
            final String totalPrice,
            final long carImageId,
            final String reservationRole,
            final long uploadMaxImageBytes,
            final long uploadMaxImageMegabytes,
            final int uploadMaxPaymentReceiptBytes,
            final int uploadMaxPaymentReceiptMegabytes,
            final int reviewCommentMaxLength,
            final boolean reservationPeriodEnded,
            final boolean canOwnerMarkCarReturned,
            final boolean canOwnerUnmarkCarReturned,
            final boolean canOwnerReviewRider,
            final boolean canRiderReviewOwner,
            final Optional<String> paymentProofDeadlineDisplay,
            final boolean hasPaymentReceipt,
            final boolean paymentReceiptApproved,
            final boolean canCancelReservation,
            final boolean hasRefundReceipt,
            final boolean refundReceiptApproved,
            final Optional<String> refundProofDeadlineDisplay,
            final boolean chatAvailable,
            final int chatMessageMaxLength,
            final long chatViewerUserId) {
        this.reservation = reservation;
        this.listing = listing;
        this.reservationPickupLocationDisplay = reservationPickupLocationDisplay;
        this.car = car;
        this.owner = owner;
        this.counterparty = counterparty;
        this.counterpartyProfileImageId = counterpartyProfileImageId;
        this.counterpartyPhoneDisplay = counterpartyPhoneDisplay;
        this.cbu = cbu;
        this.pickupDateTime = pickupDateTime;
        this.returnDateTime = returnDateTime;
        this.statusKey = statusKey;
        this.totalPrice = totalPrice;
        this.carImageId = carImageId;
        this.reservationRole = reservationRole;
        this.uploadMaxImageBytes = uploadMaxImageBytes;
        this.uploadMaxImageMegabytes = uploadMaxImageMegabytes;
        this.uploadMaxPaymentReceiptBytes = uploadMaxPaymentReceiptBytes;
        this.uploadMaxPaymentReceiptMegabytes = uploadMaxPaymentReceiptMegabytes;
        this.reviewCommentMaxLength = reviewCommentMaxLength;
        this.reservationPeriodEnded = reservationPeriodEnded;
        this.canOwnerMarkCarReturned = canOwnerMarkCarReturned;
        this.canOwnerUnmarkCarReturned = canOwnerUnmarkCarReturned;
        this.canOwnerReviewRider = canOwnerReviewRider;
        this.canRiderReviewOwner = canRiderReviewOwner;
        this.paymentProofDeadlineDisplay = paymentProofDeadlineDisplay;
        this.hasPaymentReceipt = hasPaymentReceipt;
        this.paymentReceiptApproved = paymentReceiptApproved;
        this.canCancelReservation = canCancelReservation;
        this.hasRefundReceipt = hasRefundReceipt;
        this.refundReceiptApproved = refundReceiptApproved;
        this.refundProofDeadlineDisplay = refundProofDeadlineDisplay;
        this.chatAvailable = chatAvailable;
        this.chatMessageMaxLength = chatMessageMaxLength;
        this.chatViewerUserId = chatViewerUserId;
    }

    /** Same as {@link Reservation#getListingId()}; exposed for navigation / breadcrumb context. */
    public long getListingId() {
        return reservation.getListingId();
    }

    public final void populateModel(final BiConsumer<String, Object> putObject) {
        putObject.accept("reservation", reservation);
        putObject.accept("listing", listing);
        putObject.accept("reservationPickupLocationDisplay", reservationPickupLocationDisplay);
        putObject.accept("car", car);
        putObject.accept("owner", owner);
        putObject.accept("counterparty", counterparty);
        putObject.accept("counterpartyProfileImageId", counterpartyProfileImageId);
        putObject.accept("counterpartyPhoneDisplay", counterpartyPhoneDisplay);
        putObject.accept("cbu", cbu);
        putObject.accept("pickupDateTime", pickupDateTime);
        putObject.accept("returnDateTime", returnDateTime);
        putObject.accept("statusKey", statusKey);
        putObject.accept("totalPrice", totalPrice);
        putObject.accept("carImageId", carImageId);
        putObject.accept("reservationRole", reservationRole);
        putObject.accept("uploadMaxImageBytes", uploadMaxImageBytes);
        putObject.accept("uploadMaxImageMegabytes", uploadMaxImageMegabytes);
        putObject.accept("uploadMaxPaymentReceiptBytes", uploadMaxPaymentReceiptBytes);
        putObject.accept("uploadMaxPaymentReceiptMegabytes", uploadMaxPaymentReceiptMegabytes);
        putObject.accept("reviewCommentMaxLength", reviewCommentMaxLength);
        putObject.accept("reservationPeriodEnded", reservationPeriodEnded);
        putObject.accept("canOwnerMarkCarReturned", canOwnerMarkCarReturned);
        putObject.accept("canOwnerUnmarkCarReturned", canOwnerUnmarkCarReturned);
        putObject.accept("canOwnerReviewRider", canOwnerReviewRider);
        putObject.accept("canRiderReviewOwner", canRiderReviewOwner);
        paymentProofDeadlineDisplay.ifPresent(s -> putObject.accept("paymentProofDeadlineDisplay", s));
        putObject.accept("hasPaymentReceipt", hasPaymentReceipt);
        putObject.accept("paymentReceiptApproved", paymentReceiptApproved);
        putObject.accept("canCancelReservation", canCancelReservation);
        putObject.accept("hasRefundReceipt", hasRefundReceipt);
        putObject.accept("refundReceiptApproved", refundReceiptApproved);
        refundProofDeadlineDisplay.ifPresent(s -> putObject.accept("refundProofDeadlineDisplay", s));
        putObject.accept("chatAvailable", chatAvailable);
        putObject.accept("chatMessageMaxLength", chatMessageMaxLength);
        putObject.accept("chatViewerUserId", chatViewerUserId);
    }
}
