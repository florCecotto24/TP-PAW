package ar.edu.itba.paw.webapp.dto.rest;

import javax.ws.rs.core.UriInfo;

import ar.edu.itba.paw.models.domain.reservation.Reservation;
import ar.edu.itba.paw.models.dto.reservation.ReservationCard;
import ar.edu.itba.paw.webapp.util.RestUriUtils;

/** Hypermedia links for reservation resources. */
public final class ReservationLinks {

    private ReservationLinks() {
    }

    public static LinksDto forReservation(
            final Reservation reservation,
            final long ownerId,
            final UriInfo uriInfo) {
        final long reservationId = reservation.getId();
        return LinksDto.ofSelf(RestUriUtils.reservationUri(uriInfo, reservationId).toString())
                .withRelated("car", RestUriUtils.carUri(uriInfo, reservation.getCarId()).toString())
                .withRelated("rider", RestUriUtils.userUri(uriInfo, reservation.getRiderId()).toString())
                .withRelated("owner", RestUriUtils.userUri(uriInfo, ownerId).toString())
                .withRelated("messages", RestUriUtils.reservationMessagesUri(uriInfo, reservationId).toString())
                .withRelated("reviews", RestUriUtils.reservationReviewsUri(uriInfo, reservationId).toString())
                .withRelated("payment-receipt",
                        RestUriUtils.reservationPaymentReceiptUri(uriInfo, reservationId).toString())
                .withRelated("refund-receipt",
                        RestUriUtils.reservationRefundReceiptUri(uriInfo, reservationId).toString());
    }

    public static LinksDto forCard(final ReservationCard card, final UriInfo uriInfo) {
        final long reservationId = card.getReservationId();
        return LinksDto.ofSelf(RestUriUtils.reservationUri(uriInfo, reservationId).toString())
                .withRelated("car", RestUriUtils.carUri(uriInfo, card.getCarId()).toString())
                .withRelated("messages", RestUriUtils.reservationMessagesUri(uriInfo, reservationId).toString())
                .withRelated("reviews", RestUriUtils.reservationReviewsUri(uriInfo, reservationId).toString());
    }
}
