package ar.edu.itba.paw.webapp.controller.catalog;

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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

import ar.edu.itba.paw.webapp.api.common.VndMediaType;
import ar.edu.itba.paw.webapp.dto.rest.CatalogApprovalDto;
import ar.edu.itba.paw.webapp.support.ModelCatalogSupport;

/**
 * Canonical model item under its brand ({@code /brands/{id}/models/{modelId}}).
 */
@Path("/brands/{id}/models/{modelId}")
@Component
public class BrandModelItemController {

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
        return modelCatalogSupport.getModelForBrand(brandId, modelId, uriInfo);
    }

    @GET
    @Path("/price-insight")
    @Produces(VndMediaType.PRICE_MARKET_INSIGHT_V1_JSON)
    public Response priceInsight(
            @PathParam("id") final long brandId,
            @PathParam("modelId") final long modelId,
            @QueryParam("excludeCarId") final Long excludeCarId) {
        return modelCatalogSupport.priceInsightResponse(brandId, modelId, excludeCarId, uriInfo);
    }

    @PATCH
    @PreAuthorize("@userResourceAccess.isAdmin()")
    @Consumes(VndMediaType.MODEL_V1_JSON)
    @Produces(VndMediaType.MODEL_V1_JSON)
    public Response approveModel(
            @PathParam("id") final long brandId,
            @PathParam("modelId") final long modelId,
            @Valid final CatalogApprovalDto patch) {
        return modelCatalogSupport.approveModelIfRequested(
                brandId, modelId, Boolean.TRUE.equals(patch.getValidated()), uriInfo);
    }

    @DELETE
    @PreAuthorize("@userResourceAccess.isAdmin()")
    public Response rejectModel(
            @PathParam("id") final long brandId, @PathParam("modelId") final long modelId) {
        return modelCatalogSupport.rejectModelForBrand(brandId, modelId);
    }
}
