package ar.edu.itba.paw.models;

import java.util.List;

public final class ListingDetail {

    private final Listing listing;
    private final Car car;
    private final List<CarPicture> pictures;
    private final List<ListingAvailability> listingAvailabilities;

    public ListingDetail(
            final Listing listing,
            final Car car,
            final List<CarPicture> pictures,
            final List<ListingAvailability> listingAvailabilities) {
        this.listing = listing;
        this.car = car;
        this.pictures = List.copyOf(pictures);
        this.listingAvailabilities = List.copyOf(listingAvailabilities);
    }

    public Listing getListing() {
        return listing;
    }

    public Car getCar() {
        return car;
    }

    public List<CarPicture> getPictures() {
        return pictures;
    }

    public List<ListingAvailability> getListingAvailabilities() {
        return listingAvailabilities;
    }
}
