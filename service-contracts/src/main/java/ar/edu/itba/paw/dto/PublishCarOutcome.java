package ar.edu.itba.paw.dto;

import java.util.Optional;

import ar.edu.itba.paw.models.domain.Car;

/**
 * Outcome of {@code CarPublishingService.publishCar}. Two flavours:
 *

 * {@link Kind#PUBLISHED} — the car is live (either the catalog entry was already
 * validated, or it was new but an admin auto-validated it before publishing).
 * {@link Kind#PENDING_VALIDATION} — a non-admin owner introduced a new brand/model
 * combination, so the catalog entry needs admin review. The car was still persisted so
 * it shows up under "my-cars" in pending state. The view should explain which fields
 * are pending; {@link #getPendingBrandName()} / {@link #getPendingModelName()} return
 * the newly created names that still need approval (empty when the corresponding
 * catalog row was already validated).
 */
public final class PublishCarOutcome {

    public enum Kind { PUBLISHED, PENDING_VALIDATION }

    private final Kind kind;
    private final Car car;
    private final boolean newCatalogEntry;
    private final String pendingBrandName;
    private final String pendingModelName;

    private PublishCarOutcome(
            final Kind kind,
            final Car car,
            final boolean newCatalogEntry,
            final String pendingBrandName,
            final String pendingModelName) {
        this.kind = kind;
        this.car = car;
        this.newCatalogEntry = newCatalogEntry;
        this.pendingBrandName = pendingBrandName;
        this.pendingModelName = pendingModelName;
    }

    public static PublishCarOutcome published(final Car car, final boolean newCatalogEntry) {
        return new PublishCarOutcome(Kind.PUBLISHED, car, newCatalogEntry, null, null);
    }

    public static PublishCarOutcome pending(
            final Car car, final String pendingBrandName, final String pendingModelName) {
        return new PublishCarOutcome(Kind.PENDING_VALIDATION, car, true, pendingBrandName, pendingModelName);
    }

    public Kind getKind() { return kind; }
    public Car getCar() { return car; }
    public boolean isNewCatalogEntry() { return newCatalogEntry; }
    public Optional<String> getPendingBrandName() { return Optional.ofNullable(pendingBrandName); }
    public Optional<String> getPendingModelName() { return Optional.ofNullable(pendingModelName); }
}
