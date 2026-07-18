package ar.edu.itba.paw.webapp.support;

import java.io.IOException;
import java.io.InputStream;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.springframework.stereotype.Component;

import ar.edu.itba.paw.models.domain.review.Review;
import ar.edu.itba.paw.services.review.ReviewService;
import ar.edu.itba.paw.webapp.dto.rest.ReviewDto;
import ar.edu.itba.paw.webapp.form.reservation.ReservationReviewSubmitForm;
import ar.edu.itba.paw.webapp.form.review.ReviewSubmitQueryForm;
import ar.edu.itba.paw.webapp.util.RestUriUtils;

/** Multipart binding and persistence for {@code POST /reviews}. */
@Component
public final class ReviewSubmitSupport {

    private final FormValidationSupport formValidationSupport;
    private final ReviewService reviewService;
    private final BinaryPayloadSupport binaryPayloadSupport;

    public ReviewSubmitSupport(
            final FormValidationSupport formValidationSupport,
            final ReviewService reviewService,
            final BinaryPayloadSupport binaryPayloadSupport) {
        this.formValidationSupport = formValidationSupport;
        this.reviewService = reviewService;
        this.binaryPayloadSupport = binaryPayloadSupport;
    }

    public Response submit(
            final long actorUserId,
            final String reservationUri,
            final Integer rating,
            final String comment,
            final FormDataBodyPart imagePart,
            final UriInfo uriInfo) throws IOException {
        final long reservationId = RestUriUtils.parseReservationId(reservationUri)
                .orElseThrow(() -> new BadRequestException("reservationUri required"));
        formValidationSupport.validate(ReviewSubmitQueryForm.of(reservationId));
        final ReservationReviewSubmitForm form = buildValidatedSubmitForm(rating, comment, imagePart);
        final Review created = submitForReservation(actorUserId, reservationId, form);
        return Response.created(RestUriUtils.reviewUri(uriInfo, created.getId()))
                .entity(ReviewDto.from(created, uriInfo))
                .build();
    }

    public ReservationReviewSubmitForm buildValidatedSubmitForm(
            final Integer rating,
            final String comment,
            final FormDataBodyPart imagePart) throws IOException {
        final OptionalReviewImage image = readOptionalImage(imagePart);
        final ReservationReviewSubmitForm form = new ReservationReviewSubmitForm();
        form.setRating(rating);
        form.setComment(comment);
        form.setImageBytes(image.bytes);
        form.setImageContentType(image.contentType);
        form.setImageFileName(image.fileName);
        formValidationSupport.validate(form);
        return form;
    }

    public Review submitForReservation(
            final long actorUserId,
            final long reservationId,
            final ReservationReviewSubmitForm form) {
        return reviewService.submitParticipantReview(
                actorUserId,
                reservationId,
                form.getRating(),
                form.getComment(),
                form.getImageFileName(),
                form.getImageContentType(),
                form.getImageBytes());
    }

    private OptionalReviewImage readOptionalImage(final FormDataBodyPart imagePart) throws IOException {
        if (imagePart == null) {
            return OptionalReviewImage.empty();
        }
        final InputStream stream = imagePart.getValueAs(InputStream.class);
        if (stream == null) {
            return OptionalReviewImage.empty();
        }
        final byte[] bytes = binaryPayloadSupport.readBounded(stream);
        if (bytes.length == 0) {
            return OptionalReviewImage.empty();
        }
        final String contentType = imagePart.getMediaType() != null
                ? imagePart.getMediaType().toString()
                : null;
        final String fileName = imagePart.getContentDisposition() != null
                ? imagePart.getContentDisposition().getFileName()
                : "review-image";
        return new OptionalReviewImage(fileName, contentType, bytes);
    }

    private record OptionalReviewImage(String fileName, String contentType, byte[] bytes) {
        private static OptionalReviewImage empty() {
            return new OptionalReviewImage(null, null, null);
        }
    }
}
