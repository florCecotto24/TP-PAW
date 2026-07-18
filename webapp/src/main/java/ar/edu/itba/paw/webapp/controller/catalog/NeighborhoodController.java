package ar.edu.itba.paw.webapp.controller.catalog;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ar.edu.itba.paw.webapp.api.common.VndMediaType;
import ar.edu.itba.paw.webapp.support.NeighborhoodCatalogSupport;

/**
 * Location catalog ({@code GET /neighborhoods}, {@code GET /neighborhoods/{id}}).
 * HTTP routing only.
 */
@Path("/neighborhoods")
@Component
public final class NeighborhoodController {

    private final NeighborhoodCatalogSupport neighborhoodCatalogSupport;

    @Context
    private UriInfo uriInfo;

    @Autowired
    public NeighborhoodController(final NeighborhoodCatalogSupport neighborhoodCatalogSupport) {
        this.neighborhoodCatalogSupport = neighborhoodCatalogSupport;
    }

    @GET
    @Produces(VndMediaType.NEIGHBORHOOD_V1_JSON)
    public Response listNeighborhoods() {
        return neighborhoodCatalogSupport.list(uriInfo);
    }

    @GET
    @Path("/{id}")
    @Produces(VndMediaType.NEIGHBORHOOD_V1_JSON)
    public Response getNeighborhood(@PathParam("id") final long id) {
        return neighborhoodCatalogSupport.get(id, uriInfo);
    }
}
