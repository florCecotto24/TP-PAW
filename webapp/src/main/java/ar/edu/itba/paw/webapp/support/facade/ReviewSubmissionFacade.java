package ar.edu.itba.paw.webapp.support.facade;

import ar.edu.itba.paw.webapp.support.RiderReservationReviewExceptionMapper;

import java.io.IOException;

import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;
import org.springframework.web.multipart.MultipartFile;

import ar.edu.itba.paw.exception.RydenException;
import ar.edu.itba.paw.exception.reservation.RiderReservationException;
import ar.edu.itba.paw.services.ReviewService;
import ar.edu.itba.paw.webapp.form.ReservationReviewAction;
import ar.edu.itba.paw.webapp.form.ReservationReviewForm;
import ar.edu.itba.paw.webapp.util.LocaleMessages;
import ar.edu.itba.paw.webapp.validation.support.MultipartImageValidation;

/**
 * Encapsulates the OMIT / SUBMIT-WITH-PIC / SUBMIT-WITHOUT-PIC branching that
 * {@code MyReservationsController} used to repeat across the two symmetrical review handlers
 * (owner-reviews-rider and rider-reviews-owner). The facade owns:
 *
 * <ul>
 *   <li>Image validation (content-type + size) and payload extraction.</li>
 *   <li>The call to {@link ReviewService} (per role).</li>
 *   <li>Mapping {@link RiderReservationException} onto the binding via
 *       {@link RiderReservationReviewExceptionMapper}.</li>
 *   <li>Generic {@link RydenException} → {@code rejectValue("picture", ...)} mapping.</li>
 * </ul>
 *
 * The controller is left with selecting the final view (redirect + flash on success/omit-error,
 * detail re-render on validation-error) and supplying request-scoped parameters.
 */
@Component
public final class ReviewSubmissionFacade {

    public enum Role { OWNER, RIDER }

    public enum Outcome { SUCCESS, OMIT_FAILED, NEEDS_RERENDER }

    private final ReviewService reviewService;
    private final MultipartImageValidation multipartImageValidation;
    private final RiderReservationReviewExceptionMapper riderReservationReviewExceptionMapper;
    private final LocaleMessages localeMessages;

    public ReviewSubmissionFacade(
            final ReviewService reviewService,
            final MultipartImageValidation multipartImageValidation,
            final RiderReservationReviewExceptionMapper riderReservationReviewExceptionMapper,
            final LocaleMessages localeMessages) {
        this.reviewService = reviewService;
        this.multipartImageValidation = multipartImageValidation;
        this.riderReservationReviewExceptionMapper = riderReservationReviewExceptionMapper;
        this.localeMessages = localeMessages;
    }

    /**
     * Outcome of a review submission call. {@code messageOrNull} carries either the success
     * flash message (for {@link Outcome#SUCCESS}) or the OMIT-path error flash message (for
     * {@link Outcome#OMIT_FAILED}). For {@link Outcome#NEEDS_RERENDER} the binding result has
     * been populated with field/global errors and the caller should re-render the detail page.
     */
    public record SubmissionResult(Outcome outcome, String messageOrNull) {
        public static SubmissionResult success(final String msg) {
            return new SubmissionResult(Outcome.SUCCESS, msg);
        }

        public static SubmissionResult omitFailed(final String msg) {
            return new SubmissionResult(Outcome.OMIT_FAILED, msg);
        }

        public static SubmissionResult needsRerender() {
            return new SubmissionResult(Outcome.NEEDS_RERENDER, null);
        }
    }

    /**
     * Runs the full submission pipeline for either review form. Picks the correct
     * {@link ReviewService} call based on the viewer's role and centralises the image-payload
     * extraction so the controller does not have to read {@code MultipartFile.getBytes()}
     * itself in two places.
     */
    public SubmissionResult submit(
            final long viewerId,
            final long reservationId,
            final Role viewerRole,
            final ReservationReviewForm form,
            final BindingResult binding) {
        final String successMsg = localeMessages.msg("myReservationDetail.review.success");

        if (form.getReviewAction() == ReservationReviewAction.OMIT) {
            try {
                submitWithoutPicture(viewerId, reservationId, viewerRole);
                return SubmissionResult.success(successMsg);
            } catch (final RiderReservationException ex) {
                return SubmissionResult.omitFailed(localeMessages.msg(ex));
            }
        }

        validatePicture(form.getPicture(), binding);
        if (binding.hasErrors()) {
            return SubmissionResult.needsRerender();
        }

        final ReviewImagePayload picturePayload;
        try {
            picturePayload = readPicturePayload(form.getPicture());
        } catch (final IOException e) {
            binding.rejectValue("picture", "profile.picture.readFailed",
                    localeMessages.msg("profile.picture.readFailed"));
            return SubmissionResult.needsRerender();
        }

        try {
            submitWithPicture(viewerId, reservationId, viewerRole, form, picturePayload);
            return SubmissionResult.success(successMsg);
        } catch (final RiderReservationException ex) {
            riderReservationReviewExceptionMapper.mergeOntoBinding(ex, binding);
            return SubmissionResult.needsRerender();
        } catch (final RydenException ex) {
            binding.rejectValue("picture", ex.getMessageCode(), ex.getMessageArgs(), localeMessages.msg(ex));
            return SubmissionResult.needsRerender();
        }
    }

    private void submitWithoutPicture(
            final long viewerId, final long reservationId, final Role viewerRole) {
        if (viewerRole == Role.OWNER) {
            reviewService.submitOwnerReviewOfRider(viewerId, reservationId, null, null);
        } else {
            reviewService.submitRiderReviewOfOwner(viewerId, reservationId, null, null);
        }
    }

    private void submitWithPicture(
            final long viewerId,
            final long reservationId,
            final Role viewerRole,
            final ReservationReviewForm form,
            final ReviewImagePayload payload) {
        if (viewerRole == Role.OWNER) {
            reviewService.submitOwnerReviewOfRider(
                    viewerId, reservationId, form.getRating(), form.getComment(),
                    payload.name, payload.contentType, payload.bytes);
        } else {
            reviewService.submitRiderReviewOfOwner(
                    viewerId, reservationId, form.getRating(), form.getComment(),
                    payload.name, payload.contentType, payload.bytes);
        }
    }

    private void validatePicture(final MultipartFile picture, final BindingResult binding) {
        if (picture == null || picture.isEmpty()) {
            return;
        }
        final MultipartFile[] singleton = new MultipartFile[] { picture };
        if (!multipartImageValidation.validateFilesAreImages(singleton, binding, "picture")) {
            return;
        }
        multipartImageValidation.validateFilesWithinMaxSize(singleton, binding);
    }

    private static ReviewImagePayload readPicturePayload(final MultipartFile picture) throws IOException {
        if (picture == null || picture.isEmpty()) {
            return ReviewImagePayload.empty();
        }
        return new ReviewImagePayload(picture.getOriginalFilename(), picture.getContentType(), picture.getBytes());
    }

    private static final class ReviewImagePayload {
        private final String name;
        private final String contentType;
        private final byte[] bytes;

        private ReviewImagePayload(final String name, final String contentType, final byte[] bytes) {
            this.name = name;
            this.contentType = contentType;
            this.bytes = bytes;
        }

        private static ReviewImagePayload empty() {
            return new ReviewImagePayload(null, null, null);
        }
    }
}
