package ar.edu.itba.paw.webapp.dto.rest;

import java.util.Locale;

import javax.ws.rs.core.UriInfo;

import ar.edu.itba.paw.models.domain.car.Car;
import ar.edu.itba.paw.models.domain.car.CarModel;
import ar.edu.itba.paw.webapp.util.RestUriUtils;

public final class ModelDto {

    private String name;
    private String type;
    private String brandName;
    private boolean validated;
    private LinksDto links;

    public ModelDto() {
    }

    public static ModelDto from(final CarModel model, final UriInfo uriInfo) {
        final ModelDto dto = new ModelDto();
        dto.name = model.getName();
        dto.type = model.getType().name().toLowerCase(Locale.ROOT);
        dto.brandName = model.getBrand().getName();
        dto.validated = model.isValidated();
        dto.links = LinksDto.ofSelf(
                        RestUriUtils.modelUri(uriInfo, model.getBrandId(), model.getId()).toString())
                .withRelated("brand", RestUriUtils.brandUri(uriInfo, model.getBrandId()).toString());
        return dto;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(final String type) {
        this.type = type;
    }

    public String getBrandName() {
        return brandName;
    }

    public void setBrandName(final String brandName) {
        this.brandName = brandName;
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

    public static Car.Type parseTypeOrNull(final String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return Car.Type.valueOf(raw.trim().toUpperCase(Locale.ROOT));
    }
}
