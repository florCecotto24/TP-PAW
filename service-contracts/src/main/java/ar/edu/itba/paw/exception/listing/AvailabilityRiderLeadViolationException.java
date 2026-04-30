package ar.edu.itba.paw.exception.listing;

/**
 * Publication availability row whose start date is before the first wall day allowed by rider pickup lead time.
 */
public final class AvailabilityRiderLeadViolationException extends ListingValidationException {

    private final int availabilityRowIndex;

    public AvailabilityRiderLeadViolationException(
            final int availabilityRowIndex,
            final String messageCode,
            final Object... messageArgs) {
        super(messageCode, messageArgs);
        this.availabilityRowIndex = availabilityRowIndex;
    }

    public int getAvailabilityRowIndex() {
        return availabilityRowIndex;
    }
}
