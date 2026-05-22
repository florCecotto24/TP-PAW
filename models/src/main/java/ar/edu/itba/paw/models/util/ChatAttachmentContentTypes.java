package ar.edu.itba.paw.models.util;

/**
 * Allowed MIME types for reservation chat attachments. Rejects unknown types even when the browser sends a generic
 * {@code application/octet-stream}.
 */
public final class ChatAttachmentContentTypes {

    private ChatAttachmentContentTypes() {
    }

    public static boolean isAllowed(final String contentType, final String fileName) {
        if (contentType == null || contentType.isBlank()) {
            return isAllowedByExtension(fileName);
        }
        final String t = contentType.trim().toLowerCase();
        if (t.startsWith("image/")) {
            return true;
        }
        if (t.startsWith("video/")) {
            return isAllowedVideoType(t);
        }
        return switch (t) {
            case "application/pdf",
                    "application/msword",
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                    "application/vnd.ms-excel",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    "application/vnd.ms-powerpoint",
                    "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                    "text/plain",
                    "application/zip",
                    "application/x-zip-compressed" -> true;
            default -> isAllowedByExtension(fileName);
        };
    }

    private static boolean isAllowedVideoType(final String contentType) {
        return "video/mp4".equals(contentType)
                || "video/webm".equals(contentType)
                || "video/quicktime".equals(contentType);
    }

    private static boolean isAllowedByExtension(final String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return false;
        }
        final String lower = fileName.trim().toLowerCase();
        final int dot = lower.lastIndexOf('.');
        if (dot < 0 || dot == lower.length() - 1) {
            return false;
        }
        final String ext = lower.substring(dot + 1);
        return switch (ext) {
            case "jpg", "jpeg", "png", "gif", "webp", "bmp", "svg" -> true;
            case "pdf" -> true;
            case "doc", "docx" -> true;
            case "mp4", "webm", "mov" -> true;
            case "txt", "zip" -> true;
            case "xls", "xlsx", "ppt", "pptx" -> true;
            default -> false;
        };
    }
}
