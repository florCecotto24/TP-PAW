package ar.edu.itba.paw.exception.car;

import ar.edu.itba.paw.exception.MessageKeys;
import ar.edu.itba.paw.exception.RydenException;

/**
 * Raised when {@code POST /brands} attempts to create a brand whose name already exists.
 */
public final class CarBrandConflictException extends RydenException {

    private final long existingBrandId;

    public CarBrandConflictException(final long existingBrandId) {
        super(MessageKeys.CATALOG_BRAND_ALREADY_EXISTS);
        this.existingBrandId = existingBrandId;
    }

    public long getExistingBrandId() {
        return existingBrandId;
    }
}
