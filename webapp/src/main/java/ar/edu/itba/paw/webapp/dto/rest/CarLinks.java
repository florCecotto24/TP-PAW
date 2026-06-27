package ar.edu.itba.paw.webapp.dto.rest;

import javax.ws.rs.core.UriInfo;

import ar.edu.itba.paw.models.domain.car.Car;
import ar.edu.itba.paw.models.dto.car.CarCard;
import ar.edu.itba.paw.webapp.util.RestUriUtils;

/** Hypermedia links block for {@link CarDto}. */
public final class CarLinks {

    private CarLinks() {
    }

    public static LinksDto forCar(final Car car, final UriInfo uriInfo) {
        final long carId = car.getId();
        final Long modelId = car.getCarModel().map(m -> m.getId()).orElse(null);
        final Long brandId = car.getCarModel().map(m -> m.getBrandId()).orElse(null);
        return forCar(carId, car.getOwnerId(), modelId, brandId, uriInfo);
    }

    public static LinksDto forCar(
            final long carId,
            final long ownerId,
            final Long modelId,
            final Long brandId,
            final UriInfo uriInfo) {
        return LinksDto.ofSelf(RestUriUtils.carUri(uriInfo, carId).toString())
                .withRelated("owner", RestUriUtils.userUri(uriInfo, ownerId).toString())
                .withRelated("model", modelId != null ? RestUriUtils.modelUri(uriInfo, modelId).toString() : null)
                .withRelated("brand", brandId != null ? RestUriUtils.brandUri(uriInfo, brandId).toString() : null)
                .withRelated("pictures", RestUriUtils.carPicturesUri(uriInfo, carId).toString())
                .withRelated("availabilities", RestUriUtils.carAvailabilitiesUri(uriInfo, carId).toString())
                .withRelated("insurance", RestUriUtils.carInsuranceUri(uriInfo, carId).toString())
                .withRelated("reviews", RestUriUtils.carReviewsUri(uriInfo, carId).toString());
    }

    /** Browse/favorites card projection: optional {@code cover} when the listing has a gallery image. */
    public static LinksDto forCarCard(final CarCard card, final UriInfo uriInfo) {
        LinksDto links = forCar(
                card.getCarId(),
                card.getOwnerId() != null ? card.getOwnerId() : 0L,
                null,
                null,
                uriInfo);
        if (card.getImageId() > 0) {
            links = links.withRelated(
                    "cover",
                    RestUriUtils.imageUri(uriInfo, card.getImageId()).toString());
        }
        return links;
    }
}
