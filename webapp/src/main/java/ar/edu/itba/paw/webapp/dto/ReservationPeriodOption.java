package ar.edu.itba.paw.webapp.dto;

public final class ReservationPeriodOption {

    private final long availabilityId;
    private final String label;
    private final String minDateTimeLocal;
    private final String maxDateTimeLocal;

    public ReservationPeriodOption(
            final long availabilityId,
            final String label,
            final String minDateTimeLocal,
            final String maxDateTimeLocal) {
        this.availabilityId = availabilityId;
        this.label = label;
        this.minDateTimeLocal = minDateTimeLocal;
        this.maxDateTimeLocal = maxDateTimeLocal;
    }

    public long getAvailabilityId() {
        return availabilityId;
    }

    public String getLabel() {
        return label;
    }

    public String getMinDateTimeLocal() {
        return minDateTimeLocal;
    }

    public String getMaxDateTimeLocal() {
        return maxDateTimeLocal;
    }
}
