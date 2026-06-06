package ar.edu.itba.paw.services.car.view;


import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.itba.paw.models.domain.Car;
import ar.edu.itba.paw.models.domain.CarAvailability;
import ar.edu.itba.paw.models.domain.Neighborhood;
import ar.edu.itba.paw.models.domain.User;
import ar.edu.itba.paw.models.dto.car.BookableSegmentProjection;
import ar.edu.itba.paw.models.dto.car.CarPriceMarketInsight;
import ar.edu.itba.paw.models.dto.car.OwnerCarDetailPageModel;
import ar.edu.itba.paw.models.util.format.ArsMoneyFormat;
import ar.edu.itba.paw.models.util.time.AppTimezone;
import ar.edu.itba.paw.models.util.time.BookableWallRangesJson;
import ar.edu.itba.paw.models.util.time.WallDateTimeDisplayFormat;
import ar.edu.itba.paw.policy.CarAvailabilityPolicy;

import ar.edu.itba.paw.services.car.CarAvailabilityService;
import ar.edu.itba.paw.services.car.CarPictureService;
import ar.edu.itba.paw.services.car.CarService;
import ar.edu.itba.paw.services.location.LocationService;
import ar.edu.itba.paw.services.reservation.ReservationService;
@Service
public final class OwnerCarDetailViewServiceImpl implements OwnerCarDetailViewService {

    private final CarService carService;
    private final CarPictureService carPictureService;
    private final CarAvailabilityService carAvailabilityService;
    private final ReservationService reservationService;
    private final LocationService locationService;
    private final CarAvailabilityPolicy carAvailabilityPolicy;

    @Autowired
    public OwnerCarDetailViewServiceImpl(
            @Lazy final CarService carService,
            final CarPictureService carPictureService,
            @Lazy final CarAvailabilityService carAvailabilityService,
            @Lazy final ReservationService reservationService,
            final LocationService locationService,
            final CarAvailabilityPolicy carAvailabilityPolicy) {
        this.carService = carService;
        this.carPictureService = carPictureService;
        this.carAvailabilityService = carAvailabilityService;
        this.reservationService = reservationService;
        this.locationService = locationService;
        this.carAvailabilityPolicy = carAvailabilityPolicy;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<OwnerCarDetailPageModel> buildOwnerCarDetailPageModel(
            final long carId, final Locale locale) {
        return carService.getCarById(carId).map(car -> {
            final List<CarAvailability> availabilities = carAvailabilityService.findEffectiveOfferedByCar(carId);
            final long carImageId = carPictureService.getCarPicturesByCarId(carId).stream()
                    .map(p -> p.getImageId())
                    .filter(java.util.Objects::nonNull)
                    .findFirst()
                    .orElse(0L);
            final User owner = car.getOwner();
            final long ownerId = owner.getId();
            final List<Neighborhood> allNeighborhoods = locationService.findAllNeighborhoods();
            final CarAvailability mostRecent = availabilities.stream()
                    .max(java.util.Comparator.comparing(CarAvailability::getCreatedAt))
                    .orElse(null);
            final Long carNbId = mostRecent != null ? mostRecent.getNeighborhoodId().orElse(null) : null;
            final String carNeighborhoodName = carNbId == null
                    ? null
                    : allNeighborhoods.stream()
                            .filter(nb -> nb.getId() == carNbId)
                            .map(Neighborhood::getName)
                            .findFirst()
                            .orElse(null);
            final Map<String, Long> reservationStatusCounts =
                    reservationService.countCarReservationsByStatus(ownerId, carId);
            final long reservationTotal = reservationStatusCounts.values().stream().mapToLong(Long::longValue).sum();
            final String totalEarnings = ArsMoneyFormat.format(reservationService.getCarTotalEarnings(ownerId, carId));
            final String pendingEarnings = ArsMoneyFormat.format(reservationService.getCarPendingEarnings(ownerId, carId));
            final long totalDaysRented = reservationService.getCarTotalDaysRented(ownerId, carId);
            final long reservationsThisMonth = reservationService.getCarReservationsThisMonth(ownerId, carId);
            final long cancelled = reservationStatusCounts.getOrDefault("cancelled", 0L);
            final String cancellationRateDisplay = reservationTotal > 0
                    ? String.format("%.1f%%", (double) cancelled / reservationTotal * 100.0)
                    : "0.0%";
            final String nextReservationDisplay = reservationService.getCarNextReservationDate(ownerId, carId)
                    .map(dt -> WallDateTimeDisplayFormat.formatUtcAsWallLocalNoSeconds(dt, locale))
                    .orElse(null);
            final int forwardDays = carAvailabilityPolicy.getMaxAvailabilityForwardWallDays();
            final LocalDate wallToday = LocalDate.now(AppTimezone.WALL_ZONE);
            final List<CarAvailability> editPastAvailabilities = availabilities.stream()
                    .filter(la -> la.getEndInclusive().isBefore(wallToday))
                    .collect(Collectors.toList());
            final String carCreatedAtDisplay = mostRecent != null
                    ? WallDateTimeDisplayFormat.formatUtcAsWallLocalNoSeconds(mostRecent.getCreatedAt(), locale)
                    : null;
            final String streetName = mostRecent != null ? mostRecent.getStartPointStreet() : null;
            final String streetNumber = mostRecent != null ? mostRecent.getStartPointNumber().orElse(null) : null;
            final BigDecimal dayPrice = mostRecent != null ? mostRecent.getDayPriceValue() : null;
            final LocalTime checkInTime = mostRecent != null ? mostRecent.getCheckInTime() : null;
            final LocalTime checkOutTime = mostRecent != null ? mostRecent.getCheckOutTime() : null;
            final String brand = car.getBrand() != null ? car.getBrand() : "";
            final String model = car.getModel() != null ? car.getModel() : "";
            final String title = brand + (!brand.isEmpty() && !model.isEmpty() ? " " : "") + model;
            final CarPriceMarketInsight priceMarketInsight =
                    carService.getPriceMarketInsightForCar(car, car.getId()).orElse(null);
            final List<BookableSegmentProjection> bookableSegments =
                    carAvailabilityService.getBookableSegmentsForRiderDatePickerByCar(carId, Instant.now());
            final String bookableWallRangesJson = BookableWallRangesJson.toJsonArray(bookableSegments);
            return new OwnerCarDetailPageModel(
                    allNeighborhoods,
                    carNeighborhoodName,
                    streetNumber,
                    streetName,
                    dayPrice,
                    checkInTime,
                    checkOutTime,
                    title,
                    true,
                    carCreatedAtDisplay,
                    car,
                    owner,
                    availabilities,
                    carImageId,
                    (availabilities.isEmpty() && car.getStatus() == Car.Status.ACTIVE
                            ? Car.Status.UNAVAILABLE : car.getStatus()).name(),
                    reservationStatusCounts,
                    reservationTotal,
                    totalEarnings,
                    pendingEarnings,
                    totalDaysRented,
                    reservationsThisMonth,
                    cancellationRateDisplay,
                    nextReservationDisplay,
                    editPastAvailabilities,
                    wallToday.plusDays(forwardDays).toString(),
                    wallToday,
                    priceMarketInsight,
                    bookableWallRangesJson);
        });
    }
}
