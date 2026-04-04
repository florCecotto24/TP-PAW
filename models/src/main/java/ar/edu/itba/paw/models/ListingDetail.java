package ar.edu.itba.paw.models;

import java.util.List;

public final class ListingDetail {

    private final Listing listing;
    private final Car car;
    private final User owner;
    private final List<CarPicture> pictures;
    private final List<ListingAvailability> listingAvailabilities;

    public ListingDetail(
            final Listing listing,
            final Car car,
            final User owner,
            final List<CarPicture> pictures,
            final List<ListingAvailability> listingAvailabilities) {
        this.listing = listing;
        this.car = car;
        this.owner = owner;
        this.pictures = List.copyOf(pictures);
        this.listingAvailabilities = List.copyOf(listingAvailabilities);
    }

    public Listing getListing() {
        return listing;
    }

    public Car getCar() {
        return car;
    }

    public User getOwner() {
        return owner;
    }

    public List<CarPicture> getPictures() {
        return pictures;
    }

    public List<ListingAvailability> getListingAvailabilities() {
        return listingAvailabilities;
    }
}
