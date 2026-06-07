package ar.edu.itba.paw.models.dto.car;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import ar.edu.itba.paw.models.domain.car.Car;
import ar.edu.itba.paw.models.domain.car.CarAvailability;
import ar.edu.itba.paw.models.domain.location.Neighborhood;
import ar.edu.itba.paw.models.domain.user.User;
import ar.edu.itba.paw.models.util.rules.CbuRules;

/**
 * Model attributes for the owner {@code myCarDetail} JSP (analytics, neighborhood labels, availability helpers);
 * built in the service layer from a {@link Car} and its {@link CarAvailability} rows. The controller still adds
 * {@code editForm} and {@code activeTab}.
 */
public final class OwnerCarDetailPageModel {

    private final List<Neighborhood> allNeighborhoods;
    private final String carNeighborhoodName;
    private final String carStreetNumber;
    private final String carStreetName;
    private final BigDecimal carDayPrice;
    private final LocalTime carCheckInTime;
    private final LocalTime carCheckOutTime;
    private final String carTitle;
    private final boolean hasPublishedAvailability;
    private final String carCreatedAtDisplay;
    private final Car car;
    private final User owner;
    private final List<CarAvailability> availabilities;
    private final long carImageId;
    private final String statusKey;
    private final Map<String, Long> reservationStatusCounts;
    private final long reservationTotal;
    private final String carTotalEarnings;
    private final String carPendingEarnings;
    private final long carTotalDaysRented;
    private final long carReservationsThisMonth;
    private final String carCancellationRate;
    private final String carNextReservationDisplay;
    private final List<CarAvailability> editPastAvailabilities;
    private final String editAvailMaxYmd;
    private final LocalDate editAvailWallToday;
    private final CarPriceMarketInsight priceMarketInsight;
    private final String bookableWallRangesJson;

    public OwnerCarDetailPageModel(
            final List<Neighborhood> allNeighborhoods,
            final String carNeighborhoodName,
            final String carStreetNumber,
            final String carStreetName,
            final BigDecimal carDayPrice,
            final LocalTime carCheckInTime,
            final LocalTime carCheckOutTime,
            final String carTitle,
            final boolean hasPublishedAvailability,
            final String carCreatedAtDisplay,
            final Car car,
            final User owner,
            final List<CarAvailability> availabilities,
            final long carImageId,
            final String statusKey,
            final Map<String, Long> reservationStatusCounts,
            final long reservationTotal,
            final String carTotalEarnings,
            final String carPendingEarnings,
            final long carTotalDaysRented,
            final long carReservationsThisMonth,
            final String carCancellationRate,
            final String carNextReservationDisplay,
            final List<CarAvailability> editPastAvailabilities,
            final String editAvailMaxYmd,
            final LocalDate editAvailWallToday,
            final CarPriceMarketInsight priceMarketInsight,
            final String bookableWallRangesJson) {
        this.allNeighborhoods = List.copyOf(allNeighborhoods);
        this.carNeighborhoodName = carNeighborhoodName;
        this.carStreetNumber = carStreetNumber;
        this.carStreetName = carStreetName;
        this.carDayPrice = carDayPrice;
        this.carCheckInTime = carCheckInTime;
        this.carCheckOutTime = carCheckOutTime;
        this.carTitle = carTitle;
        this.hasPublishedAvailability = hasPublishedAvailability;
        this.carCreatedAtDisplay = carCreatedAtDisplay;
        this.car = car;
        this.owner = owner;
        this.availabilities = List.copyOf(availabilities);
        this.carImageId = carImageId;
        this.statusKey = statusKey;
        this.reservationStatusCounts = Map.copyOf(reservationStatusCounts);
        this.reservationTotal = reservationTotal;
        this.carTotalEarnings = carTotalEarnings;
        this.carPendingEarnings = carPendingEarnings;
        this.carTotalDaysRented = carTotalDaysRented;
        this.carReservationsThisMonth = carReservationsThisMonth;
        this.carCancellationRate = carCancellationRate;
        this.carNextReservationDisplay = carNextReservationDisplay;
        this.editPastAvailabilities = List.copyOf(editPastAvailabilities);
        this.editAvailMaxYmd = editAvailMaxYmd;
        this.editAvailWallToday = editAvailWallToday;
        this.priceMarketInsight = priceMarketInsight;
        this.bookableWallRangesJson = bookableWallRangesJson != null ? bookableWallRangesJson : "[]";
    }

    public List<CarAvailability> getAvailabilities() {
        return availabilities;
    }

    public boolean isHasPublishedAvailability() {
        return hasPublishedAvailability;
    }

    public BigDecimal getCarDayPrice() {
        return carDayPrice;
    }

    public LocalTime getCarCheckInTime() {
        return carCheckInTime;
    }

    public LocalTime getCarCheckOutTime() {
        return carCheckOutTime;
    }

    public String getCarStreetName() {
        return carStreetName;
    }

    public CarPriceMarketInsight getPriceMarketInsight() {
        return priceMarketInsight;
    }

    public String getBookableWallRangesJson() {
        return bookableWallRangesJson;
    }

    public final void populateModel(final BiConsumer<String, Object> putObject) {
        putObject.accept("allNeighborhoods", allNeighborhoods);
        putObject.accept("carNeighborhoodName", carNeighborhoodName);
        putObject.accept("carStreetNumber", carStreetNumber);
        putObject.accept("carStreetName", carStreetName);
        putObject.accept("carDayPrice", carDayPrice);
        putObject.accept("carCheckInTime", carCheckInTime);
        putObject.accept("carCheckOutTime", carCheckOutTime);
        putObject.accept("carTitle", carTitle);
        putObject.accept("hasPublishedAvailability", hasPublishedAvailability);
        putObject.accept("carCreatedAtDisplay", carCreatedAtDisplay);
        putObject.accept("car", car);
        putObject.accept("carModelPendingValidation", car.isModelPendingValidation());
        putObject.accept("ownerHasValidCbu", CbuRules.isValidFormat(owner.getCbu().orElse(null)));
        putObject.accept("owner", owner);
        putObject.accept("availabilities", availabilities);
        putObject.accept("carImageId", carImageId);
        putObject.accept("statusKey", statusKey);
        putObject.accept("reservationStatusCounts", reservationStatusCounts);
        putObject.accept("reservationTotal", reservationTotal);
        putObject.accept("carTotalEarnings", carTotalEarnings);
        putObject.accept("carPendingEarnings", carPendingEarnings);
        putObject.accept("carTotalDaysRented", carTotalDaysRented);
        putObject.accept("carReservationsThisMonth", carReservationsThisMonth);
        putObject.accept("carCancellationRate", carCancellationRate);
        putObject.accept("carNextReservationDisplay", carNextReservationDisplay);
        putObject.accept("editPastAvailabilities", editPastAvailabilities);
        putObject.accept("editAvailMaxYmd", editAvailMaxYmd);
        putObject.accept("editAvailWallToday", editAvailWallToday);
        if (priceMarketInsight != null) {
            putObject.accept("priceMarketInsight", priceMarketInsight);
        }
        putObject.accept("bookableWallRangesJson", bookableWallRangesJson);
    }
}
