package ar.edu.itba.paw.webapp.dto;

public final class ReservationCardView {

    private final long reservationId;
    private final long listingId;
    private final long imageId;
    private final String brand;
    private final String model;
    private final String pickupDateTime;
    private final String returnDateTime;
    private final String statusKey;
    private final String totalPrice;

    public ReservationCardView(
            final long reservationId,
            final long listingId,
            final long imageId,
            final String brand,
            final String model,
            final String pickupDateTime,
            final String returnDateTime,
            final String statusKey,
            final String totalPrice) {
        this.reservationId = reservationId;
        this.listingId = listingId;
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

    public long getListingId() {
        return listingId;
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

