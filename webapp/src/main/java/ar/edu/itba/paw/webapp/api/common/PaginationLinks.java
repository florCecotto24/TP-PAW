package ar.edu.itba.paw.webapp.api.common;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

/**
 * RFC 5988 {@code Link} pagination headers ({@code rel=first|prev|next|last}).
 */
public final class PaginationLinks {

    private PaginationLinks() {
    }

    public static void add(
            final Response.ResponseBuilder builder,
            final UriInfo uriInfo,
            final int page,
            final int pageSize,
            final int totalItems) {
        if (totalItems <= 0) {
            return;
        }
        final int totalPages = Math.max(1, (int) Math.ceil((double) totalItems / pageSize));
        // Host-relative (path + query only), same policy as Link rel=authenticated-user: avoid baking
        // an absolute origin from Host / X-Forwarded-* that reverse proxies or Vite can get wrong.
        UriBuilder base = UriBuilder.fromPath(uriInfo.getRequestUri().getRawPath());
        final String rawQuery = uriInfo.getRequestUri().getRawQuery();
        if (rawQuery != null && !rawQuery.isBlank()) {
            base = base.replaceQuery(rawQuery);
        }
        base = base.replaceQueryParam("page", (Object[]) null)
                .replaceQueryParam("pageSize", (Object[]) null);

        builder.link(buildPageLink(base, 1, pageSize), "first");
        builder.link(buildPageLink(base, totalPages, pageSize), "last");

        if (page > 1) {
            builder.link(buildPageLink(base, page - 1, pageSize), "prev");
        }
        if (page < totalPages) {
            builder.link(buildPageLink(base, page + 1, pageSize), "next");
        }
    }

    private static String buildPageLink(final UriBuilder base, final int page, final int pageSize) {
        return base.clone()
                .queryParam("page", page)
                .queryParam("pageSize", pageSize)
                .build()
                .toString();
    }
}
