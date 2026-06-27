package ar.edu.itba.paw.webapp.dto.rest;

import javax.ws.rs.core.UriInfo;

import ar.edu.itba.paw.models.domain.location.Neighborhood;
import ar.edu.itba.paw.webapp.util.RestUriUtils;

public final class NeighborhoodDto {

    private String name;
    private LinksDto links;

    public NeighborhoodDto() {
    }

    public static NeighborhoodDto from(final Neighborhood neighborhood, final UriInfo uriInfo) {
        final NeighborhoodDto dto = new NeighborhoodDto();
        dto.name = neighborhood.getName();
        dto.links = LinksDto.ofSelf(RestUriUtils.neighborhoodUri(uriInfo, neighborhood.getId()).toString());
        return dto;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public LinksDto getLinks() {
        return links;
    }

    public void setLinks(final LinksDto links) {
        this.links = links;
    }
}
