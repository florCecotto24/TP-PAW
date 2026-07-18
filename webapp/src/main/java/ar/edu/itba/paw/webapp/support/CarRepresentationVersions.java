package ar.edu.itba.paw.webapp.support;

import ar.edu.itba.paw.models.domain.car.Car;

/** Version tokens for conditional GET on {@code /cars/{id}} (summary vs detail MIME). */
public final class CarRepresentationVersions {

    public static final String SUMMARY = "summary";
    public static final String DETAIL = "detail";
    public static final String PRIVATE = "private";

    private CarRepresentationVersions() {
    }

    /**
     * Stable ETag value from car id, representation key and {@link Car#getUpdatedAt()}.
     * Distinct MIME shapes on the same URI get distinct tags via {@code representationKey}.
     */
    public static String etagValue(final Car car, final String representationKey) {
        final long versionMillis = car.getUpdatedAt() == null
                ? 0L
                : car.getUpdatedAt().toInstant().toEpochMilli();
        return "car-" + car.getId() + "-" + representationKey + "-" + versionMillis;
    }
}
