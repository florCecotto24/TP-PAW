package ar.edu.itba.paw.dto;

import ar.edu.itba.paw.models.Car;
import ar.edu.itba.paw.models.Listing;
import ar.edu.itba.paw.models.User;

public final class CarPublicationResult {

    private final User publisher;
    private final Car car;
    private final Listing listing;

    public CarPublicationResult(final User publisher, final Car car, final Listing listing) {
        this.publisher = publisher;
        this.car = car;
        this.listing = listing;
    }

    public User getPublisher() {
        return publisher;
    }

    public Car getCar() {
        return car;
    }

    public Listing getListing() {
        return listing;
    }
}
