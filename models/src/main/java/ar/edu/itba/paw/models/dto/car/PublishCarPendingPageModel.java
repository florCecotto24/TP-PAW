package ar.edu.itba.paw.models.dto.car;


import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;

/**
 * Page model for {@code car/publishCarPending.jsp}, served after a successful publish whose
 * catalog entry (brand and/or model) is still pending admin validation.
 *
 * <p>The controller for {@code GET /publish-car/pending/{carId}} should redirect to the
 * regular detail page whenever this model cannot be built (car missing, not owned by viewer,
 * or catalog already validated) — see {@code CarPublishViewService#loadPendingView}.</p>
 */
public final class PublishCarPendingPageModel {

    private final long createdCarId;
    private final String pendingBrandOrNull;
    private final String pendingModelOrNull;

    public PublishCarPendingPageModel(
            final long createdCarId,
            final String pendingBrandOrNull,
            final String pendingModelOrNull) {
        this.createdCarId = createdCarId;
        this.pendingBrandOrNull = pendingBrandOrNull;
        this.pendingModelOrNull = pendingModelOrNull;
    }

    public long getCreatedCarId() {
        return createdCarId;
    }

    public Optional<String> getPendingBrand() {
        return Optional.ofNullable(pendingBrandOrNull);
    }

    public Optional<String> getPendingModel() {
        return Optional.ofNullable(pendingModelOrNull);
    }

    public void populateModel(final BiConsumer<String, Object> sink) {
        Objects.requireNonNull(sink, "sink");
        sink.accept("createdCarId", createdCarId);
        if (pendingBrandOrNull != null) {
            sink.accept("pendingBrand", pendingBrandOrNull);
        }
        if (pendingModelOrNull != null) {
            sink.accept("pendingModel", pendingModelOrNull);
        }
    }
}
