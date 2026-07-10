package ar.edu.itba.paw.webapp.dto.rest;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.UriInfo;

/**
 * HATEOAS API entry point ({@code GET /api/}). Exposes a plain {@code links} map for navigation
 * and a richer {@code resources} map ({@code href} + supported collection {@code queryParams}).
 */
public final class ApiIndexDto {

    private Map<String, String> links = new LinkedHashMap<>();
    private Map<String, ResourceDescriptor> resources = new LinkedHashMap<>();

    public ApiIndexDto() {
    }

    public static ApiIndexDto from(final UriInfo uriInfo) {
        final ApiIndexDto dto = new ApiIndexDto();
        final var base = uriInfo.getBaseUriBuilder();

        dto.links.put("self", base.build().toString());
        dto.links.put("users", href(uriInfo, "users"));
        dto.links.put("cars", href(uriInfo, "cars"));
        dto.links.put("reservations", href(uriInfo, "reservations"));
        dto.links.put("reviews", href(uriInfo, "reviews"));
        dto.links.put("brands", href(uriInfo, "brands"));
        dto.links.put("models", href(uriInfo, "models"));
        dto.links.put("neighborhoods", href(uriInfo, "neighborhoods"));
        dto.links.put("credentials", href(uriInfo, "credentials"));
        dto.links.put("image", href(uriInfo, "image"));

        dto.resources.put("users", new ResourceDescriptor(href(uriInfo, "users"),
                List.of("page", "pageSize", "blocked", "role", "q")));
        dto.resources.put("cars", new ResourceDescriptor(href(uriInfo, "cars"),
                List.of("page", "pageSize", "q", "ownerId", "category", "transmission", "powertrain",
                        "priceMin", "priceMax", "rating", "neighborhoodId", "from", "until", "flexible",
                        "flexMonth", "flexDays", "status", "sort")));
        dto.resources.put("reservations", new ResourceDescriptor(href(uriInfo, "reservations"),
                List.of("page", "pageSize", "riderId", "ownerId", "carId", "status", "riderStatus", "q",
                        "category", "transmission", "powertrain", "priceMin", "priceMax", "rating", "sort")));
        dto.resources.put("reviews", new ResourceDescriptor(href(uriInfo, "reviews"),
                List.of("carId", "recipientUserId", "reservationId", "page", "pageSize")));
        dto.resources.put("brands", new ResourceDescriptor(href(uriInfo, "brands"),
                List.of("validated", "page", "pageSize")));
        dto.resources.put("models", new ResourceDescriptor(href(uriInfo, "models"),
                List.of("validated")));
        dto.resources.put("neighborhoods", new ResourceDescriptor(href(uriInfo, "neighborhoods"), List.of()));
        dto.resources.put("credentials", new ResourceDescriptor(href(uriInfo, "credentials"), List.of()));
        dto.resources.put("image", new ResourceDescriptor(href(uriInfo, "image"), List.of()));

        return dto;
    }

    private static String href(final UriInfo uriInfo, final String collection) {
        return uriInfo.getBaseUriBuilder().path(collection).build().toString();
    }

    public Map<String, String> getLinks() {
        return links;
    }

    public void setLinks(final Map<String, String> links) {
        this.links = links;
    }

    public Map<String, ResourceDescriptor> getResources() {
        return resources;
    }

    public void setResources(final Map<String, ResourceDescriptor> resources) {
        this.resources = resources;
    }

    /** One top-level collection: absolute {@code href} and its supported GET query params. */
    public static final class ResourceDescriptor {

        private String href;
        private List<String> queryParams;

        public ResourceDescriptor() {
        }

        public ResourceDescriptor(final String href, final List<String> queryParams) {
            this.href = href;
            this.queryParams = queryParams;
        }

        public String getHref() {
            return href;
        }

        public void setHref(final String href) {
            this.href = href;
        }

        public List<String> getQueryParams() {
            return queryParams;
        }

        public void setQueryParams(final List<String> queryParams) {
            this.queryParams = queryParams;
        }
    }
}
