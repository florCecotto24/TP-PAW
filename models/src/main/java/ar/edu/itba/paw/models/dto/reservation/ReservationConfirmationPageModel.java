package ar.edu.itba.paw.models.dto.reservation;

import java.util.function.BiConsumer;

/**
 * Attributes the {@code reservationConfirmation} JSP needs after a successful reservation
 * submit. Built by {@code ReservationFormViewService#buildReservationConfirmation}.
 */
public final class ReservationConfirmationPageModel {

    private final String carName;
    private final String name;
    private final String surname;
    private final String email;
    private final String fromDateTime;
    private final String untilDateTime;
    private final String deliveryLocation;
    private final long reservationId;
    private final long carId;
    private final Long availabilityId;
    private final String reservationTotal;
    private final String ownerCbu;
    private final String fromDateTimeDisplay;
    private final String untilDateTimeDisplay;
    private final int paymentProofUploadDeadlineHours;
    private final int maxReservationBillableDays;
    private final long uploadMaxImageBytes;
    private final long uploadMaxImageMegabytes;

    private ReservationConfirmationPageModel(final Builder b) {
        this.carName = b.carName;
        this.name = b.name;
        this.surname = b.surname;
        this.email = b.email;
        this.fromDateTime = b.fromDateTime;
        this.untilDateTime = b.untilDateTime;
        this.deliveryLocation = b.deliveryLocation;
        this.reservationId = b.reservationId;
        this.carId = b.carId;
        this.availabilityId = b.availabilityId;
        this.reservationTotal = b.reservationTotal;
        this.ownerCbu = b.ownerCbu;
        this.fromDateTimeDisplay = b.fromDateTimeDisplay;
        this.untilDateTimeDisplay = b.untilDateTimeDisplay;
        this.paymentProofUploadDeadlineHours = b.paymentProofUploadDeadlineHours;
        this.maxReservationBillableDays = b.maxReservationBillableDays;
        this.uploadMaxImageBytes = b.uploadMaxImageBytes;
        this.uploadMaxImageMegabytes = b.uploadMaxImageMegabytes;
    }

    public long getReservationId() {
        return reservationId;
    }

    public long getCarId() {
        return carId;
    }

    public void populateModel(final BiConsumer<String, Object> put) {
        put.accept("carName", carName);
        put.accept("name", name);
        put.accept("surname", surname);
        put.accept("email", email);
        put.accept("fromDateTime", fromDateTime);
        put.accept("untilDateTime", untilDateTime);
        put.accept("deliveryLocation", deliveryLocation);
        put.accept("reservationId", reservationId);
        put.accept("carId", carId);
        put.accept("availabilityId", availabilityId);
        put.accept("reservationTotal", reservationTotal);
        put.accept("ownerCbu", ownerCbu);
        put.accept("fromDateTimeDisplay", fromDateTimeDisplay);
        put.accept("untilDateTimeDisplay", untilDateTimeDisplay);
        put.accept("paymentProofUploadDeadlineHours", paymentProofUploadDeadlineHours);
        put.accept("maxReservationBillableDays", maxReservationBillableDays);
        put.accept("uploadMaxImageBytes", uploadMaxImageBytes);
        put.accept("uploadMaxImageMegabytes", uploadMaxImageMegabytes);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String carName;
        private String name;
        private String surname;
        private String email;
        private String fromDateTime;
        private String untilDateTime;
        private String deliveryLocation;
        private long reservationId;
        private long carId;
        private Long availabilityId;
        private String reservationTotal;
        private String ownerCbu;
        private String fromDateTimeDisplay;
        private String untilDateTimeDisplay;
        private int paymentProofUploadDeadlineHours;
        private int maxReservationBillableDays;
        private long uploadMaxImageBytes;
        private long uploadMaxImageMegabytes;

        public Builder carName(final String v) { this.carName = v; return this; }
        public Builder name(final String v) { this.name = v; return this; }
        public Builder surname(final String v) { this.surname = v; return this; }
        public Builder email(final String v) { this.email = v; return this; }
        public Builder fromDateTime(final String v) { this.fromDateTime = v; return this; }
        public Builder untilDateTime(final String v) { this.untilDateTime = v; return this; }
        public Builder deliveryLocation(final String v) { this.deliveryLocation = v; return this; }
        public Builder reservationId(final long v) { this.reservationId = v; return this; }
        public Builder carId(final long v) { this.carId = v; return this; }
        public Builder availabilityId(final Long v) { this.availabilityId = v; return this; }
        public Builder reservationTotal(final String v) { this.reservationTotal = v; return this; }
        public Builder ownerCbu(final String v) { this.ownerCbu = v; return this; }
        public Builder fromDateTimeDisplay(final String v) { this.fromDateTimeDisplay = v; return this; }
        public Builder untilDateTimeDisplay(final String v) { this.untilDateTimeDisplay = v; return this; }
        public Builder paymentProofUploadDeadlineHours(final int v) { this.paymentProofUploadDeadlineHours = v; return this; }
        public Builder maxReservationBillableDays(final int v) { this.maxReservationBillableDays = v; return this; }
        public Builder uploadMaxImageBytes(final long v) { this.uploadMaxImageBytes = v; return this; }
        public Builder uploadMaxImageMegabytes(final long v) { this.uploadMaxImageMegabytes = v; return this; }

        public ReservationConfirmationPageModel build() {
            return new ReservationConfirmationPageModel(this);
        }
    }
}
