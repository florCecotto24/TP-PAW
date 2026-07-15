package ar.edu.itba.paw.models.domain.file;

import java.util.Arrays;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import ar.edu.itba.paw.models.domain.internal.EntityEquality;

/** Stored image metadata and inline bytes (legacy path; profile and uploads may use {@link StoredFile} instead). */
@Entity
@Table(name = "images")
public class Image {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "images_id_seq")
    @SequenceGenerator(name = "images_id_seq", sequenceName = "images_id_seq", allocationSize = 1)
    private long id;

    @Column(name = "image_name", nullable = false, length = 255)
    private String name;

    @Column(name = "content_type", nullable = false, length = 50)
    private String contentType;

    /**
     * Marked LAZY so navigations that only need {@code id} / {@code name} / {@code contentType}
     * (e.g. {@code user.getProfilePicture().getContentType()} or gallery card metadata) do not pull
     * the full byte array. The hint only fires when Hibernate bytecode enhancement is enabled at
     * build time; without enhancement Hibernate ignores the {@code LAZY} flag and the column is
     * still loaded eagerly. The annotation stays so it takes effect as soon as enhancement is on.
     */
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "byte_array", nullable = false)
    private byte[] data;

    /* package */ Image() {
        // For Hibernate
    }

    /**
     * Explicit allowlist of safe raster image MIME types (case-insensitive). Anything else is rejected,
     * notably {@code image/svg+xml}: SVG is an XML document that can carry {@code <script>}/event handlers,
     * so serving it inline would enable stored XSS. Accepting a blanket {@code image/*} would let SVG (and
     * any other future vector/markup image type) through, so we enumerate the safe raster formats instead.
     */
    public static boolean isImageContentType(final String contentType) {
        if (contentType == null) {
            return false;
        }
        final String t = contentType.trim().toLowerCase(java.util.Locale.ROOT);
        if (t.isEmpty()) {
            return false;
        }
        return switch (t) {
            case "image/jpeg", "image/png", "image/gif", "image/webp" -> true;
            default -> false;
        };
    }

    /* package */ Image(final long id, final String name, final String contentType, final byte[] data) {
        this.id = id;
        this.name = name;
        this.contentType = contentType;
        this.data = data;
    }

    public static Image forUpload(final String name, final String contentType, final byte[] data) {
        return new Image(0L, name, contentType, data);
    }

    /** Rehydrates a persisted row (tests and in-memory fixtures). */
    public static Image identified(final long id, final String name, final String contentType, final byte[] data) {
        return new Image(id, name, contentType, data);
    }

    public String getContentType() {
        return contentType;
    }

    public String getName() {
        return name;
    }

    public long getId() {
        return id;
    }

    public byte[] getData() {
        return data;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Image)) {
            return false;
        }
        return EntityEquality.equalsByLongId(this, this.id, ((Image) o).id);
    }

    @Override
    public int hashCode() {
        return EntityEquality.hashByLongId(this, id);
    }

    @Override
    public String toString() {
        return "Image{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", contentType='" + contentType + '\'' +
                ", data=" + Arrays.toString(data) +
                '}';
    }
}
