package ar.edu.itba.paw.models.domain;

import java.time.OffsetDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import ar.edu.itba.paw.models.domain.internal.EntityEquality;

/** User-uploaded binary blob with filename, MIME type, and creation timestamp. */
@Entity
@Table(name = "stored_files")
public class StoredFile {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "stored_files_id_seq")
    @SequenceGenerator(name = "stored_files_id_seq", sequenceName = "stored_files_id_seq", allocationSize = 1)
    private long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploader_user_id", nullable = false)
    private User uploader;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "byte_array", nullable = false)
    private byte[] data;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    /* package */ StoredFile() {
        // For Hibernate
    }

    public StoredFile(
            final long id,
            final User uploader,
            final String fileName,
            final String contentType,
            final byte[] data,
            final OffsetDateTime createdAt) {
        this.id = id;
        this.uploader = uploader;
        this.fileName = fileName;
        this.contentType = contentType;
        this.data = data;
        this.createdAt = createdAt;
    }

    public StoredFile(
            final User uploader,
            final String fileName,
            final String contentType,
            final byte[] data,
            final OffsetDateTime createdAt) {
        this.uploader = uploader;
        this.fileName = fileName;
        this.contentType = contentType;
        this.data = data;
        this.createdAt = createdAt;
    }

    public long getId() {
        return id;
    }

    public User getUploader() {
        return uploader;
    }

    /** Convenience accessor — returns {@code uploader.getId()}. */
    public long getUploaderUserId() {
        return uploader.getId();
    }

    public String getFileName() {
        return fileName;
    }

    public String getContentType() {
        return contentType;
    }

    public byte[] getData() {
        return data;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public static boolean isAllowedPaymentReceiptContentType(final String contentType) {
        if (contentType == null) {
            return false;
        }
        final String t = contentType.trim().toLowerCase();
        return t.startsWith("image/")
                || "application/pdf".equals(t);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof StoredFile)) {
            return false;
        }
        return EntityEquality.equalsByLongId(this, this.id, ((StoredFile) o).id);
    }

    @Override
    public int hashCode() {
        return EntityEquality.hashByLongId(this, id);
    }
}
