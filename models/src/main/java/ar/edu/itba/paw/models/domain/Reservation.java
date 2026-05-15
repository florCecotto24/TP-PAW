package ar.edu.itba.paw.models.domain;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.Optional;

/**
 * Rental agreement between a rider and a listing: UTC interval, money total, payment proof state, and lifecycle status.
 */
public final class Reservation {

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

    private final long id;
    private final long riderId;
    private final long listingId;
    private final OffsetDateTime startDate;
    private final OffsetDateTime endDate;
    private final Status status;
    private final OffsetDateTime createdAt;
    private final OffsetDateTime updatedAt;
    private final BigDecimal totalPrice;
    private final Long paymentReceiptFileId;
    private final boolean paymentApproved;
    private final OffsetDateTime paymentProofDeadlineAt;
    private final boolean carReturned;
    private final boolean paymentRefundRequired;
    private final Long paymentRefundReceiptFileId;
    private final boolean paymentRefundApproved;
    private final OffsetDateTime refundProofDeadlineAt;

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
