package ar.edu.itba.paw.webapp.support;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.springframework.stereotype.Component;

import ar.edu.itba.paw.exception.car.CarBrandNotFoundException;
import ar.edu.itba.paw.exception.car.CarModelNotFoundException;
import ar.edu.itba.paw.exception.car.CarNotFoundException;
import ar.edu.itba.paw.models.domain.car.Car;
import ar.edu.itba.paw.models.domain.car.CarModel;
import ar.edu.itba.paw.models.dto.Page;
import ar.edu.itba.paw.services.car.CarBrandService;
import ar.edu.itba.paw.services.car.CarMarketInsightService;
import ar.edu.itba.paw.services.car.CarModelService;
import ar.edu.itba.paw.services.car.CarService;
import ar.edu.itba.paw.services.user.AdminService;
import ar.edu.itba.paw.webapp.api.common.PaginationLinks;
import ar.edu.itba.paw.webapp.dto.rest.ModelDto;
import ar.edu.itba.paw.webapp.dto.rest.PriceMarketInsightDto;

/**
 * HTTP orchestration for the admin pending-model collection ({@code GET /models}) and nested
 * {@code /brands/{id}/models/{modelId}} item handlers.
 */
@Component
public final class ModelCatalogSupport {

    private final CarBrandService carBrandService;
    private final CarModelService carModelService;
    private final CarService carService;
    private final CarMarketInsightService carMarketInsightService;
    private final AdminService adminService;

    public ModelCatalogSupport(
            final CarBrandService carBrandService,
            final CarModelService carModelService,
            final CarService carService,
            final CarMarketInsightService carMarketInsightService,
            final AdminService adminService) {
        this.carBrandService = carBrandService;
        this.carModelService = carModelService;
        this.carService = carService;
        this.carMarketInsightService = carMarketInsightService;
        this.adminService = adminService;
    }

    /**
     * Admin moderation queue of PENDING models. An omitted {@code validated} defaults to the pending
     * view; {@code validated=true} is rejected because validated models are reached via their brand.
     */
    public Response listPendingModels(
            final Boolean validated,
            final PaginationParams paging,
            final UriInfo uriInfo) {
        if (Boolean.TRUE.equals(validated)) {
            throw new BadRequestException(
                    "GET /models only serves the pending-model moderation queue; use validated=false (or omit it). "
                            + "Validated models are reached via their brand.");
        }
        final Page<CarModel> modelPage =
                carModelService.findPendingPage(paging.getZeroBasedPage(), paging.getPageSize());
        final List<ModelDto> dtos = modelPage.getContent().stream()
                .map(model -> ModelDto.from(model, uriInfo))
                .collect(Collectors.toList());
        if (dtos.isEmpty()) {
            return Response.noContent().build();
        }
        final Response.ResponseBuilder builder = Response.ok(new GenericEntity<List<ModelDto>>(dtos) {})
                .header("X-Total-Count", modelPage.getTotalItems());
        PaginationLinks.add(
                builder, uriInfo, paging.getPage(), paging.getPageSize(), (int) modelPage.getTotalItems());
        return builder.build();
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
        return carMarketInsightService.getPriceMarketInsightForCar(car, excludeCarId)
                .map(insight -> Response.ok(PriceMarketInsightDto.from(insight)).build())
                .orElse(Response.noContent().build());
    }
}
