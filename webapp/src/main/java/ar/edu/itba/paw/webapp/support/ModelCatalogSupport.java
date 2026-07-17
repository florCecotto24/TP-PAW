package ar.edu.itba.paw.webapp.support;

import java.util.Locale;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.springframework.stereotype.Component;

import ar.edu.itba.paw.exception.car.CarBrandNotFoundException;
import ar.edu.itba.paw.exception.car.CarModelNotFoundException;
import ar.edu.itba.paw.exception.car.CarNotFoundException;
import ar.edu.itba.paw.models.domain.car.Car;
import ar.edu.itba.paw.models.domain.car.CarModel;
import ar.edu.itba.paw.services.car.CarBrandService;
import ar.edu.itba.paw.services.car.CarModelService;
import ar.edu.itba.paw.services.car.CarService;
import ar.edu.itba.paw.services.user.AdminService;
import ar.edu.itba.paw.webapp.dto.rest.ModelDto;
import ar.edu.itba.paw.webapp.dto.rest.PriceMarketInsightDto;

/** Shared catalog model item operations for nested {@code /brands/{id}/models/{modelId}} handlers. */
@Component
public final class ModelCatalogSupport {

    private final CarBrandService carBrandService;
    private final CarModelService carModelService;
    private final CarService carService;
    private final AdminService adminService;

    public ModelCatalogSupport(
            final CarBrandService carBrandService,
            final CarModelService carModelService,
            final CarService carService,
            final AdminService adminService) {
        this.carBrandService = carBrandService;
        this.carModelService = carModelService;
        this.carService = carService;
        this.adminService = adminService;
    }

    public CarModel requireModelForBrand(final long brandId, final long modelId) {
        carBrandService.findById(brandId)
                .orElseThrow(() -> new CarBrandNotFoundException(brandId));
        final CarModel model = carModelService.findById(modelId)
                .orElseThrow(() -> new CarModelNotFoundException(modelId));
        if (model.getBrandId() != brandId) {
            throw new CarModelNotFoundException(modelId);
        }
        return model;
    }

    public ModelDto toDto(final CarModel model, final UriInfo uriInfo) {
        return ModelDto.from(model, uriInfo);
    }

    public void approveModel(final long modelId, final Locale locale) {
        carModelService.findById(modelId)
                .orElseThrow(() -> new CarModelNotFoundException(modelId));
        adminService.validateCatalogEntry(modelId, locale);
    }

    public void rejectModel(final long modelId, final Locale locale) {
        adminService.rejectCatalogEntry(modelId, locale);
    }

    public Response priceInsightResponse(final long modelId, final Long excludeCarId) {
        carModelService.findById(modelId)
                .orElseThrow(() -> new CarModelNotFoundException(modelId));
        if (excludeCarId == null) {
            return Response.noContent().build();
        }
        final Car car = carService.getCarById(excludeCarId)
                .orElseThrow(() -> new CarNotFoundException(excludeCarId));
        final long carModelId = car.getCarModel()
                .map(CarModel::getId)
                .orElse(-1L);
        if (carModelId != modelId) {
            throw new CarNotFoundException(excludeCarId);
        }
        return carService.getPriceMarketInsightForCar(car, excludeCarId)
                .map(insight -> Response.ok(PriceMarketInsightDto.from(insight)).build())
                .orElse(Response.noContent().build());
    }
}
