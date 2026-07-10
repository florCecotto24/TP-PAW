package ar.edu.itba.paw.webapp.support;

import org.springframework.stereotype.Component;

import ar.edu.itba.paw.dto.PublishCarRequest;
import ar.edu.itba.paw.exception.car.CarModelNotFoundException;
import ar.edu.itba.paw.models.domain.car.Car;
import ar.edu.itba.paw.models.domain.car.CarModel;
import ar.edu.itba.paw.services.car.CarModelService;
import ar.edu.itba.paw.webapp.form.car.CarCreateForm;
import ar.edu.itba.paw.webapp.util.ModelUriSupport;

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
                .minimumRentalDays(form.getMinimumRentalDays())
                .build();
    }

    private ResolvedCatalog resolveCatalog(final CarCreateForm form) {
        if (form.getModelUri() != null && !form.getModelUri().isBlank()) {
            final long modelId = ModelUriSupport.parseModelId(form.getModelUri());
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

    private record ResolvedCatalog(String brand, String model, Car.Type type) {
    }
}
