package ar.edu.itba.paw.models.domain.reservation;

import java.time.OffsetDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import ar.edu.itba.paw.models.domain.file.StoredFile;
import ar.edu.itba.paw.models.domain.internal.EntityEquality;
import ar.edu.itba.paw.models.domain.user.User;

/** Append-only chat message between reservation participants (owner and rider). */
@Entity
@Table(name = "reservation_messages")
public class ReservationMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "reservation_messages_id_seq")
    @SequenceGenerator(
            name = "reservation_messages_id_seq",
            sequenceName = "reservation_messages_id_seq",
            allocationSize = 1)
    private long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id", nullable = false)
    private Reservation reservation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_user_id", nullable = false)
    private User sender;

    @Column(name = "body", nullable = false)
    private String body;

    @OneToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "attachment_file_id", unique = true)
    private StoredFile attachment;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "email_notified", nullable = false)
    private boolean emailNotified;

    @Column(name = "seen", nullable = false)
    private boolean seen;

    /* package */ ReservationMessage() {
        // For Hibernate
    }

    public ReservationMessage(
            final Reservation reservation,
            final User sender,
            final String body,
            final StoredFile attachment,
            final OffsetDateTime createdAt) {
        this.reservation = reservation;
        this.sender = sender;
        this.body = body;
        this.attachment = attachment;
        this.createdAt = createdAt;
        this.emailNotified = false;
        this.seen = false;
    }

    public long getId() {
        return id;
    }

    public Reservation getReservation() {
        return reservation;
    }

    public long getReservationId() {
        return reservation.getId();
    }

    public User getSender() {
        return sender;
    }

    public long getSenderUserId() {
        return sender.getId();
    }

    public String getBody() {
        return body;
    }

    public StoredFile getAttachment() {
        return attachment;
    }

    public Long getAttachmentFileId() {
        return attachment == null ? null : attachment.getId();
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public boolean isEmailNotified() {
        return emailNotified;
    }

    public void setEmailNotified(final boolean emailNotified) {
        this.emailNotified = emailNotified;
    }

    public boolean isSeen() {
        return seen;
    }

    public void setSeen(final boolean seen) {
        this.seen = seen;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ReservationMessage)) {
            return false;
        }
        return EntityEquality.equalsByLongId(this, getId(), ((ReservationMessage) o).getId());
    }

    @Override
    public int hashCode() {
        return EntityEquality.hashByLongId(this, id);
    }
}
