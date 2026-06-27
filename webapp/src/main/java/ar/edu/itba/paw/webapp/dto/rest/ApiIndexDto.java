package ar.edu.itba.paw.webapp.dto.rest;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.ws.rs.core.UriInfo;

public final class ApiIndexDto {

    private Map<String, String> resources = new LinkedHashMap<>();

    public ApiIndexDto() {
    }

    public static ApiIndexDto from(final UriInfo uriInfo) {
        final ApiIndexDto dto = new ApiIndexDto();
        final var base = uriInfo.getBaseUriBuilder();
        dto.resources.put("users", base.clone().path("users").build().toString());
        dto.resources.put("cars", base.clone().path("cars").build().toString());
        dto.resources.put("reservations", base.clone().path("reservations").build().toString());
        dto.resources.put("brands", base.clone().path("brands").build().toString());
        dto.resources.put("neighborhoods", base.clone().path("neighborhoods").build().toString());
        return dto;
    }

    public Map<String, String> getResources() {
        return resources;
    }

    public void setResources(final Map<String, String> resources) {
        this.resources = resources;
    }
}
