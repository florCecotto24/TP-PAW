package ar.edu.itba.paw.webapp.support;

import java.util.function.Supplier;

import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;

/**
 * Conditional GET for vendor-JSON item representations ({@code GET /cars/{id}}, …).
 *
 * Uses a caller-supplied version token (typically derived from {@code updatedAt}) so clients
 * can revalidate with {@code If-None-Match} and receive {@code 304 Not Modified} without
 * re-serializing the DTO. {@link ar.edu.itba.paw.webapp.config.ApiCacheHeadersFilter} skips
 * responses that already carry an {@code ETag} header.
 */
public final class ConditionalJsonResponses {

    private ConditionalJsonResponses() {
    }

    /**
     * Returns {@code 200} with body or {@code 304} when {@code request}'s preconditions match
     * {@code etagValue}. The {@code entitySupplier} runs only when a body is required.
     */
    public static Response okOrNotModified(
            final Request request,
            final String etagValue,
            final String mediaType,
            final Supplier<?> entitySupplier) {
        final EntityTag entityTag = new EntityTag(etagValue);
        final Response.ResponseBuilder notModified = request.evaluatePreconditions(entityTag);
        final CacheControl cacheControl = noCacheMustRevalidate();
        if (notModified != null) {
            return notModified
                    .cacheControl(cacheControl)
                    .header(HttpHeaders.VARY, "Accept")
                    .build();
        }
        return Response.ok(entitySupplier.get())
                .type(mediaType)
                .tag(entityTag)
                .cacheControl(cacheControl)
                .header(HttpHeaders.VARY, "Accept")
                .build();
    }

    private static CacheControl noCacheMustRevalidate() {
        final CacheControl cacheControl = new CacheControl();
        cacheControl.setNoCache(true);
        cacheControl.setMustRevalidate(true);
        return cacheControl;
    }
}
