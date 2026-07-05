package ar.edu.itba.paw.webapp.controller.reservation;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.method.P;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

import ar.edu.itba.paw.models.domain.reservation.ReservationParticipantRole;
import ar.edu.itba.paw.models.domain.review.Review;
import ar.edu.itba.paw.services.review.ReviewService;
import ar.edu.itba.paw.webapp.api.common.VndMediaType;
import ar.edu.itba.paw.webapp.dto.rest.ReviewDto;
import ar.edu.itba.paw.webapp.form.reservation.ReservationReviewSubmitForm;
import ar.edu.itba.paw.webapp.security.auth.userdetails.RydenUserDetails;
import ar.edu.itba.paw.webapp.support.CurrentUserResolver;
import ar.edu.itba.paw.webapp.support.FormValidationSupport;
import ar.edu.itba.paw.webapp.support.ReservationResourceAccess;
import ar.edu.itba.paw.webapp.util.RestUriUtils;

/** Reservation-scoped reviews ({@code /reservations/{id}/reviews}). */
@Path("/reservations/{id}/reviews")
@Component
public class ReservationReviewController {

    private final ReviewService reviewService;
    private final CurrentUserResolver currentUserResolver;
    private final ReservationResourceAccess reservationResourceAccess;
    private final FormValidationSupport formValidationSupport;

    @Context
    private UriInfo uriInfo;

    @Autowired
    public ReservationReviewController(
            final ReviewService reviewService,
            final CurrentUserResolver currentUserResolver,
            final ReservationResourceAccess reservationResourceAccess,
            final FormValidationSupport formValidationSupport) {
        this.reviewService = reviewService;
        this.currentUserResolver = currentUserResolver;
        this.reservationResourceAccess = reservationResourceAccess;
        this.formValidationSupport = formValidationSupport;
    }

    @GET
    @Produces(VndMediaType.REVIEW_V1_JSON)
    @PreAuthorize("@reservationResourceAccess.canViewReservation(#id, @currentUserResolver.currentPrincipalOrNull())")
    public Response listReviews(@P("id") @PathParam("id") final long reservationId) {
        final List<ReviewDto> dtos = reviewService.getReviewsForReservation(reservationId).stream()
                .map(review -> ReviewDto.from(review, uriInfo))
                .collect(Collectors.toList());
        if (dtos.isEmpty()) {
            return Response.noContent().build();
        }
        return Response.ok(new GenericEntity<List<ReviewDto>>(dtos) {}).build();
    }

    @GET
    @Path("/{reviewId}")
    @Produces(VndMediaType.REVIEW_V1_JSON)
    @PreAuthorize("@reservationResourceAccess.canViewReservation(#id, @currentUserResolver.currentPrincipalOrNull())")
    public Response getReview(
            @P("id") @PathParam("id") final long reservationId,
            @PathParam("reviewId") final long reviewId) {
        final Review review = reviewService.getReviewById(reviewId)
                .filter(r -> r.getReservationId() == reservationId)
                .orElseThrow(NotFoundException::new);
        return Response.ok(ReviewDto.from(review, uriInfo)).build();
    }

    // Not a @PreAuthorize candidate: the participant check also decides *which* role the caller
    // is submitting as (rider-of-owner vs. owner-of-rider), a business-routing value the method
    // body needs, not just a pure gate — see ReservationResourceAccess#requireParticipantRole.
    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(VndMediaType.REVIEW_V1_JSON)
    public Response submitReview(
            @PathParam("id") final long reservationId,
            @FormDataParam("rating") final Integer rating,
            @FormDataParam("comment") final String comment,
            @FormDataParam("image") final FormDataBodyPart imagePart) throws IOException {
        final RydenUserDetails viewer = currentUserResolver.requirePrincipal();
        final ReservationParticipantRole role = reservationResourceAccess.requireParticipantRole(reservationId, viewer);
        final ReservationReviewSubmitForm form = new ReservationReviewSubmitForm();
        form.setRating(rating);
        form.setComment(comment);
        final ReviewImagePayload image = readImage(imagePart);
        form.setImageBytes(image.bytes);
        form.setImageContentType(image.contentType);
        form.setImageFileName(image.fileName);
        formValidationSupport.validate(form);
        if (role == ReservationParticipantRole.OWNER) {
            reviewService.submitOwnerReviewOfRider(
                    viewer.getUserId(), reservationId, form.getRating(), form.getComment(),
                    image.fileName, image.contentType, image.bytes);
        } else {
            reviewService.submitRiderReviewOfOwner(
                    viewer.getUserId(), reservationId, form.getRating(), form.getComment(),
                    image.fileName, image.contentType, image.bytes);
        }
        final List<Review> reviews = reviewService.getReviewsForReservation(reservationId);
        final Review created = reviews.stream()
                .filter(r -> r.isMadeByRider() == (role == ReservationParticipantRole.RIDER))
                .findFirst()
                .orElseThrow(() -> new javax.ws.rs.InternalServerErrorException("Review not found after submit."));
        final ReviewDto dto = ReviewDto.from(created, uriInfo);
        return Response.created(RestUriUtils.reservationReviewUri(uriInfo, reservationId, created.getId()))
                .entity(dto)
                .build();
    }

    private static ReviewImagePayload readImage(final FormDataBodyPart imagePart) throws IOException {
        if (imagePart == null) {
            return ReviewImagePayload.empty();
        }
        final var stream = imagePart.getValueAs(java.io.InputStream.class);
        if (stream == null) {
            return ReviewImagePayload.empty();
        }
        final byte[] bytes = stream.readAllBytes();
        if (bytes.length == 0) {
            return ReviewImagePayload.empty();
        }
        final String contentType = imagePart.getMediaType() != null
                ? imagePart.getMediaType().toString()
                : null;
        final String fileName = imagePart.getContentDisposition() != null
                ? imagePart.getContentDisposition().getFileName()
                : "review-image";
        return new ReviewImagePayload(fileName, contentType, bytes);
    }

    private record ReviewImagePayload(String fileName, String contentType, byte[] bytes) {
        private static ReviewImagePayload empty() {
            return new ReviewImagePayload(null, null, null);
        }
    }
}
