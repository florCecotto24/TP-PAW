package ar.edu.itba.paw.models;

public class CarPicture {
    private final long id;
    private final long carId;
    private final long imageId;
    private final int order;

    public CarPicture(final long id, final long carId, final long imageId, final int order) {
        this.id = id;
        this.carId = carId;
        this.imageId = imageId;
        this.order = order;
    }

    public long getId() {
        return id;
    }

    public long getCarId() {
        return carId;
    }

    public long getImageId() {
        return imageId;
    }

    public int getOrder() {
        return order;
    }

    @Override
    public String toString() {
        return "CarPictures{" +
                "id=" + id +
                ", carId=" + carId +
                ", imageId=" + imageId +
                ", order=" + order +
                '}';
    }
}
