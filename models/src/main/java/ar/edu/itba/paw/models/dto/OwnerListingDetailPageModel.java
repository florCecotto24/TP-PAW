package ar.edu.itba.paw.models.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import ar.edu.itba.paw.models.domain.Car;
import ar.edu.itba.paw.models.domain.ListingAvailability;
import ar.edu.itba.paw.models.domain.Neighborhood;
import ar.edu.itba.paw.models.domain.User;

/**
 * Model attributes for the owner {@code myListingDetail} JSP (analytics, neighborhood labels, availability helpers);
 * built in the service layer from a {@link Car} and its {@link ListingAvailability} rows. The controller still adds
 * {@code editForm} and {@code activeTab}.
 */
public final class OwnerListingDetailPageModel {

    private final List<Neighborhood> allNeighborhoods;
    private final String listingNeighborhoodName;
    private final String listingStreetNumber;
    private final String listingStreetName;
    private final BigDecimal listingDayPrice;
    private final LocalTime listingCheckInTime;
    private final LocalTime listingCheckOutTime;
    private final String listingTitle;
    private final boolean hasPublishedAvailability;
    private final String listingCreatedAtDisplay;
    private final Car car;
    private final User owner;
    private final List<ListingAvailability> availabilities;
    private final long carImageId;
    private final String statusKey;
    private final Map<String, Long> reservationStatusCounts;
    private final long reservationTotal;
    private final String listingTotalEarnings;
    private final String listingPendingEarnings;
    private final long listingTotalDaysRented;
    private final long listingReservationsThisMonth;
    private final String listingCancellationRate;
    private final String listingNextReservationDisplay;
    private final List<ListingAvailability> editPastAvailabilities;
    private final String editAvailMaxYmd;
    private final LocalDate editAvailWallToday;
    private final ListingPriceMarketInsight priceMarketInsight;

    public OwnerListingDetailPageModel(
            final List<Neighborhood> allNeighborhoods,
            final String listingNeighborhoodName,
            final String listingStreetNumber,
            final String listingStreetName,
            final BigDecimal listingDayPrice,
            final LocalTime listingCheckInTime,
            final LocalTime listingCheckOutTime,
            final String listingTitle,
            final boolean hasPublishedAvailability,
            final String listingCreatedAtDisplay,
            final Car car,
            final User owner,
            final List<ListingAvailability> availabilities,
            final long carImageId,
            final String statusKey,
            final Map<String, Long> reservationStatusCounts,
            final long reservationTotal,
            final String listingTotalEarnings,
            final String listingPendingEarnings,
            final long listingTotalDaysRented,
            final long listingReservationsThisMonth,
            final String listingCancellationRate,
            final String listingNextReservationDisplay,
            final List<ListingAvailability> editPastAvailabilities,
            final String editAvailMaxYmd,
            final LocalDate editAvailWallToday,
            final ListingPriceMarketInsight priceMarketInsight) {
        this.allNeighborhoods = List.copyOf(allNeighborhoods);
        this.listingNeighborhoodName = listingNeighborhoodName;
        this.listingStreetNumber = listingStreetNumber;
        this.listingStreetName = listingStreetName;
        this.listingDayPrice = listingDayPrice;
        this.listingCheckInTime = listingCheckInTime;
        this.listingCheckOutTime = listingCheckOutTime;
        this.listingTitle = listingTitle;
        this.hasPublishedAvailability = hasPublishedAvailability;
        this.listingCreatedAtDisplay = listingCreatedAtDisplay;
        this.car = car;
        this.owner = owner;
        this.availabilities = List.copyOf(availabilities);
        this.carImageId = carImageId;
        this.statusKey = statusKey;
        this.reservationStatusCounts = Map.copyOf(reservationStatusCounts);
        this.reservationTotal = reservationTotal;
        this.listingTotalEarnings = listingTotalEarnings;
        this.listingPendingEarnings = listingPendingEarnings;
        this.listingTotalDaysRented = listingTotalDaysRented;
        this.listingReservationsThisMonth = listingReservationsThisMonth;
        this.listingCancellationRate = listingCancellationRate;
        this.listingNextReservationDisplay = listingNextReservationDisplay;
        this.editPastAvailabilities = List.copyOf(editPastAvailabilities);
        this.editAvailMaxYmd = editAvailMaxYmd;
        this.editAvailWallToday = editAvailWallToday;
        this.priceMarketInsight = priceMarketInsight;
    }

    public List<ListingAvailability> getAvailabilities() {
        return availabilities;
    }

    public boolean isHasPublishedAvailability() {
        return hasPublishedAvailability;
    }

    public BigDecimal getListingDayPrice() {
        return listingDayPrice;
    }

    public LocalTime getListingCheckInTime() {
        return listingCheckInTime;
    }

    public LocalTime getListingCheckOutTime() {
        return listingCheckOutTime;
    }

    public String getListingStreetName() {
        return listingStreetName;
    }

    public ListingPriceMarketInsight getPriceMarketInsight() {
        return priceMarketInsight;
    }

    public final void populateModel(final BiConsumer<String, Object> putObject) {
        putObject.accept("allNeighborhoods", allNeighborhoods);
        putObject.accept("listingNeighborhoodName", listingNeighborhoodName);
        putObject.accept("listingStreetNumber", listingStreetNumber);
        putObject.accept("listingStreetName", listingStreetName);
        putObject.accept("listingDayPrice", listingDayPrice);
        putObject.accept("listingCheckInTime", listingCheckInTime);
        putObject.accept("listingCheckOutTime", listingCheckOutTime);
        putObject.accept("listingTitle", listingTitle);
        putObject.accept("hasPublishedAvailability", hasPublishedAvailability);
        putObject.accept("listingCreatedAtDisplay", listingCreatedAtDisplay);
        putObject.accept("car", car);
        putObject.accept("owner", owner);
        putObject.accept("availabilities", availabilities);
        putObject.accept("carImageId", carImageId);
        putObject.accept("statusKey", statusKey);
        putObject.accept("reservationStatusCounts", reservationStatusCounts);
        putObject.accept("reservationTotal", reservationTotal);
        putObject.accept("listingTotalEarnings", listingTotalEarnings);
        putObject.accept("listingPendingEarnings", listingPendingEarnings);
        putObject.accept("listingTotalDaysRented", listingTotalDaysRented);
        putObject.accept("listingReservationsThisMonth", listingReservationsThisMonth);
        putObject.accept("listingCancellationRate", listingCancellationRate);
        putObject.accept("listingNextReservationDisplay", listingNextReservationDisplay);
        putObject.accept("editPastAvailabilities", editPastAvailabilities);
        putObject.accept("editAvailMaxYmd", editAvailMaxYmd);
        putObject.accept("editAvailWallToday", editAvailWallToday);
        if (priceMarketInsight != null) {
            putObject.accept("priceMarketInsight", priceMarketInsight);
        }
    }
}
