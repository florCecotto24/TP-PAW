package ar.edu.itba.paw.webapp.controller.catalog;

import java.util.List;
import java.util.stream.Collectors;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ar.edu.itba.paw.models.domain.location.Neighborhood;
import ar.edu.itba.paw.services.location.LocationService;
import ar.edu.itba.paw.webapp.api.common.VndMediaType;
import ar.edu.itba.paw.webapp.dto.rest.NeighborhoodDto;

/** Location catalog ({@code GET /neighborhoods}). */
@Path("/neighborhoods")
@Component
public final class NeighborhoodController {

    private final LocationService locationService;

    @Context
    private UriInfo uriInfo;

    @Autowired
    public NeighborhoodController(final LocationService locationService) {
        this.locationService = locationService;
    }

    @GET
    @Produces(VndMediaType.NEIGHBORHOOD_V1_JSON)
    public Response listNeighborhoods() {
        final List<NeighborhoodDto> dtos = locationService.findAllNeighborhoods().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
        if (dtos.isEmpty()) {
            return Response.noContent().build();
        }
        return Response.ok(new GenericEntity<List<NeighborhoodDto>>(dtos) {}).build();
    }

    private NeighborhoodDto toDto(final Neighborhood neighborhood) {
        return NeighborhoodDto.from(neighborhood, uriInfo);
    }
}
