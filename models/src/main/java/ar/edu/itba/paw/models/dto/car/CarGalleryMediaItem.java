package ar.edu.itba.paw.models.dto.car;

import java.util.Objects;

/** One entry in the public car detail gallery (image or video). */
public final class CarGalleryMediaItem {

    public enum MediaKind {
        IMAGE,
        VIDEO
    }

    private final MediaKind mediaKind;
    private final String url;
    private final String contentType;

    public CarGalleryMediaItem(final MediaKind mediaKind, final String url, final String contentType) {
        this.mediaKind = Objects.requireNonNull(mediaKind, "mediaKind");
        this.url = Objects.requireNonNull(url, "url");
        this.contentType = contentType != null ? contentType : "";
    }

    public MediaKind getMediaKind() {
        return mediaKind;
    }

    public String getUrl() {
        return url;
    }

    public String getContentType() {
        return contentType;
    }

    public boolean isVideo() {
        return mediaKind == MediaKind.VIDEO;
    }
}
