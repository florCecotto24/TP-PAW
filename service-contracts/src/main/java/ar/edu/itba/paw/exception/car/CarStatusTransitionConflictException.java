package ar.edu.itba.paw.exception.car;

import ar.edu.itba.paw.exception.MessageKeys;
import ar.edu.itba.paw.exception.RydenException;

/**
 * Raised when a car status transition is syntactically valid but forbidden on that verb
 * (e.g. deactivation via {@code PATCH} — use {@code DELETE} instead).
 */
public final class CarStatusTransitionConflictException extends RydenException {

    private final long carId;

    public CarStatusTransitionConflictException(final long carId) {
        super(MessageKeys.CAR_INVALID_STATUS_TRANSITION);
        this.carId = carId;
    }

    public long getCarId() {
        return carId;
    }
}
