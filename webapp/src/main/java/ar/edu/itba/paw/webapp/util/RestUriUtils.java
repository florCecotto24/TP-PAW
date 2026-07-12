package ar.edu.itba.paw.webapp.util;

import java.net.URI;
import java.util.Optional;

import javax.ws.rs.core.UriInfo;

/**
 * Hypermedia URI builders for JAX-RS resources ({@code openapi.yaml} contract).
 */
public final class RestUriUtils {

    private RestUriUtils() {
    }

    public static URI userUri(final UriInfo uriInfo, final long userId) {
        return uriInfo.getBaseUriBuilder().path("users").path(String.valueOf(userId)).build();
    }

    public static URI userProfilePictureUri(final UriInfo uriInfo, final long userId) {
        return uriInfo.getBaseUriBuilder()
                .path("users").path(String.valueOf(userId))
                .path("profile-picture")
                .build();
    }

    public static URI userCarsUri(final UriInfo uriInfo, final long userId) {
        return uriInfo.getBaseUriBuilder()
                .path("cars")
                .queryParam("ownerId", userId)
                .build();
    }

    /**
     * Reservations made by {@code userId} as a rider — {@code GET /reservations} keys collection
     * filters off {@code riderId}/{@code ownerId} (a plain {@code userId} is not a recognized
     * param and would 403 non-admin viewers, since it falls through to the admin-only branch).
     */
    public static URI userReservationsUri(final UriInfo uriInfo, final long userId) {
        return uriInfo.getBaseUriBuilder()
                .path("reservations")
                .queryParam("riderId", userId)
                .build();
    }

    public static URI userFavoritesUri(final UriInfo uriInfo, final long userId) {
        return uriInfo.getBaseUriBuilder()
                .path("users").path(String.valueOf(userId))
                .path("favorites")
                .build();
    }

    public static URI userDocumentsUri(final UriInfo uriInfo, final long userId) {
        return uriInfo.getBaseUriBuilder()
                .path("users").path(String.valueOf(userId))
                .path("documents")
                .build();
    }

    public static URI userReviewsUri(final UriInfo uriInfo, final long userId) {
        return uriInfo.getBaseUriBuilder()
                .path("reviews")
                .queryParam("recipientUserId", userId)
                .build();
    }

    public static URI carUri(final UriInfo uriInfo, final long carId) {
        return uriInfo.getBaseUriBuilder().path("cars").path(String.valueOf(carId)).build();
    }

    public static URI userDocumentUri(final UriInfo uriInfo, final long userId, final String documentType) {
        return uriInfo.getBaseUriBuilder()
                .path("users").path(String.valueOf(userId))
                .path("documents").path(documentType)
                .build();
    }

    public static URI userFavoriteCarUri(final UriInfo uriInfo, final long userId, final long carId) {
        return uriInfo.getBaseUriBuilder()
                .path("users").path(String.valueOf(userId))
                .path("favorites").path(String.valueOf(carId))
                .build();
    }

    public static URI brandUri(final UriInfo uriInfo, final long brandId) {
        return uriInfo.getBaseUriBuilder().path("brands").path(String.valueOf(brandId)).build();
    }

    public static URI brandModelsUri(final UriInfo uriInfo, final long brandId) {
        return uriInfo.getBaseUriBuilder()
                .path("brands").path(String.valueOf(brandId))
                .path("models")
                .build();
    }

    public static URI modelUri(final UriInfo uriInfo, final long brandId, final long modelId) {
        return uriInfo.getBaseUriBuilder()
                .path("brands").path(String.valueOf(brandId))
                .path("models").path(String.valueOf(modelId))
                .build();
    }

    public static URI modelPriceInsightUri(final UriInfo uriInfo, final long brandId, final long modelId) {
        return uriInfo.getBaseUriBuilder()
                .path("brands").path(String.valueOf(brandId))
                .path("models").path(String.valueOf(modelId))
                .path("price-insight")
                .build();
    }

    public static URI neighborhoodUri(final UriInfo uriInfo, final long neighborhoodId) {
        return uriInfo.getBaseUriBuilder()
                .path("neighborhoods")
                .path(String.valueOf(neighborhoodId))
                .build();
    }

    /**
     * Extracts the neighborhood primary key from a canonical {@code /neighborhoods/{id}} link
     * (absolute or relative). Returns empty for null/blank input or when the path suffix is not
     * a numeric id.
     */
    public static Optional<Long> parseNeighborhoodId(final String neighborhoodUri) {
        if (neighborhoodUri == null || neighborhoodUri.isBlank()) {
            return Optional.empty();
        }
        final String path = neighborhoodUri.split("\\?")[0].replaceAll("/+$", "");
        final int lastSlash = path.lastIndexOf('/');
        if (lastSlash < 0 || lastSlash == path.length() - 1) {
            return Optional.empty();
        }
        try {
            return Optional.of(Long.parseLong(path.substring(lastSlash + 1)));
        } catch (final NumberFormatException e) {
            return Optional.empty();
        }
    }

