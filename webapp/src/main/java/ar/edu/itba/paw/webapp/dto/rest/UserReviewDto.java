package ar.edu.itba.paw.webapp.dto.rest;

import javax.ws.rs.core.UriInfo;

import ar.edu.itba.paw.models.dto.profile.ReviewItemDto;
import ar.edu.itba.paw.webapp.util.RestUriUtils;

/** Review received by a user ({@code application/vnd.paw.review.v1+json}). */
public final class UserReviewDto {

    private int rating;
    private String comment;
    private String authorName;
    private String createdAt;
    private LinksDto links;

    public UserReviewDto() {
    }

    public static UserReviewDto from(
            final ReviewItemDto item,
            final UriInfo uriInfo) {
        final UserReviewDto dto = new UserReviewDto();
        dto.rating = item.getRating();
        dto.comment = item.getComment().orElse(null);
        dto.authorName = item.getReviewerName();
        dto.createdAt = item.getReviewDate() != null ? item.getReviewDate().toString() : null;
        dto.links = LinksDto.ofSelf(
                RestUriUtils.reservationReviewUri(uriInfo, item.getReservationId(), item.getId()).toString());
        return dto;
    }

    public int getRating() {
        return rating;
    }

    public void setRating(final int rating) {
        this.rating = rating;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(final String comment) {
        this.comment = comment;
    }

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(final String authorName) {
        this.authorName = authorName;
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
