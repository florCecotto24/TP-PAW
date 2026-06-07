package ar.edu.itba.paw.exception.car;

import ar.edu.itba.paw.exception.MessageKeys;
import ar.edu.itba.paw.exception.RydenException;

/**
 * Raised when an operation references a car id that doesn't exist. Replaces the previous practice
 * of throwing {@link IllegalArgumentException} from the service tier for missing-row lookups, so
 * callers can map "not found" to a 404-style response without {@code instanceof} checks.
 */
public final class CarNotFoundException extends RydenException {

    private final long carId;

    public CarNotFoundException(final long carId) {
        super(MessageKeys.CAR_NOT_FOUND);
        this.carId = carId;
    }

    public long getCarId() {
        return carId;
    }
}
