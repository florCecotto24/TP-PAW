package ar.edu.itba.paw.webapp.support;

import java.util.List;
import java.util.stream.Collectors;

import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.springframework.stereotype.Component;

import ar.edu.itba.paw.models.domain.review.Review;
import ar.edu.itba.paw.models.dto.Page;
import ar.edu.itba.paw.services.review.ReviewService;
import ar.edu.itba.paw.webapp.api.common.PaginationLinks;
import ar.edu.itba.paw.webapp.dto.rest.LinksDto;
import ar.edu.itba.paw.webapp.form.review.ReviewListQueryForm;
import ar.edu.itba.paw.webapp.security.auth.userdetails.RydenUserDetails;
import ar.edu.itba.paw.webapp.util.RestUriUtils;

/** REST representations for canonical review collections. */
@Component
public final class ReviewRepresentationSupport {

    private final ReviewService reviewService;
    private final ReviewResourceAccess reviewResourceAccess;
    private final PaginationSupport paginationSupport;

    public ReviewRepresentationSupport(
            final ReviewService reviewService,
            final ReviewResourceAccess reviewResourceAccess,
            final PaginationSupport paginationSupport) {
        this.reviewService = reviewService;
        this.reviewResourceAccess = reviewResourceAccess;
        this.paginationSupport = paginationSupport;
    }

    public Response listReviews(
            final ReviewListQueryForm query,
            final int page,
            final Integer pageSizeParam,
            final UriInfo uriInfo,
            final RydenUserDetails viewer) {
        return switch (query.filter()) {
            case RESERVATION -> listReservationReviews(query.requireReservationId(), uriInfo, viewer);
            case CAR -> listCarReviews(query.requireCarId(), page, pageSizeParam, uriInfo, viewer);
            case RECIPIENT_USER -> listRecipientUserReviews(
                    query.requireRecipientUserId(), page, pageSizeParam, uriInfo, viewer);
        };
    }

    private Response listReservationReviews(
            final long reservationId, final UriInfo uriInfo, final RydenUserDetails viewer) {
        final List<LinksDto> links = reviewService.getReviewsForReservation(reservationId).stream()
                .filter(review -> reviewResourceAccess.canViewReview(review, viewer))
                .map(review -> LinksDto.ofSelf(RestUriUtils.reviewUri(uriInfo, review.getId()).toString()))
                .collect(Collectors.toList());
        if (links.isEmpty()) {
            return Response.noContent().build();
        }
        return Response.ok(new GenericEntity<List<LinksDto>>(links) {})
                .header("X-Total-Count", links.size())
                .build();
    }

    private Response listCarReviews(
            final long carId,
            final int page,
            final Integer pageSizeParam,
            final UriInfo uriInfo,
            final RydenUserDetails viewer) {
        final PaginationParams paging = paginationSupport.forCarReviews(page, pageSizeParam);
        final Page<Review> reviews = reviewService.getCarPublicReviewEntities(
                carId, paging.getZeroBasedPage(), paging.getPageSize());
        return pagedReviewLinks(reviews, paging, uriInfo, viewer);
    }

    private Response listRecipientUserReviews(
            final long recipientUserId,
            final int page,
            final Integer pageSizeParam,
            final UriInfo uriInfo,
            final RydenUserDetails viewer) {
        final PaginationParams paging = paginationSupport.forCarReviews(page, pageSizeParam);
        final Page<Review> reviews = reviewService.getReviewsReceivedByUserEntities(
                recipientUserId, paging.getZeroBasedPage(), paging.getPageSize());
        return pagedReviewLinks(reviews, paging, uriInfo, viewer);
    }

    private Response pagedReviewLinks(
            final Page<Review> reviews,
            final PaginationParams paging,
            final UriInfo uriInfo,
            final RydenUserDetails viewer) {
        final List<LinksDto> links = reviews.getContent().stream()
                .filter(review -> reviewResourceAccess.canViewReview(review, viewer))
                .map(review -> LinksDto.ofSelf(RestUriUtils.reviewUri(uriInfo, review.getId()).toString()))
                .collect(Collectors.toList());
        if (links.isEmpty()) {
            return Response.noContent().build();
        }
        final Response.ResponseBuilder builder =
                Response.ok(new GenericEntity<List<LinksDto>>(links) {})
                        .header("X-Total-Count", reviews.getTotalItems());
        PaginationLinks.add(
                builder, uriInfo, paging.getPage(), paging.getPageSize(), (int) reviews.getTotalItems());
        return builder.build();
    }
}
