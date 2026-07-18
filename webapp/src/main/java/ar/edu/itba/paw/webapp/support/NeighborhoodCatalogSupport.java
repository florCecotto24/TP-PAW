package ar.edu.itba.paw.webapp.support;

import java.util.List;
import java.util.stream.Collectors;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.springframework.stereotype.Component;

import ar.edu.itba.paw.models.domain.location.Neighborhood;
import ar.edu.itba.paw.services.location.LocationService;
import ar.edu.itba.paw.webapp.dto.rest.NeighborhoodDto;

/**
 * HTTP binding for location catalog ({@code GET /neighborhoods}, {@code GET /neighborhoods/{id}}).
 */
@Component
public final class NeighborhoodCatalogSupport {

    private final LocationService locationService;

    public NeighborhoodCatalogSupport(final LocationService locationService) {
        this.locationService = locationService;
    }

    public Response list(final UriInfo uriInfo) {
        final List<NeighborhoodDto> dtos = locationService.findAllNeighborhoods().stream()
                .map(neighborhood -> NeighborhoodDto.from(neighborhood, uriInfo))
                .collect(Collectors.toList());
        if (dtos.isEmpty()) {
            return Response.noContent().build();
        }
        return Response.ok(new GenericEntity<List<NeighborhoodDto>>(dtos) {})
                .header("X-Total-Count", dtos.size())
                .build();
    }

    public Response get(final long id, final UriInfo uriInfo) {
        final Neighborhood neighborhood = locationService.findNeighborhoodById(id)
                .orElseThrow(NotFoundException::new);
        return Response.ok(NeighborhoodDto.from(neighborhood, uriInfo)).build();
    }
}
