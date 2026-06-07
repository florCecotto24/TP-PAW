package ar.edu.itba.paw.exception.car;

import ar.edu.itba.paw.exception.MessageKeys;
import ar.edu.itba.paw.exception.RydenException;

/**
 * Raised when an admin operation targets a model id that no longer exists. Replaces the previous
 * {@link IllegalArgumentException} thrown from the validate / reject model flows.
 */
public final class CarModelNotFoundException extends RydenException {

    private final long modelId;

    public CarModelNotFoundException(final long modelId) {
        super(MessageKeys.CATALOG_MODEL_NOT_FOUND);
        this.modelId = modelId;
    }

    public long getModelId() {
        return modelId;
    }
}
