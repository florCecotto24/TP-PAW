package ar.edu.itba.paw.webapp.support;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;

import ar.edu.itba.paw.models.dto.file.BinaryContent;

/**
 * Shared conditional-GET support for publicly served binary media ({@code /image/{id}},
 * {@code /cars/{id}/pictures/{pictureId}} — images and gallery videos alike,
 * {@code /users/{id}/profile-picture}).
 *
 * <p>These URLs are stable but their content is editable (a user can replace their profile
 * picture, an owner can replace a car photo/video), so serving them with no cache policy at all
 * lets browsers/proxies apply heuristic caching that can keep showing stale bytes after an edit —
 * the URL never changes to bust it. Rather than an aggressive {@code max-age} (which has the same
 * staleness problem) this uses a content-hash {@link EntityTag} + {@code Cache-Control: no-cache}
 * (HTTP's confusingly-named "cache but always revalidate" directive): the client can keep a local
 * copy but must send {@code If-None-Match} first, so an edit is reflected on the very next request
 * while the byte-identical common case is a cheap 304 instead of a full re-download.</p>
 */
public final class CacheableBinaryResponses {

    private CacheableBinaryResponses() {
    }

    /**
     * Builds a 200 for a SENSITIVE binary (KYC documents, payment/refund receipts, chat attachments):
     * {@code Cache-Control: private, no-store} so no shared or local cache retains it, {@code nosniff}
     * so the browser cannot MIME-sniff it into an executable type, and {@code Content-Disposition:
     * attachment} so it downloads rather than rendering inline (defuses any stored-XSS via a crafted
     * upload). No ETag/conditional caching: these must never be cached.
     */
    public static Response sensitive(final BinaryContent content, final String downloadFileName) {
        final CacheControl noStore = new CacheControl();
        noStore.setPrivate(true);
        noStore.setNoStore(true);
        noStore.setNoCache(true);
        final String safeName = sanitizeFileName(downloadFileName);
        return Response.ok(content.getBytes())
                .type(content.getContentType())
                .cacheControl(noStore)
                .header("X-Content-Type-Options", "nosniff")
                .header("Content-Disposition", "attachment; filename=\"" + safeName + "\"")
                .build();
    }

    private static String sanitizeFileName(final String name) {
        if (name == null || name.isBlank()) {
            return "download";
        }
        // Strip anything that could break out of the quoted filename or inject header/path characters.
        final String cleaned = name.replaceAll("[\\r\\n\"\\\\/]", "_").trim();
        return cleaned.isEmpty() ? "download" : cleaned;
    }

    /** Builds a conditional 200 (or 304 if {@code request}'s {@code If-None-Match} still matches). */
    public static Response of(final Request request, final BinaryContent content) {
        final EntityTag etag = new EntityTag(sha256Hex(content.getBytes()));
        final Response.ResponseBuilder notModified = request.evaluatePreconditions(etag);
        if (notModified != null) {
            return notModified.cacheControl(noCacheMustRevalidate()).build();
        }
        return Response.ok(content.getBytes())
                .type(content.getContentType())
                .tag(etag)
                .cacheControl(noCacheMustRevalidate())
                .build();
    }

    private static CacheControl noCacheMustRevalidate() {
        final CacheControl cacheControl = new CacheControl();
        cacheControl.setNoCache(true);
        cacheControl.setMustRevalidate(true);
        return cacheControl;
    }

    private static String sha256Hex(final byte[] bytes) {
        try {
            final MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(bytes));
        } catch (final NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
