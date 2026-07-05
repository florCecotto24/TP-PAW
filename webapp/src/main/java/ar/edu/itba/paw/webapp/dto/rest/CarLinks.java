package ar.edu.itba.paw.webapp.dto.rest;

import javax.ws.rs.core.UriInfo;

import ar.edu.itba.paw.models.domain.car.Car;
import ar.edu.itba.paw.models.dto.car.CarCard;
import ar.edu.itba.paw.webapp.util.RestUriUtils;

/** Hypermedia links block for {@link CarDto}. */
public final class CarLinks {

    private CarLinks() {
    }

    public static CarBuilder car(final long carId, final long ownerId, final UriInfo uriInfo) {
        return new CarBuilder(carId, ownerId, uriInfo);
    }

    public static LinksDto forCar(final Car car, final UriInfo uriInfo) {
        final Long modelId = car.getCarModel().map(m -> m.getId()).orElse(null);
        final Long brandId = car.getCarModel().map(m -> m.getBrandId()).orElse(null);
        return car(car.getId(), car.getOwnerId(), uriInfo)
                .modelId(modelId)
                .brandId(brandId)
                .build();
    }

    /** Browse/favorites card projection: optional {@code cover} when the listing has a gallery image. */
    public static LinksDto forCarCard(final CarCard card, final UriInfo uriInfo) {
        final CarBuilder builder = car(
                card.getCarId(),
                card.getOwnerId() != null ? card.getOwnerId() : 0L,
                uriInfo);
        if (card.getImageId() > 0) {
            builder.coverImageId(card.getImageId());
        }
        return builder.build();
    }

    public static final class CarBuilder {

        private final long carId;
        private final long ownerId;
        private final UriInfo uriInfo;
        private Long modelId;
        private Long brandId;
        private Long coverImageId;

        private CarBuilder(final long carId, final long ownerId, final UriInfo uriInfo) {
            this.carId = carId;
            this.ownerId = ownerId;
            this.uriInfo = uriInfo;
        }

        public CarBuilder modelId(final Long modelId) {
            this.modelId = modelId;
            return this;
        }

        public CarBuilder brandId(final Long brandId) {
            this.brandId = brandId;
            return this;
        }

        public CarBuilder coverImageId(final long coverImageId) {
            this.coverImageId = coverImageId;
            return this;
        }

        public LinksDto build() {
            LinksDto links = LinksDto.ofSelf(RestUriUtils.carUri(uriInfo, carId).toString())
                    .withRelated("owner", RestUriUtils.userUri(uriInfo, ownerId).toString())
                    .withRelated("model", modelId != null ? RestUriUtils.modelUri(uriInfo, modelId).toString() : null)
                    .withRelated("brand", brandId != null ? RestUriUtils.brandUri(uriInfo, brandId).toString() : null)
                    .withRelated("pictures", RestUriUtils.carPicturesUri(uriInfo, carId).toString())
                    .withRelated("availabilities", RestUriUtils.carAvailabilitiesUri(uriInfo, carId).toString())
                    .withRelated("insurance", RestUriUtils.carInsuranceUri(uriInfo, carId).toString())
                    .withRelated("reviews", RestUriUtils.carReviewsUri(uriInfo, carId).toString());
            if (coverImageId != null && coverImageId > 0) {
                links = links.withRelated(
                        "cover",
                        RestUriUtils.imageUri(uriInfo, coverImageId).toString());
            }
            return links;
        }
    }
}
