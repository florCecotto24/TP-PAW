package ar.edu.itba.paw.webapp.dto.rest;

import java.time.format.DateTimeFormatter;

import javax.ws.rs.core.UriInfo;

import ar.edu.itba.paw.models.domain.review.Review;
import ar.edu.itba.paw.models.domain.user.User;
import ar.edu.itba.paw.webapp.util.RestUriUtils;

/** Public review ({@code application/vnd.paw.review.v1+json}). */
public final class ReviewDto {

    private static final DateTimeFormatter ISO_OFFSET = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private Integer rating;
    private String comment;
    private boolean madeByRider;
    private String createdAt;
    private LinksDto links;

    public ReviewDto() {
    }

    public static ReviewDto from(final Review review, final UriInfo uriInfo) {
        final ReviewDto dto = new ReviewDto();
        dto.rating = review.getRating().orElse(null);
        dto.comment = review.getComment().orElse(null);
        dto.madeByRider = review.isMadeByRider();
        dto.createdAt = review.getCreatedAt() == null ? null : ISO_OFFSET.format(review.getCreatedAt());
        final long reservationId = review.getReservationId();
        final long carId = review.getReservation().getCarId();
        final User author = dto.madeByRider
                ? review.getReservation().getRider()
                : review.getReservation().getCar().getOwner();
        dto.links = LinksDto.ofSelf(RestUriUtils.reviewUri(uriInfo, review.getId()).toString())
                .withRelated("reservation", RestUriUtils.reservationUri(uriInfo, reservationId).toString())
                .withRelated("car", RestUriUtils.carUri(uriInfo, carId).toString())
                .withRelated("author", RestUriUtils.userUri(uriInfo, author.getId()).toString());
        review.getImage().map(img -> img.getId()).ifPresent(imageId ->
                dto.links.withRelated("image", RestUriUtils.imageUri(uriInfo, imageId).toString()));
        return dto;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(final Integer rating) {
        this.rating = rating;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(final String comment) {
        this.comment = comment;
    }

    public boolean isMadeByRider() {
        return madeByRider;
    }

    public void setMadeByRider(final boolean madeByRider) {
        this.madeByRider = madeByRider;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(final String createdAt) {
        this.createdAt = createdAt;
    }

    public LinksDto getLinks() {
        return links;
    }

    public void setLinks(final LinksDto links) {
        this.links = links;
    }
}
