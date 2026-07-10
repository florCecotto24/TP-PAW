package ar.edu.itba.paw.webapp.controller.catalog;

import java.util.Locale;

import javax.annotation.security.RolesAllowed;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PATCH;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import ar.edu.itba.paw.models.domain.car.CarModel;
import ar.edu.itba.paw.webapp.api.common.VndMediaType;
import ar.edu.itba.paw.webapp.dto.rest.CatalogApprovalDto;
import ar.edu.itba.paw.webapp.support.ModelCatalogSupport;

/**
 * Canonical model item under its brand ({@code /brands/{id}/models/{modelId}}).
 */
@Path("/brands/{id}/models/{modelId}")
@Component
public final class BrandModelItemController {

    private final ModelCatalogSupport modelCatalogSupport;

    @Context
    private UriInfo uriInfo;

    @Autowired
    public BrandModelItemController(final ModelCatalogSupport modelCatalogSupport) {
        this.modelCatalogSupport = modelCatalogSupport;
    }

    @GET
    @Produces(VndMediaType.MODEL_V1_JSON)
    public Response getModel(
            @PathParam("id") final long brandId, @PathParam("modelId") final long modelId) {
        final CarModel model = modelCatalogSupport.requireModelForBrand(brandId, modelId);
        return Response.ok(modelCatalogSupport.toDto(model, uriInfo)).build();
    }

    @GET
    @Path("/price-insight")
    @Produces(VndMediaType.PRICE_MARKET_INSIGHT_V1_JSON)
    public Response priceInsight(
            @PathParam("id") final long brandId,
            @PathParam("modelId") final long modelId,
            @QueryParam("excludeCarId") final Long excludeCarId) {
        modelCatalogSupport.requireModelForBrand(brandId, modelId);
        return modelCatalogSupport.priceInsightResponse(modelId, excludeCarId);
    }

    @PATCH
    @RolesAllowed("ROLE_ADMIN")
    @Consumes(VndMediaType.MODEL_V1_JSON)
    @Produces(VndMediaType.MODEL_V1_JSON)
    public Response approveModel(
            @PathParam("id") final long brandId,
            @PathParam("modelId") final long modelId,
            @Valid final CatalogApprovalDto patch) {
        modelCatalogSupport.requireModelForBrand(brandId, modelId);
        final Locale locale = LocaleContextHolder.getLocale();
        modelCatalogSupport.approveModel(modelId, locale);
        final CarModel updated = modelCatalogSupport.requireModelForBrand(brandId, modelId);
        return Response.ok(modelCatalogSupport.toDto(updated, uriInfo)).build();
    }

    @DELETE
    @RolesAllowed("ROLE_ADMIN")
    public Response rejectModel(
            @PathParam("id") final long brandId, @PathParam("modelId") final long modelId) {
        modelCatalogSupport.requireModelForBrand(brandId, modelId);
        final Locale locale = LocaleContextHolder.getLocale();
        modelCatalogSupport.rejectModel(modelId, locale);
        return Response.noContent().build();
    }
}
