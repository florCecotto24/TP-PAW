package ar.edu.itba.paw.exception.admin;

import ar.edu.itba.paw.exception.MessageKeys;

/**
 * Raised when an administrator tries to admin-pause a listing whose owner is itself an
 * administrator. Replaces the previous {@link IllegalArgumentException}; admin-vs-admin moderation
 * is intentionally a separate (manual) operation so this case is reported as a domain rule
 * violation rather than a programmer error.
 */
public final class AdminCannotPauseAdminCarException extends AdminException {

    private final long carId;

    public AdminCannotPauseAdminCarException(final long carId) {
        super(MessageKeys.ADMIN_PAUSE_CANNOT_PAUSE_ADMIN_CAR);
        this.carId = carId;
    }

    public long getCarId() {
        return carId;
    }
}
