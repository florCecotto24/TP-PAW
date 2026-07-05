package ar.edu.itba.paw.webapp.dto.rest;

import javax.ws.rs.core.UriInfo;

import ar.edu.itba.paw.models.domain.reservation.Reservation;
import ar.edu.itba.paw.models.dto.reservation.ReservationCard;
import ar.edu.itba.paw.webapp.util.RestUriUtils;

/** Hypermedia links for reservation resources. */
public final class ReservationLinks {

    private ReservationLinks() {
    }

    public static ReservationBuilder reservation(
            final Reservation reservation,
            final long ownerId,
            final UriInfo uriInfo) {
        return new ReservationBuilder(reservation, ownerId, uriInfo);
    }

    public static final class ReservationBuilder {

        private final Reservation reservation;
        private final long ownerId;
        private final UriInfo uriInfo;
        private boolean hasPaymentReceipt;
        private boolean hasRefundReceipt;

        private ReservationBuilder(
                final Reservation reservation,
                final long ownerId,
                final UriInfo uriInfo) {
            this.reservation = reservation;
            this.ownerId = ownerId;
            this.uriInfo = uriInfo;
        }

        public ReservationBuilder hasPaymentReceipt(final boolean hasPaymentReceipt) {
            this.hasPaymentReceipt = hasPaymentReceipt;
            return this;
        }

        public ReservationBuilder hasRefundReceipt(final boolean hasRefundReceipt) {
            this.hasRefundReceipt = hasRefundReceipt;
            return this;
        }

        public LinksDto build() {
            final long reservationId = reservation.getId();
            LinksDto links = LinksDto.ofSelf(RestUriUtils.reservationUri(uriInfo, reservationId).toString())
                    .withRelated("car", RestUriUtils.carUri(uriInfo, reservation.getCarId()).toString())
                    .withRelated("rider", RestUriUtils.userUri(uriInfo, reservation.getRiderId()).toString())
                    .withRelated("owner", RestUriUtils.userUri(uriInfo, ownerId).toString())
                    .withRelated("messages", RestUriUtils.reservationMessagesUri(uriInfo, reservationId).toString())
                    .withRelated("reviews", RestUriUtils.reservationReviewsUri(uriInfo, reservationId).toString())
                    .withRelated("counterparty",
                            RestUriUtils.reservationUri(uriInfo, reservationId).toString() + "/counterparty");
            if (hasPaymentReceipt) {
                links = links.withRelated(
                        "payment-receipt",
                        RestUriUtils.reservationPaymentReceiptUri(uriInfo, reservationId).toString());
            }
            if (hasRefundReceipt) {
                links = links.withRelated(
                        "refund-receipt",
                        RestUriUtils.reservationRefundReceiptUri(uriInfo, reservationId).toString());
            }
            return links;
        }
    }

    public static LinksDto forCard(final ReservationCard card, final UriInfo uriInfo) {
        final long reservationId = card.getReservationId();
        LinksDto links = LinksDto.ofSelf(RestUriUtils.reservationUri(uriInfo, reservationId).toString())
                .withRelated("car", RestUriUtils.carUri(uriInfo, card.getCarId()).toString())
                .withRelated("messages", RestUriUtils.reservationMessagesUri(uriInfo, reservationId).toString())
                .withRelated("reviews", RestUriUtils.reservationReviewsUri(uriInfo, reservationId).toString());
        if (card.getImageId() > 0) {
            links = links.withRelated(
                    "cover",
                    RestUriUtils.imageUri(uriInfo, card.getImageId()).toString());
        }
        return links;
    }
}
