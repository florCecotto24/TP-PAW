package ar.edu.itba.paw.models.dto.reservation;

/**
 * Pre-formatted reservation card row for owner/rider hub JSPs (wall-local datetimes, ARS total, lowercase status key).
 */
public final class ReservationCardDisplayRow {

    private final long reservationId;
    private final long carId;
    private final long imageId;
    private final String brand;
    private final String model;
    private final String pickupDateTime;
    private final String returnDateTime;
    private final String statusKey;
    private final String totalPrice;

    public ReservationCardDisplayRow(
            final long reservationId,
            final long carId,
            final long imageId,
            final String brand,
            final String model,
            final String pickupDateTime,
            final String returnDateTime,
            final String statusKey,
            final String totalPrice) {
        this.reservationId = reservationId;
        this.carId = carId;
        this.imageId = imageId;
        this.brand = brand;
        this.model = model;
        this.pickupDateTime = pickupDateTime;
        this.returnDateTime = returnDateTime;
        this.statusKey = statusKey;
        this.totalPrice = totalPrice;
    }

    public long getReservationId() {
        return reservationId;
    }

    public long getCarId() {
        return carId;
    }

    public long getImageId() {
        return imageId;
    }

    public String getBrand() {
        return brand;
    }

    public String getModel() {
        return model;
    }

    public String getPickupDateTime() {
        return pickupDateTime;
    }

    public String getReturnDateTime() {
        return returnDateTime;
    }

    public String getStatusKey() {
        return statusKey;
    }

    public String getTotalPrice() {
        return totalPrice;
    }
}
