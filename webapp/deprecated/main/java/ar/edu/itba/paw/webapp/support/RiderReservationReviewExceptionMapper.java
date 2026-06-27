package ar.edu.itba.paw.webapp.support;

import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;

import ar.edu.itba.paw.exception.MessageKeys;
import ar.edu.itba.paw.exception.reservation.RiderReservationException;

@Component
public final class RiderReservationReviewExceptionMapper {

    /** Maps domain review failures onto {@link BindingResult} for rendering {@code form:errors}. */
    public void mergeOntoBinding(final RiderReservationException ex, final BindingResult binding) {
        final String code = ex.getMessageCode();
        final Object[] args = ex.getMessageArgs();
        if (MessageKeys.REVIEW_COMMENT_TOO_LONG.equals(code)) {
            binding.rejectValue("comment", code, args, null);
            return;
        }
        if (MessageKeys.REVIEW_RATING_INVALID.equals(code) || MessageKeys.REVIEW_RATING_REQUIRED_WHEN_COMMENT.equals(code)) {
            binding.rejectValue("rating", code, args, null);
            return;
        }
        binding.reject(code, args, null);
    }
}
