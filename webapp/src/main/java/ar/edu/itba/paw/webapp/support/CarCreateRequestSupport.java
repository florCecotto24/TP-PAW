package ar.edu.itba.paw.webapp.support;

import java.net.URI;

import org.springframework.stereotype.Component;

import ar.edu.itba.paw.dto.PublishCarRequest;
import ar.edu.itba.paw.exception.car.CarModelNotFoundException;
import ar.edu.itba.paw.models.domain.car.Car;
import ar.edu.itba.paw.models.domain.car.CarModel;
import ar.edu.itba.paw.services.car.CarModelService;
import ar.edu.itba.paw.webapp.form.car.CarCreateForm;

/** Maps validated REST {@link CarCreateForm} payloads to {@link PublishCarRequest}. */
@Component
public final class CarCreateRequestSupport {

    private final CarModelService carModelService;

    public CarCreateRequestSupport(final CarModelService carModelService) {
        this.carModelService = carModelService;
    }

    public PublishCarRequest toPublishRequest(final CarCreateForm form) {
        final ResolvedCatalog catalog = resolveCatalog(form);
        return PublishCarRequest.builder()
                .brand(catalog.brand())
                .model(catalog.model())
                .type(catalog.type())
                .plate(form.getPlate())
                .year(form.getYear())
                .powertrain(CarRestEnums.parsePowertrain(form.getPowertrain()))
                .transmission(CarRestEnums.parseTransmission(form.getTransmission()))
                .description(form.getDescription())
                .build();
    }

    private ResolvedCatalog resolveCatalog(final CarCreateForm form) {
        if (form.getModelUri() != null && !form.getModelUri().isBlank()) {
            final long modelId = parseModelId(form.getModelUri());
            final CarModel model = carModelService.findById(modelId)
                    .orElseThrow(() -> new CarModelNotFoundException(modelId));
            return new ResolvedCatalog(
                    model.getBrand().getName(),
                    model.getName(),
                    model.getType());
        }
        return new ResolvedCatalog(
                form.getBrandName(),
                form.getModelName(),
                CarRestEnums.parseType(form.getType()));
    }

    private static long parseModelId(final String modelUri) {
        final URI uri = URI.create(modelUri.trim());
        final String path = uri.getPath();
        if (path == null || path.isBlank()) {
            throw new IllegalStateException("modelUri path is empty");
        }
        final String[] segments = path.split("/");
        for (int i = segments.length - 1; i >= 0; i--) {
            if (!segments[i].isBlank()) {
                return Long.parseLong(segments[i]);
            }
        }
        throw new IllegalStateException("modelUri has no id segment");
    }

    private record ResolvedCatalog(String brand, String model, Car.Type type) {
    }
}
