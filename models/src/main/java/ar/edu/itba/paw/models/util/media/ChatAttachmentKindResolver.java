package ar.edu.itba.paw.models.util.media;

import java.util.Locale;

import ar.edu.itba.paw.models.util.media.ChatAttachmentKind;

/** Maps stored file metadata to a chat UI display category. */
public final class ChatAttachmentKindResolver {

    private ChatAttachmentKindResolver() {
    }

    public static ChatAttachmentKind resolve(final String contentType, final String fileName) {
        if (contentType != null && !contentType.isBlank()) {
            final String t = contentType.trim().toLowerCase(Locale.ROOT);
            if (t.startsWith("image/")) {
                return ChatAttachmentKind.IMAGE;
            }
            if ("application/pdf".equals(t)) {
                return ChatAttachmentKind.PDF;
            }
            if (t.startsWith("video/")) {
                return ChatAttachmentKind.VIDEO;
            }
            if ("application/msword".equals(t)
                    || "application/vnd.openxmlformats-officedocument.wordprocessingml.document".equals(t)) {
                return ChatAttachmentKind.DOCUMENT;
            }
        }
        if (fileName != null && !fileName.isBlank()) {
            final String lower = fileName.trim().toLowerCase(Locale.ROOT);
            if (lower.endsWith(".pdf")) {
                return ChatAttachmentKind.PDF;
            }
            if (lower.endsWith(".doc") || lower.endsWith(".docx")) {
                return ChatAttachmentKind.DOCUMENT;
            }
            if (lower.endsWith(".mp4") || lower.endsWith(".webm") || lower.endsWith(".mov")) {
                return ChatAttachmentKind.VIDEO;
            }
            final int dot = lower.lastIndexOf('.');
            if (dot > 0) {
                final String ext = lower.substring(dot + 1);
                if (switch (ext) {
                    case "jpg", "jpeg", "png", "gif", "webp", "bmp" -> true;
                    default -> false;
                }) {
                    return ChatAttachmentKind.IMAGE;
                }
            }
        }
        return ChatAttachmentKind.GENERIC;
    }
}
