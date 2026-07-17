package ar.edu.itba.paw.webapp.support;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
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
import ar.edu.itba.paw.webapp.dto.rest.BrandDto;
import ar.edu.itba.paw.webapp.dto.rest.CatalogApprovalDto;
import ar.edu.itba.paw.webapp.dto.rest.ModelDto;
import ar.edu.itba.paw.webapp.form.catalog.BrandCreateForm;
import ar.edu.itba.paw.webapp.form.catalog.ModelCreateForm;
import ar.edu.itba.paw.webapp.security.auth.AuthenticationAuthorities;
import ar.edu.itba.paw.webapp.util.RestUriUtils;

/**
 * HTTP orchestration for brand catalog collections and nested models under {@code /brands}.
 */
@Component
public final class BrandCatalogSupport {

    private final CarBrandService carBrandService;
    private final CarModelService carModelService;
    private final AdminService adminService;

    public BrandCatalogSupport(
            final CarBrandService carBrandService,
            final CarModelService carModelService,
            final AdminService adminService) {
        this.carBrandService = carBrandService;
        this.carModelService = carModelService;
        this.adminService = adminService;
    }

    public Response listBrands(
            final Boolean validated,
            final PaginationParams paging,
            final UriInfo uriInfo) {
        if (Boolean.FALSE.equals(validated)) {
            requireAdmin();
        }
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

    public Response createBrand(final BrandCreateForm form, final UriInfo uriInfo) {
        final CarBrand brand = carBrandService.createUnvalidated(form.getName())
                .orElseThrow(() -> new IllegalStateException("Brand name required"));
        final URI location = uriInfo.getBaseUriBuilder()
                .path("brands")
                .path(String.valueOf(brand.getId()))
                .build();
        return Response.created(location).entity(BrandDto.from(brand, uriInfo)).build();
    }

    public Response getBrand(final long id, final UriInfo uriInfo) {
        return Response.ok(BrandDto.from(requireReadableBrand(id), uriInfo)).build();
    }

    public Response approveBrand(final long id, final CatalogApprovalDto patch, final UriInfo uriInfo) {
        carBrandService.findById(id)
                .orElseThrow(() -> new CarBrandNotFoundException(id));
        if (Boolean.TRUE.equals(patch.getValidated())) {
            adminService.validateCarBrand(id);
        }
        final CarBrand updated = carBrandService.findById(id)
                .orElseThrow(() -> new CarBrandNotFoundException(id));
        return Response.ok(BrandDto.from(updated, uriInfo)).build();
    }

    public Response rejectBrand(final long id) {
        if (carBrandService.findById(id).isEmpty()) {
            throw new CarBrandNotFoundException(id);
        }
        adminService.rejectCarBrand(id);
        return Response.noContent().build();
    }

    public Response listModelsForBrand(
            final long brandId,
            final Boolean validated,
            final UriInfo uriInfo) {
        carBrandService.findById(brandId)
                .orElseThrow(() -> new CarBrandNotFoundException(brandId));
        final List<CarModel> models;
        if (validated == null || Boolean.TRUE.equals(validated)) {
            models = carModelService.findValidatedByBrandIdOrdered(brandId);
        } else {
            requireAdmin();
            models = carModelService.findByBrandIdOrdered(brandId);
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

    public Response createModelForBrand(
            final long brandId,
            final ModelCreateForm form,
            final UriInfo uriInfo) {
        carBrandService.findById(brandId)
                .orElseThrow(() -> new CarBrandNotFoundException(brandId));
        final Car.Type type = CarRestEnums.parseType(form.getType());
        final CarModel model = carModelService.createUnvalidated(brandId, form.getName(), type)
                .orElseThrow(() -> new IllegalStateException("Model name and type required"));
        final URI location = RestUriUtils.modelUri(uriInfo, brandId, model.getId());
        return Response.created(location).entity(ModelDto.from(model, uriInfo)).build();
    }

    /**
     * Admin gate when a collection query opts into pending rows ({@code validated=false}).
     */
    private void requireAdmin() {
        if (!AuthenticationAuthorities.hasAdminRole(SecurityContextHolder.getContext().getAuthentication())) {
            throw new AccessDeniedException("Admin role required.");
        }
    }

    /**
     * Validated brands are public; pending brands are admin-only (non-admins get the same 404).
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
