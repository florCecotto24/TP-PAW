package ar.edu.itba.paw.models.domain;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.Optional;

import javax.persistence.AttributeConverter;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Converter;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

/**
 * Rental agreement between a rider and a listing: UTC interval, money total, payment proof state, and lifecycle status.
 */
@Entity
@Table(name = "reservations")
public class Reservation {

    /**
     * Stored in lowercase with underscores ({@code pending}, {@code cancelled_by_rider}, …).
     * Participant-driven cancellations use {@code cancelled_by_*}; automated rules use {@code cancelled_due_to_*}
     * (English {@code due to} reads more naturally than {@code by} when there is no cancelling user).
     * {@link #CANCELLED} remains for reservations cancelled before granular reasons existed.
     */
    public enum Status {
        PENDING,
        ACCEPTED,
        STARTED,
        CANCELLED,
        CANCELLED_BY_RIDER,
        CANCELLED_BY_OWNER,
        CANCELLED_DUE_TO_MISSING_PAYMENT_PROOF,
        FINISHED
    }

    @Converter
    public static class StatusConverter implements AttributeConverter<Status, String> {
        @Override
        public String convertToDatabaseColumn(final Status attribute) {
            return attribute == null ? null : attribute.name().toLowerCase();
        }
        @Override
        public Status convertToEntityAttribute(final String dbData) {
            return dbData == null ? null : Status.valueOf(dbData.toUpperCase());
        }
    }

