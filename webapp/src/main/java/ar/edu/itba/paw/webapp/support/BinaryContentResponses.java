package ar.edu.itba.paw.webapp.support;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import ar.edu.itba.paw.models.dto.file.BinaryContent;
import ar.edu.itba.paw.webapp.util.DownloadFileNameSanitizer;

/** Builds Jersey {@link Response} bodies for inline binary downloads. */
public final class BinaryContentResponses {

    private BinaryContentResponses() {
    }

    public static Response inline(final BinaryContent content, final String fallbackFileName) {
        final String safeName = DownloadFileNameSanitizer.sanitize(
                content.getFileName(),
                fallbackFileNameWithExtension(fallbackFileName, content.getContentType()));
        final String contentType = content.getContentType();
        final Response.ResponseBuilder builder = Response.ok(content.getBytes())
                .header("Content-Disposition", "inline; filename=\"" + safeName + "\"");
        if (contentType != null && !contentType.isBlank()) {
            builder.type(contentType);
        } else {
            builder.type(MediaType.APPLICATION_OCTET_STREAM);
        }
        return builder.build();
    }

    public static String fallbackFileName(final String baseName, final String contentType) {
        return fallbackFileNameWithExtension(baseName, contentType);
    }

    private static String fallbackFileNameWithExtension(final String baseName, final String contentType) {
        if (baseName == null || baseName.isBlank()) {
            return "document";
        }
        if (baseName.contains(".")) {
            return baseName;
        }
        if (contentType == null || contentType.isBlank()) {
            return baseName;
        }
        final String lower = contentType.toLowerCase(java.util.Locale.ROOT);
        if (lower.contains("pdf")) {
            return baseName + ".pdf";
        }
        if (lower.startsWith("image/")) {
            final String sub = lower.substring("image/".length()).split(";")[0].trim();
            if ("jpeg".equals(sub)) {
                return baseName + ".jpg";
            }
            if (!sub.isBlank()) {
                return baseName + "." + sub;
            }
        }
        return baseName;
    }
}
