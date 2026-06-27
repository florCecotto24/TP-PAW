package ar.edu.itba.paw.webapp.support;

import java.net.URI;
import java.time.Year;

import org.springframework.stereotype.Component;

import ar.edu.itba.paw.dto.PublishCarRequest;
import ar.edu.itba.paw.exception.car.CarModelNotFoundException;
import ar.edu.itba.paw.models.domain.car.Car;
import ar.edu.itba.paw.models.domain.car.CarModel;
import ar.edu.itba.paw.policy.CarValidationPolicy;
import ar.edu.itba.paw.services.car.CarModelService;
import ar.edu.itba.paw.webapp.form.car.CarCreateForm;

/** Maps REST {@link CarCreateForm} payloads to {@link PublishCarRequest}. */
@Component
public final class CarCreateRequestSupport {

    private final CarModelService carModelService;
    private final CarValidationPolicy carValidationPolicy;

    public CarCreateRequestSupport(
            final CarModelService carModelService,
            final CarValidationPolicy carValidationPolicy) {
        this.carModelService = carModelService;
        this.carValidationPolicy = carValidationPolicy;
    }

    public PublishCarRequest toPublishRequest(final CarCreateForm form) {
        validateYear(form.getYear());
        validateCatalogFields(form);
        final ResolvedCatalog catalog = resolveCatalog(form);
        return PublishCarRequest.builder()
                .brand(catalog.brand())
                .model(catalog.model())
                .type(catalog.type())
                .plate(form.getPlate())
                .year(form.getYear())
                .powertrain(parsePowertrain(form.getPowertrain()))
                .transmission(parseTransmission(form.getTransmission()))
                .description(form.getDescription())
                .build();
    }

    public void validateCatalogFields(final CarCreateForm form) {
        if (form.getModelUri() != null && !form.getModelUri().isBlank()) {
            parseModelId(form.getModelUri());
            return;
        }
        if (form.getBrandName() == null || form.getBrandName().isBlank()) {
            throw new javax.ws.rs.BadRequestException("brandName is required when modelUri is absent.");
        }
        if (form.getModelName() == null || form.getModelName().isBlank()) {
            throw new javax.ws.rs.BadRequestException("modelName is required when modelUri is absent.");
        }
        if (form.getType() == null || form.getType().isBlank()) {
            throw new javax.ws.rs.BadRequestException("type is required when modelUri is absent.");
        }
        if (CarRestEnums.parseType(form.getType()) == null) {
            throw new javax.ws.rs.BadRequestException("Unknown car type: " + form.getType());
        }
    }

    private void validateYear(final Integer year) {
        if (year == null) {
            return;
        }
        final int yearMin = carValidationPolicy.getYearMin();
        if (year < yearMin) {
            throw new javax.ws.rs.BadRequestException("year must be >= " + yearMin);
        }
        if (year > Year.now().getValue()) {
            throw new javax.ws.rs.BadRequestException("year must be <= " + Year.now().getValue());
        }
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

    private static Car.Powertrain parsePowertrain(final String raw) {
        final Car.Powertrain parsed = CarRestEnums.parsePowertrain(raw);
        if (parsed == null) {
            throw new javax.ws.rs.BadRequestException("Unknown powertrain: " + raw);
        }
        return parsed;
    }

    private static Car.Transmission parseTransmission(final String raw) {
        final Car.Transmission parsed = CarRestEnums.parseTransmission(raw);
        if (parsed == null) {
            throw new javax.ws.rs.BadRequestException("Unknown transmission: " + raw);
        }
        return parsed;
    }

    private static long parseModelId(final String modelUri) {
        try {
            final URI uri = URI.create(modelUri.trim());
            final String path = uri.getPath();
            if (path == null || path.isBlank()) {
                throw new IllegalArgumentException("empty path");
            }
            final String[] segments = path.split("/");
            for (int i = segments.length - 1; i >= 0; i--) {
                if (!segments[i].isBlank()) {
                    return Long.parseLong(segments[i]);
                }
            }
            throw new IllegalArgumentException("no id segment");
        } catch (final RuntimeException ex) {
            throw new javax.ws.rs.BadRequestException("Invalid modelUri: " + modelUri);
        }
    }

    private record ResolvedCatalog(String brand, String model, Car.Type type) {
    }
}