    public static URI carPicturesUri(final UriInfo uriInfo, final long carId) {
        return uriInfo.getBaseUriBuilder()
                .path("cars").path(String.valueOf(carId))
                .path("pictures")
                .build();
    }

    public static URI carPrimaryPictureUri(final UriInfo uriInfo, final long carId) {
        return uriInfo.getBaseUriBuilder()
                .path("cars").path(String.valueOf(carId))
                .path("pictures").path("primary")
                .build();
    }

    public static URI carPictureUri(final UriInfo uriInfo, final long carId, final long pictureId) {
        return uriInfo.getBaseUriBuilder()
                .path("cars").path(String.valueOf(carId))
                .path("pictures").path(String.valueOf(pictureId))
                .build();
    }

    public static URI carAvailabilitiesUri(final UriInfo uriInfo, final long carId) {
        return uriInfo.getBaseUriBuilder()
                .path("cars").path(String.valueOf(carId))
                .path("availabilities")
                .build();
    }

    public static URI carBookableSegmentsUri(final UriInfo uriInfo, final long carId) {
        return uriInfo.getBaseUriBuilder()
                .path("cars").path(String.valueOf(carId))
                .path("bookable-segments")
                .build();
    }

    public static URI carAvailabilityUri(final UriInfo uriInfo, final long carId, final long availabilityId) {
        return uriInfo.getBaseUriBuilder()
                .path("cars").path(String.valueOf(carId))
                .path("availabilities").path(String.valueOf(availabilityId))
                .build();
    }

    public static URI carAvailabilityRangeUri(
            final UriInfo uriInfo,
            final long carId,
            final String startDate,
            final String endDate) {
        return uriInfo.getBaseUriBuilder()
                .path("cars").path(String.valueOf(carId))
                .path("availabilities").path("range")
                .queryParam("from", startDate)
                .queryParam("until", endDate)
                .build();
    }

    public static URI carSimilarUri(final UriInfo uriInfo, final long carId) {
        return uriInfo.getBaseUriBuilder()
                .path("cars").path(String.valueOf(carId))
                .path("similar")
                .build();
    }

    public static URI carInsuranceUri(final UriInfo uriInfo, final long carId) {
        return uriInfo.getBaseUriBuilder()
                .path("cars").path(String.valueOf(carId))
                .path("insurance")
                .build();
    }

    public static URI carReviewsUri(final UriInfo uriInfo, final long carId) {
        return uriInfo.getBaseUriBuilder()
                .path("reviews")
                .queryParam("carId", carId)
                .build();
    }

    /**
     * Canonical collection URI for reviews of a reservation ({@code /reviews?reservationId=…}).
     * Same URI for {@code GET} (list) and {@code POST} (create).
     */
    public static URI reservationReviewsUri(final UriInfo uriInfo, final long reservationId) {
        return uriInfo.getBaseUriBuilder()
                .path("reviews")
                .queryParam("reservationId", reservationId)
                .build();
    }

    /** Canonical URN for a single review ({@code GET /reviews/{id}}). */
    public static URI reviewUri(final UriInfo uriInfo, final long reviewId) {
        return uriInfo.getBaseUriBuilder().path("reviews").path(String.valueOf(reviewId)).build();
    }

    public static URI reservationUri(final UriInfo uriInfo, final long reservationId) {
        return uriInfo.getBaseUriBuilder()
                .path("reservations")
                .path(String.valueOf(reservationId))
                .build();
    }

    public static URI reservationMessagesUri(final UriInfo uriInfo, final long reservationId) {
        return uriInfo.getBaseUriBuilder()
                .path("reservations").path(String.valueOf(reservationId))
                .path("messages")
                .build();
    }

    public static URI reservationMessageUri(
            final UriInfo uriInfo, final long reservationId, final long messageId) {
        return uriInfo.getBaseUriBuilder()
                .path("reservations").path(String.valueOf(reservationId))
                .path("messages").path(String.valueOf(messageId))
                .build();
    }

    public static URI reservationMessageAttachmentUri(
            final UriInfo uriInfo, final long reservationId, final long messageId) {
        return uriInfo.getBaseUriBuilder()
                .path("reservations").path(String.valueOf(reservationId))
                .path("messages").path(String.valueOf(messageId))
                .path("attachment")
                .build();
    }

    public static URI reservationPaymentReceiptUri(final UriInfo uriInfo, final long reservationId) {
        return uriInfo.getBaseUriBuilder()
                .path("reservations").path(String.valueOf(reservationId))
                .path("payment-receipt")
                .build();
    }

    public static URI reservationRefundReceiptUri(final UriInfo uriInfo, final long reservationId) {
        return uriInfo.getBaseUriBuilder()
                .path("reservations").path(String.valueOf(reservationId))
                .path("refund-receipt")
                .build();
    }

    public static URI imageUri(final UriInfo uriInfo, final long imageId) {
        return uriInfo.getBaseUriBuilder()
                .path("image").path(String.valueOf(imageId))
                .build();
    }
}