    /**
     * Any terminal cancellation status, including legacy {@link Status#CANCELLED}.
     */
    public static boolean isCancelledStatus(final Status status) {
        if (status == null) {
            return false;
        }
        return switch (status) {
            case CANCELLED, CANCELLED_BY_RIDER, CANCELLED_BY_OWNER, CANCELLED_DUE_TO_MISSING_PAYMENT_PROOF -> true;
            default -> false;
        };
    }

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "reservations_id_seq")
    @SequenceGenerator(name = "reservations_id_seq", sequenceName = "reservations_id_seq", allocationSize = 1)
    private long id;

    @Column(name = "rider_id", nullable = false)
    private long riderId;

    @Column(name = "listing_id", nullable = false)
    private long listingId;

    @Column(name = "start_date", nullable = false)
    private OffsetDateTime startDate;

    @Column(name = "end_date", nullable = false)
    private OffsetDateTime endDate;

    @Convert(converter = StatusConverter.class)
    @Column(nullable = false, length = 60)
    private Status status;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "total_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalPrice;

    @Column(name = "payment_receipt_file_id")
    private Long paymentReceiptFileId;

    @Column(name = "payment_approved", nullable = false)
    private boolean paymentApproved;

    @Column(name = "payment_proof_deadline_at")
    private OffsetDateTime paymentProofDeadlineAt;

    @Column(name = "car_returned", nullable = false)
    private boolean carReturned;

    @Column(name = "payment_refund_required", nullable = false)
    private boolean paymentRefundRequired;

    @Column(name = "payment_refund_receipt_file_id")
    private Long paymentRefundReceiptFileId;

    @Column(name = "payment_refund_approved", nullable = false)
    private boolean paymentRefundApproved;

    @Column(name = "refund_proof_deadline_at")
    private OffsetDateTime refundProofDeadlineAt;

    /* package */ Reservation() {
        // For Hibernate
    }

    private Reservation(final Builder b) {
        this.id = b.id;
        this.riderId = b.riderId;
        this.listingId = b.listingId;
        this.startDate = b.startDate;
        this.endDate = b.endDate;
        this.status = b.status;
        this.createdAt = b.createdAt;
        this.updatedAt = b.updatedAt;
        this.totalPrice = b.totalPrice;
        this.paymentReceiptFileId = b.paymentReceiptFileId;
        this.paymentApproved = b.paymentApproved;
        this.paymentProofDeadlineAt = b.paymentProofDeadlineAt;
        this.carReturned = b.carReturned;
        this.paymentRefundRequired = b.paymentRefundRequired;
        this.paymentRefundReceiptFileId = b.paymentRefundReceiptFileId;
        this.paymentRefundApproved = b.paymentRefundApproved;
        this.refundProofDeadlineAt = b.refundProofDeadlineAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private long id;
        private long riderId;
        private long listingId;
        private OffsetDateTime startDate;
        private OffsetDateTime endDate;
        private Status status;
        private OffsetDateTime createdAt;
        private OffsetDateTime updatedAt;
        private BigDecimal totalPrice;
        private Long paymentReceiptFileId;
        private boolean paymentApproved;
        private OffsetDateTime paymentProofDeadlineAt;
        private boolean carReturned;
        private boolean paymentRefundRequired;
        private Long paymentRefundReceiptFileId;
        private boolean paymentRefundApproved;
        private OffsetDateTime refundProofDeadlineAt;

        public Builder id(final long id) {
            this.id = id;
            return this;
        }

        public Builder riderId(final long riderId) {
            this.riderId = riderId;
            return this;
        }

        public Builder listingId(final long listingId) {
            this.listingId = listingId;
            return this;
        }

        public Builder startDate(final OffsetDateTime startDate) {
            this.startDate = startDate;
            return this;
        }

        public Builder endDate(final OffsetDateTime endDate) {
            this.endDate = endDate;
            return this;
        }

        public Builder status(final Status status) {
            this.status = status;
            return this;
        }

        public Builder createdAt(final OffsetDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder updatedAt(final OffsetDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public Builder totalPrice(final BigDecimal totalPrice) {
            this.totalPrice = totalPrice;
            return this;
        }

        public Builder paymentReceiptFileId(final Long paymentReceiptFileId) {
            this.paymentReceiptFileId = paymentReceiptFileId;
            return this;
        }

        public Builder paymentApproved(final boolean paymentApproved) {
            this.paymentApproved = paymentApproved;
            return this;
        }

        public Builder paymentProofDeadlineAt(final OffsetDateTime paymentProofDeadlineAt) {
            this.paymentProofDeadlineAt = paymentProofDeadlineAt;
            return this;
        }

        public Builder carReturned(final boolean carReturned) {
            this.carReturned = carReturned;
            return this;
        }

        public Builder paymentRefundRequired(final boolean paymentRefundRequired) {
            this.paymentRefundRequired = paymentRefundRequired;
            return this;
        }

        public Builder paymentRefundReceiptFileId(final Long paymentRefundReceiptFileId) {
            this.paymentRefundReceiptFileId = paymentRefundReceiptFileId;
            return this;
        }

        public Builder paymentRefundApproved(final boolean paymentRefundApproved) {
            this.paymentRefundApproved = paymentRefundApproved;
            return this;
        }

        public Builder refundProofDeadlineAt(final OffsetDateTime refundProofDeadlineAt) {
            this.refundProofDeadlineAt = refundProofDeadlineAt;
            return this;
        }

        public Reservation build() {
            Objects.requireNonNull(startDate, "startDate");
            Objects.requireNonNull(endDate, "endDate");
            Objects.requireNonNull(status, "status");
            Objects.requireNonNull(createdAt, "createdAt");
            Objects.requireNonNull(updatedAt, "updatedAt");
            Objects.requireNonNull(totalPrice, "totalPrice");
            return new Reservation(this);
        }
    }

    public long getId() {
        return id;
    }

    public long getRiderId() {
        return riderId;
    }

    public long getListingId() {
        return listingId;
    }

    public OffsetDateTime getStartDate() {
        return startDate;
    }

    public OffsetDateTime getEndDate() {
        return endDate;
    }

    public Status getStatus() {
        return status;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public Optional<Long> getPaymentReceiptFileId() {
        return Optional.ofNullable(paymentReceiptFileId);
    }

    public boolean isPaymentApproved() {
        return paymentApproved;
    }

    public Optional<OffsetDateTime> getPaymentProofDeadlineAt() {
        return Optional.ofNullable(paymentProofDeadlineAt);
    }

    public boolean isCarReturned() {
        return carReturned;
    }

    public boolean isPaymentRefundRequired() {
        return paymentRefundRequired;
    }

    public Optional<Long> getPaymentRefundReceiptFileId() {
        return Optional.ofNullable(paymentRefundReceiptFileId);
    }

    public boolean isPaymentRefundApproved() {
        return paymentRefundApproved;
    }

    public Optional<OffsetDateTime> getRefundProofDeadlineAt() {
        return Optional.ofNullable(refundProofDeadlineAt);
    }

    @Override
    public String toString() {
        return "Reservation{" +
                "id=" + id +
                ", riderId=" + riderId +
                ", listingId=" + listingId +
                ", startDate=" + startDate +
                ", endDate=" + endDate +
                ", status=" + status +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                ", totalPrice=" + totalPrice +
                ", carReturned=" + carReturned +
                '}';
    }
}
