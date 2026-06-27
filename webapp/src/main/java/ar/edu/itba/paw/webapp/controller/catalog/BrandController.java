package ar.edu.itba.paw.webapp.controller.catalog;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.security.RolesAllowed;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PATCH;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ar.edu.itba.paw.exception.car.CarBrandNotFoundException;
import ar.edu.itba.paw.models.domain.car.Car;
import ar.edu.itba.paw.models.domain.car.CarBrand;
import ar.edu.itba.paw.models.domain.car.CarModel;
import ar.edu.itba.paw.services.car.CarBrandService;
import ar.edu.itba.paw.services.car.CarModelService;
import ar.edu.itba.paw.services.user.AdminService;
import ar.edu.itba.paw.webapp.api.common.PaginationLinks;
import ar.edu.itba.paw.webapp.api.common.VndMediaType;
import ar.edu.itba.paw.webapp.dto.rest.BrandDto;
import ar.edu.itba.paw.webapp.dto.rest.ModelDto;
import ar.edu.itba.paw.webapp.form.catalog.BrandCreateForm;
import ar.edu.itba.paw.webapp.form.catalog.CatalogApprovalForm;
import ar.edu.itba.paw.webapp.form.catalog.ModelCreateForm;
import ar.edu.itba.paw.webapp.support.CarRestEnums;
import ar.edu.itba.paw.webapp.support.CurrentUserResolver;

/**
 * Catalog brands ({@code /brands}, {@code /brands/{id}}, {@code /brands/{id}/models}).
 * Replaces admin catalog MVC flows for brand moderation (decision D6).
 */
@Path("/brands")
@Component
public final class BrandController {

    private final CarBrandService carBrandService;
    private final CarModelService carModelService;
    private final AdminService adminService;
    private final CurrentUserResolver currentUserResolver;

    @Context
    private UriInfo uriInfo;

    @Autowired
    public BrandController(
            final CarBrandService carBrandService,
            final CarModelService carModelService,
            final AdminService adminService,
            final CurrentUserResolver currentUserResolver) {
        this.carBrandService = carBrandService;
        this.carModelService = carModelService;
        this.adminService = adminService;
        this.currentUserResolver = currentUserResolver;
    }

    @GET
    @Produces(VndMediaType.BRAND_V1_JSON)
    public Response listBrands(
            @QueryParam("validated") final Boolean validated,
            @QueryParam("page") @javax.ws.rs.DefaultValue("1") final int page,
            @QueryParam("pageSize") @javax.ws.rs.DefaultValue("12") final int pageSize) {
        final List<CarBrand> all;
        if (validated == null) {
            all = carBrandService.findAllOrdered();
        } else if (validated) {
            all = carBrandService.findValidatedOrdered();
        } else {
            all = carBrandService.findPendingOrdered();
        }

        final int safePage = Math.max(1, page);
        final int safeSize = Math.max(1, pageSize);
        final int total = all.size();
        final int fromIndex = Math.min((safePage - 1) * safeSize, total);
        final int toIndex = Math.min(fromIndex + safeSize, total);
        final List<BrandDto> dtos = all.subList(fromIndex, toIndex).stream()
                .map(brand -> BrandDto.from(brand, uriInfo))
                .collect(Collectors.toList());

        if (dtos.isEmpty()) {
            return Response.noContent().build();
        }

        final Response.ResponseBuilder builder =
                Response.ok(new GenericEntity<List<BrandDto>>(dtos) {});
        PaginationLinks.add(builder, uriInfo, safePage, safeSize, total);
        return builder.build();
    }

    @POST
    @Consumes(VndMediaType.BRAND_V1_JSON)
    @Produces(VndMediaType.BRAND_V1_JSON)
    public Response createBrand(@Valid final BrandCreateForm form) {
        currentUserResolver.requireUserId();
        final CarBrand brand = carBrandService.findOrCreateUnvalidated(form.getName())
                .orElseThrow(() -> new javax.ws.rs.BadRequestException("Brand name must not be blank."));

        final URI location = uriInfo.getBaseUriBuilder()
                .path("brands")
                .path(String.valueOf(brand.getId()))
                .build();
        return Response.created(location).entity(BrandDto.from(brand, uriInfo)).build();
    }

    @GET
    @Path("/{id}")
    @Produces(VndMediaType.BRAND_V1_JSON)
    public Response getBrand(@PathParam("id") final long id) {
        final CarBrand brand = carBrandService.findById(id)
                .orElseThrow(() -> new CarBrandNotFoundException(id));
        return Response.ok(BrandDto.from(brand, uriInfo)).build();
    }

    @PATCH
    @Path("/{id}")
    @RolesAllowed("ROLE_ADMIN")
    @Consumes(VndMediaType.BRAND_V1_JSON)
    @Produces(VndMediaType.BRAND_V1_JSON)
    public Response approveBrand(@PathParam("id") final long id, @Valid final CatalogApprovalForm patch) {
        carBrandService.findById(id)
                .orElseThrow(() -> new CarBrandNotFoundException(id));

        adminService.validateCarBrand(id);

        final CarBrand updated = carBrandService.findById(id)
                .orElseThrow(() -> new CarBrandNotFoundException(id));
        return Response.ok(BrandDto.from(updated, uriInfo)).build();
    }

    @DELETE
    @Path("/{id}")
    @RolesAllowed("ROLE_ADMIN")
    public Response rejectBrand(@PathParam("id") final long id) {
        if (carBrandService.findById(id).isEmpty()) {
            throw new CarBrandNotFoundException(id);
        }
        adminService.rejectCarBrand(id);
        return Response.noContent().build();
    }

    @GET
    @Path("/{id}/models")
    @Produces(VndMediaType.MODEL_V1_JSON)
    public Response listModelsForBrand(
            @PathParam("id") final long id,
            @QueryParam("validated") final Boolean validated) {
        carBrandService.findById(id)
                .orElseThrow(() -> new CarBrandNotFoundException(id));

        final List<CarModel> models = Boolean.TRUE.equals(validated)
                ? carModelService.findValidatedByBrandIdOrdered(id)
                : carModelService.findByBrandIdOrdered(id);
        final List<ModelDto> dtos = models.stream()
                .map(model -> ModelDto.from(model, uriInfo))
                .collect(Collectors.toList());
        if (dtos.isEmpty()) {
            return Response.noContent().build();
        }
        return Response.ok(new GenericEntity<List<ModelDto>>(dtos) {}).build();
    }

    @POST
    @Path("/{id}/models")
    @Consumes(VndMediaType.MODEL_V1_JSON)
    @Produces(VndMediaType.MODEL_V1_JSON)
    public Response createModelForBrand(
            @PathParam("id") final long id,
            @Valid final ModelCreateForm form) {
        currentUserResolver.requireUserId();
        carBrandService.findById(id)
                .orElseThrow(() -> new CarBrandNotFoundException(id));

        final Car.Type type = CarRestEnums.parseType(form.getType());
        final CarModel model = carModelService.findOrCreateUnvalidated(id, form.getName(), type)
                .orElseThrow(() -> new javax.ws.rs.BadRequestException("Model name must not be blank."));

        final URI location = uriInfo.getBaseUriBuilder()
                .path("models")
                .path(String.valueOf(model.getId()))
                .build();
        return Response.created(location).entity(ModelDto.from(model, uriInfo)).build();
    }
}
