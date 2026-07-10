package ar.edu.itba.paw.webapp.controller.review;

import java.io.IOException;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ar.edu.itba.paw.models.domain.review.Review;
import ar.edu.itba.paw.services.review.ReviewService;
import ar.edu.itba.paw.webapp.api.common.VndMediaType;
import ar.edu.itba.paw.webapp.dto.rest.ReviewDto;
import ar.edu.itba.paw.webapp.form.review.ReviewListQueryForm;
import ar.edu.itba.paw.webapp.form.review.ReviewSubmitQueryForm;
import ar.edu.itba.paw.webapp.support.CurrentUserResolver;
import ar.edu.itba.paw.webapp.support.FormValidationSupport;
import ar.edu.itba.paw.webapp.support.ReviewRepresentationSupport;
import ar.edu.itba.paw.webapp.support.ReviewResourceAccess;
import ar.edu.itba.paw.webapp.support.ReviewSubmitSupport;
import ar.edu.itba.paw.webapp.util.RestUriUtils;

/**
 * Canonical reviews resource ({@code /reviews}, {@code /reviews/{id}}).
 * Create and list share the same collection URI; creation requires {@code reservationId}.
 */
@Path("/reviews")
@Component
public final class ReviewsController {

    private final ReviewService reviewService;
    private final CurrentUserResolver currentUserResolver;
    private final ReviewResourceAccess reviewResourceAccess;
    private final FormValidationSupport formValidationSupport;
    private final ReviewRepresentationSupport reviewRepresentationSupport;
    private final ReviewSubmitSupport reviewSubmitSupport;

    @Context
    private UriInfo uriInfo;

    @Autowired
    public ReviewsController(
            final ReviewService reviewService,
            final CurrentUserResolver currentUserResolver,
            final ReviewResourceAccess reviewResourceAccess,
            final FormValidationSupport formValidationSupport,
            final ReviewRepresentationSupport reviewRepresentationSupport,
            final ReviewSubmitSupport reviewSubmitSupport) {
        this.reviewService = reviewService;
        this.currentUserResolver = currentUserResolver;
        this.reviewResourceAccess = reviewResourceAccess;
        this.formValidationSupport = formValidationSupport;
        this.reviewRepresentationSupport = reviewRepresentationSupport;
        this.reviewSubmitSupport = reviewSubmitSupport;
    }

    @GET
    @Produces(VndMediaType.REVIEW_LINKS_V1_JSON)
    public Response listReviews(
            @QueryParam("carId") final Long carId,
            @QueryParam("recipientUserId") final Long recipientUserId,
            @QueryParam("reservationId") final Long reservationId,
            @QueryParam("page") @DefaultValue("1") final int page,
            @QueryParam("pageSize") final Integer pageSizeParam) {
        final ReviewListQueryForm query = ReviewListQueryForm.of(carId, recipientUserId, reservationId);
        formValidationSupport.validate(query);
        return reviewRepresentationSupport.listReviews(
                query,
                page,
                pageSizeParam,
                uriInfo,
                currentUserResolver.currentPrincipalOrNull());
    }

    /**
     * Creates a review for a finished reservation. Same collection URI as {@link #listReviews};
     * {@code reservationId} is required (query) so the resource identity stays unique.
     */
    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(VndMediaType.REVIEW_V1_JSON)
    public Response submitReview(
            @QueryParam("reservationId") final Long reservationId,
            @FormDataParam("rating") final Integer rating,
            @FormDataParam("comment") final String comment,
            @FormDataParam("image") final FormDataBodyPart imagePart) throws IOException {
        final ReviewSubmitQueryForm query = ReviewSubmitQueryForm.of(reservationId);
        formValidationSupport.validate(query);
        final var form = reviewSubmitSupport.buildValidatedSubmitForm(rating, comment, imagePart);
        final Review created = reviewSubmitSupport.submitForReservation(
                currentUserResolver.requirePrincipal().getUserId(),
                query.getReservationId(),
                form);
        final ReviewDto dto = ReviewDto.from(created, uriInfo);
        return Response.created(RestUriUtils.reviewUri(uriInfo, created.getId()))
                .entity(dto)
                .build();
    }

    @GET
    @Path("/{reviewId}")
    @Produces(VndMediaType.REVIEW_V1_JSON)
    public Response getReview(@PathParam("reviewId") final long reviewId) {
        final var viewer = currentUserResolver.currentPrincipalOrNull();
        final Review review = reviewService.getReviewById(reviewId)
                .orElseThrow(NotFoundException::new);
        reviewResourceAccess.requireCanViewReview(review, viewer);
        return Response.ok(ReviewDto.from(review, uriInfo)).build();
    }
}
