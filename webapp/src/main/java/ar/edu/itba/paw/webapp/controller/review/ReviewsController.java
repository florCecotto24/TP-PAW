package ar.edu.itba.paw.webapp.controller.review;

import java.io.IOException;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
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

import ar.edu.itba.paw.webapp.api.common.VndMediaType;
import ar.edu.itba.paw.webapp.support.CurrentUserResolver;
import ar.edu.itba.paw.webapp.support.ReviewRepresentationSupport;
import ar.edu.itba.paw.webapp.support.ReviewSubmitSupport;

/**
 * Canonical reviews resource ({@code /reviews}, {@code /reviews/{id}}). HTTP routing only.
 * Create and list share the same collection URI; creation receives {@code reservationUri} in multipart.
 */
@Path("/reviews")
@Component
public final class ReviewsController {

    private final CurrentUserResolver currentUserResolver;
    private final ReviewRepresentationSupport reviewRepresentationSupport;
    private final ReviewSubmitSupport reviewSubmitSupport;

    @Context
    private UriInfo uriInfo;

    @Autowired
    public ReviewsController(
            final CurrentUserResolver currentUserResolver,
            final ReviewRepresentationSupport reviewRepresentationSupport,
            final ReviewSubmitSupport reviewSubmitSupport) {
        this.currentUserResolver = currentUserResolver;
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
        return reviewRepresentationSupport.listReviews(
                carId,
                recipientUserId,
                reservationId,
                page,
                pageSizeParam,
                uriInfo,
                currentUserResolver.currentPrincipalOrNull());
    }

    /**
     * Creates a review for a finished reservation. The reservation is identified by URN in the
     * multipart body ({@code reservationUri}), not by query string.
     */
    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(VndMediaType.REVIEW_V1_JSON)
    public Response submitReview(
            @FormDataParam("reservationUri") final String reservationUri,
            @FormDataParam("rating") final Integer rating,
            @FormDataParam("comment") final String comment,
            @FormDataParam("image") final FormDataBodyPart imagePart) throws IOException {
        return reviewSubmitSupport.submit(
                currentUserResolver.requirePrincipal().getUserId(),
                reservationUri,
                rating,
                comment,
                imagePart,
                uriInfo);
    }

    @GET
    @Path("/{reviewId}")
    @Produces(VndMediaType.REVIEW_V1_JSON)
    public Response getReview(@PathParam("reviewId") final long reviewId) {
        return reviewRepresentationSupport.getReview(
                reviewId, currentUserResolver.currentPrincipalOrNull(), uriInfo);
    }
}
