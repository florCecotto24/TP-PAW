package ar.edu.itba.paw.webapp.controller.catalog;

import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.PATCH;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

import ar.edu.itba.paw.webapp.api.common.VndMediaType;
import ar.edu.itba.paw.webapp.dto.rest.CatalogApprovalDto;
import ar.edu.itba.paw.webapp.form.catalog.BrandCreateForm;
import ar.edu.itba.paw.webapp.form.catalog.ModelCreateForm;
import ar.edu.itba.paw.webapp.support.BrandCatalogSupport;
import ar.edu.itba.paw.webapp.support.CurrentUserResolver;
import ar.edu.itba.paw.webapp.support.PaginationSupport;

/**
 * Catalog brands ({@code /brands}, {@code /brands/{id}}, {@code /brands/{id}/models}).
 * HTTP routing only; catalog ACL / DTO binding lives in {@link BrandCatalogSupport}.
 */
@Path("/brands")
@Component
public class BrandController {

    private final BrandCatalogSupport brandCatalogSupport;
    private final CurrentUserResolver currentUserResolver;
    private final PaginationSupport paginationSupport;

    @Context
    private UriInfo uriInfo;

    @Autowired
    public BrandController(
            final BrandCatalogSupport brandCatalogSupport,
            final CurrentUserResolver currentUserResolver,
            final PaginationSupport paginationSupport) {
        this.brandCatalogSupport = brandCatalogSupport;
        this.currentUserResolver = currentUserResolver;
        this.paginationSupport = paginationSupport;
    }

    // Not a @PreAuthorize candidate: a single method-level precondition cannot express that two-way
    // routing, so the admin gate stays imperative in BrandCatalogSupport when validated=false.
    @GET
    @Produces(VndMediaType.BRAND_V1_JSON)
    public Response listBrands(
            @QueryParam("validated") final Boolean validated,
            @QueryParam("page") @DefaultValue("1") final int page,
            @QueryParam("pageSize") final Integer pageSizeParam) {
        return brandCatalogSupport.listBrands(
                validated, paginationSupport.forDefaultCollection(page, pageSizeParam), uriInfo);
    }

    @POST
    @Consumes(VndMediaType.BRAND_V1_JSON)
    @Produces(VndMediaType.BRAND_V1_JSON)
    @PreAuthorize("isAuthenticated()")
    public Response createBrand(@Valid final BrandCreateForm form) {
        return brandCatalogSupport.createBrand(form, uriInfo);
    }

    @GET
    @Path("/{id}")
    @Produces(VndMediaType.BRAND_V1_JSON)
    public Response getBrand(@PathParam("id") final long id) {
        return brandCatalogSupport.getBrand(id, uriInfo);
    }

    @PATCH
    @Path("/{id}")
    @PreAuthorize("@userResourceAccess.isAdmin()")
    @Consumes(VndMediaType.BRAND_V1_JSON)
    @Produces(VndMediaType.BRAND_V1_JSON)
    public Response approveBrand(@PathParam("id") final long id, @Valid final CatalogApprovalDto patch) {
        return brandCatalogSupport.approveBrand(id, patch, uriInfo);
    }

    @DELETE
    @Path("/{id}")
    @PreAuthorize("@userResourceAccess.isAdmin()")
    public Response rejectBrand(@PathParam("id") final long id) {
        return brandCatalogSupport.rejectBrand(id);
    }

    // Same query-param ACL split as listBrands: validated models are public; validated=false is
    // admin-only and cannot be expressed as a fixed @PreAuthorize on the path alone.
    @GET
    @Path("/{id}/models")
    @Produces(VndMediaType.MODEL_V1_JSON)
    public Response listModelsForBrand(
            @PathParam("id") final long id,
            @QueryParam("validated") final Boolean validated) {
        return brandCatalogSupport.listModelsForBrand(id, validated, uriInfo);
    }

    @POST
    @Path("/{id}/models")
    @Consumes(VndMediaType.MODEL_V1_JSON)
    @Produces(VndMediaType.MODEL_V1_JSON)
    @PreAuthorize("isAuthenticated()")
    public Response createModelForBrand(
            @PathParam("id") final long id,
            @Valid final ModelCreateForm form) {
        return brandCatalogSupport.createModelForBrand(id, form, uriInfo);
    }
}
