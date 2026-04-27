package ar.edu.itba.paw.webapp.validation;

/**
 * Marker interfaces for Bean Validation {@code groups} (never implemented by types).
 * Use with {@link org.springframework.validation.annotation.Validated} on controller parameters.
 */
public final class ValidationGroups {

    private ValidationGroups() {
    }

    /** Registration account form ({@code /register}). */
    public interface OnRegistration {
    }

    /** Profile data update ({@code /profile} POST). */
    public interface OnProfileUpdate {
    }

    /** Profile password change ({@code /profile/password} POST). */
    public interface OnProfilePassword {
    }

    /** Owner listing edit form ({@code /my-listings/{id}/edit}). */
    public interface OnListingEdit {
    }

    /** Rider reservation submit ({@code /reservation} POST). */
    public interface OnReservationSubmit {
    }

    /** Forgot-password reset with code ({@code /forgot-password/reset} POST). */
    public interface OnForgotPasswordReset {
    }

    /** Publish car wizard submit ({@code /publish-car} POST). */
    public interface OnPublishCar {
    }
}
