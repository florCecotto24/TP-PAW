package ar.edu.itba.paw.models.util.format;

/** Shared UI/mail truncation limits (avoid magic numbers across services and webapp). */
public final class TextTruncationLimits {

    public static final int RESERVATION_ATTACHMENT_FILENAME = 200;
    public static final int CONTENT_TYPE_HEADER_MAX = 100;
    public static final int DOWNLOAD_FILENAME = 120;
    public static final int PROFILE_LOCALE_TAG = 32;

    private TextTruncationLimits() {
    }
}
