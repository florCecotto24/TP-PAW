package ar.edu.itba.paw.exception.car;

import ar.edu.itba.paw.exception.MessageKeys;
import ar.edu.itba.paw.exception.RydenException;

/**
 * Raised when an admin operation targets a brand id that no longer exists. Replaces the previous
 * {@link IllegalArgumentException} thrown from the validate / reject brand flows so the controller
 * can map this case to a flash error or 404 without sniffing exception messages.
 */
public final class CarBrandNotFoundException extends RydenException {

    private final long brandId;

    public CarBrandNotFoundException(final long brandId) {
        super(MessageKeys.CATALOG_BRAND_NOT_FOUND);
        this.brandId = brandId;
    }

    public long getBrandId() {
        return brandId;
    }
}
