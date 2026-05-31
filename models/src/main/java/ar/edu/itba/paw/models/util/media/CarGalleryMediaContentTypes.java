package ar.edu.itba.paw.models.util.media;

import ar.edu.itba.paw.models.domain.Image;

/** Allowed MIME types for car gallery uploads (photos and short videos). */
public final class CarGalleryMediaContentTypes {

    private CarGalleryMediaContentTypes() {
    }

    public static boolean isAllowed(final String contentType, final String fileName) {
        return isImageContentType(contentType) || isVideoContentType(contentType, fileName);
    }

    public static boolean isImageContentType(final String contentType) {
        return Image.isImageContentType(contentType);
    }

    public static boolean isVideoContentType(final String contentType, final String fileName) {
        if (contentType != null && !contentType.isBlank()) {
            final String t = contentType.trim().toLowerCase();
            if (t.startsWith("video/")) {
                return isAllowedVideoType(t);
            }
        }
        return isAllowedVideoExtension(fileName);
    }

    private static boolean isAllowedVideoType(final String contentType) {
        return "video/mp4".equals(contentType)
                || "video/webm".equals(contentType)
                || "video/quicktime".equals(contentType);
    }

    private static boolean isAllowedVideoExtension(final String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return false;
        }
        final String lower = fileName.trim().toLowerCase();
        final int dot = lower.lastIndexOf('.');
        if (dot < 0 || dot == lower.length() - 1) {
            return false;
        }
        return switch (lower.substring(dot + 1)) {
            case "mp4", "webm", "mov" -> true;
            default -> false;
        };
    }
}
