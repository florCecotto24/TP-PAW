package ar.edu.itba.paw.webapp.support;

import java.util.List;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import org.springframework.stereotype.Component;

import ar.edu.itba.paw.webapp.api.common.VndMediaType;

/**
 * Vendor MIME negotiation for car collections and item teasers (summary vs full detail).
 */
@Component
public final class CarRepresentationSupport {

    public boolean acceptsFullCar(final HttpHeaders headers) {
        if (headers == null) {
            return false;
        }
        final List<MediaType> acceptable = headers.getAcceptableMediaTypes();
        if (acceptable == null || acceptable.isEmpty()) {
            return false;
        }
        final MediaType full = MediaType.valueOf(VndMediaType.CAR_V1_JSON);
        final MediaType summary = MediaType.valueOf(VndMediaType.CAR_SUMMARY_V1_JSON);
        boolean wantsFull = false;
        boolean wantsSummary = false;
        for (final MediaType candidate : acceptable) {
            if (candidate.isCompatible(full)) {
                wantsFull = true;
            }
            if (candidate.isCompatible(summary)) {
                wantsSummary = true;
            }
        }
        return wantsFull && !wantsSummary;
    }

    /**
     * Item {@code GET /cars/{id}}: prefer the teaser only when {@code Accept} explicitly names
     * {@code car.summary}. Wildcards and bare {@code car.v1} keep the full representation.
     */
    public boolean acceptsCarSummary(final HttpHeaders headers) {
        if (headers == null) {
            return false;
        }
        final List<MediaType> acceptable = headers.getAcceptableMediaTypes();
        if (acceptable == null || acceptable.isEmpty()) {
            return false;
        }
        final MediaType summary = MediaType.valueOf(VndMediaType.CAR_SUMMARY_V1_JSON);
        for (final MediaType candidate : acceptable) {
            if (isWildcard(candidate)) {
                continue;
            }
            if (candidate.isCompatible(summary)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isWildcard(final MediaType mediaType) {
        return MediaType.MEDIA_TYPE_WILDCARD.equals(mediaType.getType())
                || MediaType.MEDIA_TYPE_WILDCARD.equals(mediaType.getSubtype());
    }
}
