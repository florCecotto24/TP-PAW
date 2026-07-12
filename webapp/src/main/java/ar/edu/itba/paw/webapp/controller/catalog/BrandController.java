package ar.edu.itba.paw.webapp.controller.catalog;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import ar.edu.itba.paw.exception.car.CarBrandNotFoundException;
import ar.edu.itba.paw.models.domain.car.Car;
import ar.edu.itba.paw.models.domain.car.CarBrand;
import ar.edu.itba.paw.models.domain.car.CarModel;
import ar.edu.itba.paw.models.dto.Page;
import ar.edu.itba.paw.services.car.CarBrandService;
import ar.edu.itba.paw.services.car.CarModelService;
import ar.edu.itba.paw.services.user.AdminService;
import ar.edu.itba.paw.webapp.api.common.PaginationLinks;
import ar.edu.itba.paw.webapp.api.common.VndMediaType;
import ar.edu.itba.paw.webapp.dto.rest.BrandDto;
import ar.edu.itba.paw.webapp.dto.rest.ModelDto;
import ar.edu.itba.paw.webapp.form.catalog.BrandCreateForm;
import ar.edu.itba.paw.webapp.dto.rest.CatalogApprovalDto;
import ar.edu.itba.paw.webapp.form.catalog.ModelCreateForm;
import ar.edu.itba.paw.webapp.support.CarRestEnums;
import ar.edu.itba.paw.webapp.support.CurrentUserResolver;
import ar.edu.itba.paw.webapp.security.auth.AuthenticationAuthorities;
import ar.edu.itba.paw.webapp.support.PaginationParams;
import ar.edu.itba.paw.webapp.support.PaginationSupport;
import ar.edu.itba.paw.webapp.util.RestUriUtils;

/**
 * Catalog brands ({@code /brands}, {@code /brands/{id}}, {@code /brands/{id}/models}).
 * Replaces admin catalog MVC flows for brand moderation (decision D6).
 */
@Path("/brands")
@Component
public class BrandController {

    private final CarBrandService carBrandService;
    private final CarModelService carModelService;
    private final AdminService adminService;
    private final CurrentUserResolver currentUserResolver;
    private final PaginationSupport paginationSupport;

    @Context
    private UriInfo uriInfo;

    @Autowired
    public BrandController(
            final CarBrandService carBrandService,
            final CarModelService carModelService,
            final AdminService adminService,
            final CurrentUserResolver currentUserResolver,
            final PaginationSupport paginationSupport) {
        this.carBrandService = carBrandService;
        this.carModelService = carModelService;
        this.adminService = adminService;
        this.currentUserResolver = currentUserResolver;
        this.paginationSupport = paginationSupport;
    }

    @GET
    @Produces(VndMediaType.BRAND_V1_JSON)
    public Response listBrands(
            @QueryParam("validated") final Boolean validated,
            @QueryParam("page") @javax.ws.rs.DefaultValue("1") final int page,
            @QueryParam("pageSize") final Integer pageSizeParam) {
        if (Boolean.FALSE.equals(validated)) {
            requireAdmin();
        }
        final PaginationParams paging = paginationSupport.forDefaultCollection(page, pageSizeParam);
        final Page<CarBrand> brandPage =
                carBrandService.findPage(validated, paging.getZeroBasedPage(), paging.getPageSize());
        final List<BrandDto> dtos = brandPage.getContent().stream()
                .map(brand -> BrandDto.from(brand, uriInfo))
                .collect(Collectors.toList());

        if (dtos.isEmpty()) {
            return Response.noContent().build();
        }

        final Response.ResponseBuilder builder = Response.ok(new GenericEntity<List<BrandDto>>(dtos) {})
                .header("X-Total-Count", brandPage.getTotalItems());
        PaginationLinks.add(
                builder, uriInfo, paging.getPage(), paging.getPageSize(), (int) brandPage.getTotalItems());
        return builder.build();
    }

    @POST
    @Consumes(VndMediaType.BRAND_V1_JSON)
    @Produces(VndMediaType.BRAND_V1_JSON)
    public Response createBrand(@Valid final BrandCreateForm form) {
        currentUserResolver.requireUserId();
        final CarBrand brand = carBrandService.findOrCreateUnvalidated(form.getName())
                .orElseThrow(IllegalStateException::new);

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
        final CarBrand brand = requireReadableBrand(id);
        return Response.ok(BrandDto.from(brand, uriInfo)).build();
    }

    @PATCH
    @Path("/{id}")
    @PreAuthorize("@userResourceAccess.isAdmin()")
    @Consumes(VndMediaType.BRAND_V1_JSON)
    @Produces(VndMediaType.BRAND_V1_JSON)
    public Response approveBrand(@PathParam("id") final long id, @Valid final CatalogApprovalDto patch) {
        carBrandService.findById(id)
                .orElseThrow(() -> new CarBrandNotFoundException(id));

        adminService.validateCarBrand(id);

        final CarBrand updated = carBrandService.findById(id)
                .orElseThrow(() -> new CarBrandNotFoundException(id));
        return Response.ok(BrandDto.from(updated, uriInfo)).build();
    }

    @DELETE
    @Path("/{id}")
    @PreAuthorize("@userResourceAccess.isAdmin()")
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

        final List<CarModel> models;
        if (validated == null || Boolean.TRUE.equals(validated)) {
            models = carModelService.findValidatedByBrandIdOrdered(id);
        } else {
            requireAdmin();
            models = carModelService.findByBrandIdOrdered(id);
        }
        final List<ModelDto> dtos = models.stream()
                .map(model -> ModelDto.from(model, uriInfo))
                .collect(Collectors.toList());
        if (dtos.isEmpty()) {
            return Response.noContent().build();
        }
        return Response.ok(new GenericEntity<List<ModelDto>>(dtos) {})
                .header("X-Total-Count", dtos.size())
                .build();
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
                .orElseThrow(IllegalStateException::new);

        final URI location = RestUriUtils.modelUri(uriInfo, id, model.getId());
        return Response.created(location).entity(ModelDto.from(model, uriInfo)).build();
    }

    private void requireAdmin() {
        if (!AuthenticationAuthorities.hasAdminRole(SecurityContextHolder.getContext().getAuthentication())) {
            throw new AccessDeniedException("Admin role required.");
        }
    }

    /**
     * Validated brands are public; pending ({@code validated = false}) brands are visible to admins only.
     * Non-admins receive the same 404 as a missing id so pending moderation rows are not enumerable.
     */
    private CarBrand requireReadableBrand(final long id) {
        final CarBrand brand = carBrandService.findById(id)
                .orElseThrow(() -> new CarBrandNotFoundException(id));
        if (!brand.isValidated() && !AuthenticationAuthorities.hasAdminRole(
                SecurityContextHolder.getContext().getAuthentication())) {
            throw new CarBrandNotFoundException(id);
        }
        return brand;
    }
}
