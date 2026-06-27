package ar.edu.itba.paw.webapp.dto.rest;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.ws.rs.core.UriInfo;

import ar.edu.itba.paw.models.domain.car.CarBrand;
import ar.edu.itba.paw.webapp.util.RestUriUtils;

public final class BrandDto {

    private String name;
    private boolean validated;
    private LinksDto links;

    public BrandDto() {
    }

    public static BrandDto from(final CarBrand brand, final UriInfo uriInfo) {
        final BrandDto dto = new BrandDto();
        dto.name = brand.getName();
        dto.validated = brand.isValidated();
        dto.links = LinksDto.ofSelf(RestUriUtils.brandUri(uriInfo, brand.getId()).toString())
                .withRelated("models", RestUriUtils.brandModelsUri(uriInfo, brand.getId()).toString());
        return dto;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public boolean isValidated() {
        return validated;
    }

    public void setValidated(final boolean validated) {
        this.validated = validated;
    }

    public LinksDto getLinks() {
        return links;
    }

    public void setLinks(final LinksDto links) {
        this.links = links;
    }
}
