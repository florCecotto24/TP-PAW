package ar.edu.itba.paw.exception.car;

import ar.edu.itba.paw.exception.MessageKeys;
import ar.edu.itba.paw.exception.RydenException;

/**
 * Raised when a user attempts to publish a car without meeting the account-level prerequisites
 * (valid CBU, identity document uploaded). Mapped to {@code 403} — see
 * {@code RydenExceptionHttpStatus}.
 */
public final class CarPublishPrerequisitesMissingException extends RydenException {

    public CarPublishPrerequisitesMissingException() {
        super(MessageKeys.PUBLISH_PREREQUISITES_MISSING);
    }
}
