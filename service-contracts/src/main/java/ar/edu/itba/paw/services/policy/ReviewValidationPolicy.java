package ar.edu.itba.paw.services.policy;

/**
 * Limits for listing/reservation review comments. Config: {@code app.validation.review-comment-max-length}.
 * Instances are created via {@link #fromValidatedCommentMaxLength(int)}.
 */
public final class ReviewValidationPolicy {

    private final int commentMaxLength;

    public static ReviewValidationPolicy fromValidatedCommentMaxLength(final int commentMaxLength) {
        if (commentMaxLength < 1) {
            throw new IllegalArgumentException(
                    "app.validation.review-comment-max-length must be >= 1, got " + commentMaxLength);
        }
        return new ReviewValidationPolicy(commentMaxLength);
    }

    private ReviewValidationPolicy(final int commentMaxLength) {
        this.commentMaxLength = commentMaxLength;
    }

    public int getCommentMaxLength() {
        return commentMaxLength;
    }
}
