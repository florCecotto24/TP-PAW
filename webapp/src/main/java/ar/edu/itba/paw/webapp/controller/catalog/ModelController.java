package ar.edu.itba.paw.webapp.controller.catalog;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

import ar.edu.itba.paw.webapp.api.common.VndMediaType;
import ar.edu.itba.paw.webapp.support.ModelCatalogSupport;
import ar.edu.itba.paw.webapp.support.PaginationSupport;

/**
 * Admin pending-model collection only ({@code GET /models?validated=false}).
 * HTTP routing only; collection binding lives in {@link ModelCatalogSupport}.
 */
@Path("/models")
@Component
public class ModelController {

    private final ModelCatalogSupport modelCatalogSupport;
    private final PaginationSupport paginationSupport;

    @Context
    private UriInfo uriInfo;

    @Autowired
    public ModelController(
            final ModelCatalogSupport modelCatalogSupport,
            final PaginationSupport paginationSupport) {
        this.modelCatalogSupport = modelCatalogSupport;
        this.paginationSupport = paginationSupport;
    }

    @GET
    @PreAuthorize("@userResourceAccess.isAdmin()")
    @Produces(VndMediaType.MODEL_V1_JSON)
    public Response listPendingModels(
            @QueryParam("validated") final Boolean validated,
            @QueryParam("page") @DefaultValue("1") final int page,
            @QueryParam("pageSize") final Integer pageSize) {
        return modelCatalogSupport.listPendingModels(
                validated, paginationSupport.forDefaultCollection(page, pageSize), uriInfo);
    }
}
