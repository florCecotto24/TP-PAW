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
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

/**
 * Rental agreement between a rider and a car: UTC interval, money total, payment proof state, and lifecycle status.
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rider_id", nullable = false)
    private User rider;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "car_id", nullable = false)
    private Car car;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_receipt_file_id", nullable = true)
    private StoredFile paymentReceiptFile;

    @Column(name = "payment_approved", nullable = false)
    private boolean paymentApproved;

    @Column(name = "payment_proof_deadline_at")
    private OffsetDateTime paymentProofDeadlineAt;

    @Column(name = "car_returned", nullable = false)
    private boolean carReturned;

    @Column(name = "payment_refund_required", nullable = false)
    private boolean paymentRefundRequired;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_refund_receipt_file_id", nullable = true)
    private StoredFile paymentRefundReceiptFile;

    @Column(name = "payment_refund_approved", nullable = false)
    private boolean paymentRefundApproved;

    @Column(name = "refund_proof_deadline_at")
    private OffsetDateTime refundProofDeadlineAt;

    @Column(name = "return_reminder_email_sent", nullable = false)
    private boolean returnReminderEmailSent;

    @Column(name = "return_checkout_email_sent", nullable = false)
    private boolean returnCheckoutEmailSent;

    @Column(name = "rider_review_invite_email_sent", nullable = false)
    private boolean riderReviewInviteEmailSent;

    @Column(name = "pending_paymentproof_email_sent", nullable = false)
    private boolean pendingPaymentproofEmailSent;

    @Column(name = "pending_refund_email_sent", nullable = false)
    private boolean pendingRefundEmailSent;

    /* package */ Reservation() {
        // For Hibernate
    }

    private Reservation(final Builder b) {
        this.id = b.id;
        this.rider = b.rider;
        this.car = b.car;
        this.startDate = b.startDate;
        this.endDate = b.endDate;
        this.status = b.status;
        this.createdAt = b.createdAt;
        this.updatedAt = b.updatedAt;
        this.totalPrice = b.totalPrice;
        this.paymentReceiptFile = b.paymentReceiptFile;
        this.paymentApproved = b.paymentApproved;
        this.paymentProofDeadlineAt = b.paymentProofDeadlineAt;
        this.carReturned = b.carReturned;
        this.paymentRefundRequired = b.paymentRefundRequired;
        this.paymentRefundReceiptFile = b.paymentRefundReceiptFile;
        this.paymentRefundApproved = b.paymentRefundApproved;
        this.refundProofDeadlineAt = b.refundProofDeadlineAt;
        this.returnReminderEmailSent = b.returnReminderEmailSent;
        this.returnCheckoutEmailSent = b.returnCheckoutEmailSent;
        this.riderReviewInviteEmailSent = b.riderReviewInviteEmailSent;
        this.pendingPaymentproofEmailSent = b.pendingPaymentproofEmailSent;
        this.pendingRefundEmailSent = b.pendingRefundEmailSent;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private long id;
        private User rider;
        private Car car;
        private OffsetDateTime startDate;
        private OffsetDateTime endDate;
        private Status status;
        private OffsetDateTime createdAt;
        private OffsetDateTime updatedAt;
        private BigDecimal totalPrice;
        private StoredFile paymentReceiptFile;
        private boolean paymentApproved;
        private OffsetDateTime paymentProofDeadlineAt;
        private boolean carReturned;
        private boolean paymentRefundRequired;
        private StoredFile paymentRefundReceiptFile;
        private boolean paymentRefundApproved;
        private OffsetDateTime refundProofDeadlineAt;
        private boolean returnReminderEmailSent;
        private boolean returnCheckoutEmailSent;
        private boolean riderReviewInviteEmailSent;
        private boolean pendingPaymentproofEmailSent;
        private boolean pendingRefundEmailSent;

        public Builder id(final long id) {
            this.id = id;
            return this;
        }

        public Builder rider(final User rider) {
            this.rider = rider;
            return this;
        }

        public Builder car(final Car car) {
            this.car = car;
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

        public Builder paymentReceiptFile(final StoredFile paymentReceiptFile) {
            this.paymentReceiptFile = paymentReceiptFile;
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

        public Builder paymentRefundReceiptFile(final StoredFile paymentRefundReceiptFile) {
            this.paymentRefundReceiptFile = paymentRefundReceiptFile;
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

        public Builder returnReminderEmailSent(final boolean returnReminderEmailSent) {
            this.returnReminderEmailSent = returnReminderEmailSent;
            return this;
        }

        public Builder returnCheckoutEmailSent(final boolean returnCheckoutEmailSent) {
            this.returnCheckoutEmailSent = returnCheckoutEmailSent;
            return this;
        }

        public Builder riderReviewInviteEmailSent(final boolean riderReviewInviteEmailSent) {
            this.riderReviewInviteEmailSent = riderReviewInviteEmailSent;
            return this;
        }

        public Builder pendingPaymentproofEmailSent(final boolean pendingPaymentproofEmailSent) {
            this.pendingPaymentproofEmailSent = pendingPaymentproofEmailSent;
            return this;
        }

        public Builder pendingRefundEmailSent(final boolean pendingRefundEmailSent) {
            this.pendingRefundEmailSent = pendingRefundEmailSent;
            return this;
        }

        public Reservation build() {
            Objects.requireNonNull(rider, "rider");
            Objects.requireNonNull(car, "car");
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

    public User getRider() {
        return rider;
    }

    /** Convenience accessor — returns {@code rider.getId()}. */
    public long getRiderId() {
        return rider.getId();
    }

    public Car getCar() {
        return car;
    }

    public long getCarId() {
        return car.getId();
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

    public Optional<StoredFile> getPaymentReceiptFile() {
        return Optional.ofNullable(paymentReceiptFile);
    }

    /** Convenience accessor — returns the payment receipt file id, or empty if no file is set. */
    public Optional<Long> getPaymentReceiptFileId() {
        return paymentReceiptFile == null ? Optional.empty() : Optional.of(paymentReceiptFile.getId());
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

    public Optional<StoredFile> getPaymentRefundReceiptFile() {
        return Optional.ofNullable(paymentRefundReceiptFile);
    }

    /** Convenience accessor — returns the refund receipt file id, or empty if no file is set. */
    public Optional<Long> getPaymentRefundReceiptFileId() {
        return paymentRefundReceiptFile == null ? Optional.empty() : Optional.of(paymentRefundReceiptFile.getId());
    }

    public boolean isPaymentRefundApproved() {
        return paymentRefundApproved;
    }

    public Optional<OffsetDateTime> getRefundProofDeadlineAt() {
        return Optional.ofNullable(refundProofDeadlineAt);
    }

    public void setStatus(final Status status) {
        this.status = status;
    }

    public void setUpdatedAt(final OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public void setPaymentReceiptFile(final StoredFile paymentReceiptFile) {
        this.paymentReceiptFile = paymentReceiptFile;
    }

    public void setPaymentApproved(final boolean paymentApproved) {
        this.paymentApproved = paymentApproved;
    }

    public void setCarReturned(final boolean carReturned) {
        this.carReturned = carReturned;
    }

    public void setPaymentRefundRequired(final boolean paymentRefundRequired) {
        this.paymentRefundRequired = paymentRefundRequired;
    }

    public void setPaymentRefundReceiptFile(final StoredFile paymentRefundReceiptFile) {
        this.paymentRefundReceiptFile = paymentRefundReceiptFile;
    }

    public void setPaymentRefundApproved(final boolean paymentRefundApproved) {
        this.paymentRefundApproved = paymentRefundApproved;
    }

    public void setRefundProofDeadlineAt(final OffsetDateTime refundProofDeadlineAt) {
        this.refundProofDeadlineAt = refundProofDeadlineAt;
    }

    public void setReturnReminderEmailSent(final boolean returnReminderEmailSent) {
        this.returnReminderEmailSent = returnReminderEmailSent;
    }

    public void setReturnCheckoutEmailSent(final boolean returnCheckoutEmailSent) {
        this.returnCheckoutEmailSent = returnCheckoutEmailSent;
    }

    public void setRiderReviewInviteEmailSent(final boolean riderReviewInviteEmailSent) {
        this.riderReviewInviteEmailSent = riderReviewInviteEmailSent;
    }

    public void setPendingPaymentproofEmailSent(final boolean pendingPaymentproofEmailSent) {
        this.pendingPaymentproofEmailSent = pendingPaymentproofEmailSent;
    }

    public void setPendingRefundEmailSent(final boolean pendingRefundEmailSent) {
        this.pendingRefundEmailSent = pendingRefundEmailSent;
    }

    public boolean isReturnReminderEmailSent() {
        return returnReminderEmailSent;
    }

    public boolean isReturnCheckoutEmailSent() {
        return returnCheckoutEmailSent;
    }

    public boolean isRiderReviewInviteEmailSent() {
        return riderReviewInviteEmailSent;
    }

    public boolean isPendingPaymentproofEmailSent() {
        return pendingPaymentproofEmailSent;
    }

    public boolean isPendingRefundEmailSent() {
        return pendingRefundEmailSent;
    }

    @Override
    public String toString() {
        return "Reservation{" +
                "id=" + id +
                ", riderId=" + rider.getId() +
                ", carId=" + (car != null ? car.getId() : "null") +
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
