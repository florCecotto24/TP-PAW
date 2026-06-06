package ar.edu.itba.paw.models.dto.car;


import java.util.Objects;
import java.util.function.BiConsumer;

import ar.edu.itba.paw.models.domain.Car;

/**
 * Page model for {@code car/publishCarConfirmation.jsp}, the success screen after a publish.
 *
 * <p>The controller for {@code GET /publish-car/confirmation/{carId}} should redirect to
 * {@code /my-cars} whenever this model cannot be built (car missing or not owned by viewer)
 * — see {@code CarPublishViewService#loadConfirmationView}.</p>
 */
public final class PublishCarConfirmationPageModel {

    private final Car car;
    private final boolean newCatalogEntry;

    public PublishCarConfirmationPageModel(final Car car, final boolean newCatalogEntry) {
        this.car = Objects.requireNonNull(car, "car");
        this.newCatalogEntry = newCatalogEntry;
    }

    public Car getCar() {
        return car;
    }

    public boolean isNewCatalogEntry() {
        return newCatalogEntry;
    }

    public void populateModel(final BiConsumer<String, Object> sink) {
        Objects.requireNonNull(sink, "sink");
        sink.accept("car", car);
        if (newCatalogEntry) {
            sink.accept("newCatalogEntry", true);
        }
    }
}
