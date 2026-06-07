package ar.edu.itba.paw.models.dto.reservation;

import java.util.function.BiConsumer;

import ar.edu.itba.paw.models.domain.car.Car;

/**
 * Everything the rider-facing {@code reservationForm} JSP needs from the persistence/service
 * layer. Built by {@code ReservationFormViewService} so the {@code ReservationFormController}
 * stops repeating the same five-service bundle in four different branches of {@code formSubmit}.
 *
 * The model deliberately exposes only post-resolution data (car, formatted display strings,
 * policy limits). Raw query/form parameters that the controller needs to echo back on the page
 * (availabilityId, the original from/until inputs) are left to the controller because they are
 * pure HTTP concerns.
 */
public final class ReservationFormPageModel {

    private final Car car;
    private final String resolvedCarName;
    private final String deliveryLocation;
    private final String riderForename;
    private final String riderSurname;
    private final String riderEmail;
    private final String clientReservationTotal;
    private final String reservationTotal;
    private final String fromDateTimeDisplay;
    private final String untilDateTimeDisplay;
    private final int paymentProofUploadDeadlineHours;
    private final int maxReservationBillableDays;
    private final boolean riderHasBookingDocuments;
    private final boolean riderMissingLicenseDocument;
    private final boolean riderMissingIdentityDocument;
    private final int uploadMaxProfileDocumentMegabytes;

    private ReservationFormPageModel(final Builder b) {
        this.car = b.car;
        this.resolvedCarName = b.resolvedCarName;
        this.deliveryLocation = b.deliveryLocation;
        this.riderForename = b.riderForename;
        this.riderSurname = b.riderSurname;
        this.riderEmail = b.riderEmail;
        this.clientReservationTotal = b.clientReservationTotal;
        this.reservationTotal = b.reservationTotal;
        this.fromDateTimeDisplay = b.fromDateTimeDisplay;
        this.untilDateTimeDisplay = b.untilDateTimeDisplay;
        this.paymentProofUploadDeadlineHours = b.paymentProofUploadDeadlineHours;
        this.maxReservationBillableDays = b.maxReservationBillableDays;
        this.riderHasBookingDocuments = b.riderHasBookingDocuments;
        this.riderMissingLicenseDocument = b.riderMissingLicenseDocument;
        this.riderMissingIdentityDocument = b.riderMissingIdentityDocument;
        this.uploadMaxProfileDocumentMegabytes = b.uploadMaxProfileDocumentMegabytes;
    }

    public Car getCar() {
        return car;
    }

    public String getResolvedCarName() {
        return resolvedCarName;
    }

    public String getDeliveryLocation() {
        return deliveryLocation;
    }

    public void populateModel(final BiConsumer<String, Object> put) {
        put.accept("riderForename", riderForename);
        put.accept("riderSurname", riderSurname);
        put.accept("riderEmail", riderEmail);
        put.accept("clientReservationTotal", clientReservationTotal);
        put.accept("reservationTotal", reservationTotal);
        put.accept("fromDateTimeDisplay", fromDateTimeDisplay);
        put.accept("untilDateTimeDisplay", untilDateTimeDisplay);
        put.accept("paymentProofUploadDeadlineHours", paymentProofUploadDeadlineHours);
        put.accept("maxReservationBillableDays", maxReservationBillableDays);
        put.accept("riderHasBookingDocuments", riderHasBookingDocuments);
        put.accept("riderMissingLicenseDocument", riderMissingLicenseDocument);
        put.accept("riderMissingIdentityDocument", riderMissingIdentityDocument);
        put.accept("uploadMaxProfileDocumentMegabytes", uploadMaxProfileDocumentMegabytes);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private Car car;
        private String resolvedCarName;
        private String deliveryLocation;
        private String riderForename;
        private String riderSurname;
        private String riderEmail;
        private String clientReservationTotal;
        private String reservationTotal;
        private String fromDateTimeDisplay;
        private String untilDateTimeDisplay;
        private int paymentProofUploadDeadlineHours;
        private int maxReservationBillableDays;
        private boolean riderHasBookingDocuments;
        private boolean riderMissingLicenseDocument;
        private boolean riderMissingIdentityDocument;
        private int uploadMaxProfileDocumentMegabytes;

        public Builder car(final Car v) { this.car = v; return this; }
        public Builder resolvedCarName(final String v) { this.resolvedCarName = v; return this; }
        public Builder deliveryLocation(final String v) { this.deliveryLocation = v; return this; }
        public Builder riderForename(final String v) { this.riderForename = v; return this; }
        public Builder riderSurname(final String v) { this.riderSurname = v; return this; }
        public Builder riderEmail(final String v) { this.riderEmail = v; return this; }
        public Builder clientReservationTotal(final String v) { this.clientReservationTotal = v; return this; }
        public Builder reservationTotal(final String v) { this.reservationTotal = v; return this; }
        public Builder fromDateTimeDisplay(final String v) { this.fromDateTimeDisplay = v; return this; }
        public Builder untilDateTimeDisplay(final String v) { this.untilDateTimeDisplay = v; return this; }
        public Builder paymentProofUploadDeadlineHours(final int v) { this.paymentProofUploadDeadlineHours = v; return this; }
        public Builder maxReservationBillableDays(final int v) { this.maxReservationBillableDays = v; return this; }
        public Builder riderHasBookingDocuments(final boolean v) { this.riderHasBookingDocuments = v; return this; }
        public Builder riderMissingLicenseDocument(final boolean v) { this.riderMissingLicenseDocument = v; return this; }
        public Builder riderMissingIdentityDocument(final boolean v) { this.riderMissingIdentityDocument = v; return this; }
        public Builder uploadMaxProfileDocumentMegabytes(final int v) { this.uploadMaxProfileDocumentMegabytes = v; return this; }

        public ReservationFormPageModel build() {
            return new ReservationFormPageModel(this);
        }
    }
}
