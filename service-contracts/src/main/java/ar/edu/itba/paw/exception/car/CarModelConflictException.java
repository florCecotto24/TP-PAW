package ar.edu.itba.paw.exception.car;

import ar.edu.itba.paw.exception.MessageKeys;
import ar.edu.itba.paw.exception.RydenException;

/**
 * Raised when {@code POST /brands/{id}/models} attempts to create a model whose name already exists
 * for that brand.
 */
public final class CarModelConflictException extends RydenException {

    private final long existingModelId;

    public CarModelConflictException(final long existingModelId) {
        super(MessageKeys.CATALOG_MODEL_ALREADY_EXISTS);
        this.existingModelId = existingModelId;
    }

    public long getExistingModelId() {
        return existingModelId;
    }
}
