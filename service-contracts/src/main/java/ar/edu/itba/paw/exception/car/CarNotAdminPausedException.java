package ar.edu.itba.paw.exception.car;

import ar.edu.itba.paw.exception.MessageKeys;
import ar.edu.itba.paw.exception.RydenException;

/**
 * Raised when an admin attempts to release the admin pause on a car that isn't currently in the
 * {@code ADMIN_PAUSED} state. Replaces {@link IllegalStateException} so the controller can render a
 * localized message and a graceful redirect.
 */
public final class CarNotAdminPausedException extends RydenException {

    private final long carId;

    public CarNotAdminPausedException(final long carId) {
        super(MessageKeys.CAR_RESUME_NOT_ADMIN_PAUSED);
        this.carId = carId;
    }

    public long getCarId() {
        return carId;
    }
}
