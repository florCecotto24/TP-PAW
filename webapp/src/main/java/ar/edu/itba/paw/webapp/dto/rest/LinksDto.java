package ar.edu.itba.paw.webapp.dto.rest;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Hypermedia {@code links} block ({@code openapi.yaml} {@code Links} schema).
 */
public final class LinksDto extends LinkedHashMap<String, String> {

    public static LinksDto ofSelf(final String selfUri) {
        final LinksDto links = new LinksDto();
        links.put("self", selfUri);
        return links;
    }

    public LinksDto withRelated(final String rel, final String uri) {
        if (uri != null) {
            put(rel, uri);
        }
        return this;
    }

    public Map<String, String> asMap() {
        return this;
    }
}
