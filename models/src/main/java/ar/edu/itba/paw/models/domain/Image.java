package ar.edu.itba.paw.models.domain;

import java.util.Arrays;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

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

    @Column(name = "byte_array", nullable = false)
    private byte[] data;

    /* package */ Image() {
        // For Hibernate
    }

    public static boolean isImageContentType(final String contentType) {
        if (contentType == null) {
            return false;
        }
        final String t = contentType.trim();
        if (t.isEmpty()) {
            return false;
        }
        return t.regionMatches(true, 0, "image/", 0, 6);
    }

    public Image(final long id, final String name, final String contentType, final byte[] data) {
        this.id = id;
        this.name = name;
        this.contentType = contentType;
        this.data = data;
    }

    public Image(final String name, final String contentType, final byte[] data) {
        this.name = name;
        this.contentType = contentType;
        this.data = data;
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
    public String toString() {
        return "Image{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", contentType='" + contentType + '\'' +
                ", data=" + Arrays.toString(data) +
                '}';
    }
}
