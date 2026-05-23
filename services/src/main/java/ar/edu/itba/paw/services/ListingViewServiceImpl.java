package ar.edu.itba.paw.services;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.itba.paw.models.domain.AvailabilityPeriod;
import ar.edu.itba.paw.models.domain.Listing;
import ar.edu.itba.paw.models.domain.ListingAvailability;
import ar.edu.itba.paw.models.domain.Neighborhood;
import ar.edu.itba.paw.models.domain.Reservation;
import ar.edu.itba.paw.models.dto.ListingDetail;
import ar.edu.itba.paw.models.dto.ListingPriceMarketInsight;
import ar.edu.itba.paw.models.dto.OwnerListingDetailPageModel;
import ar.edu.itba.paw.models.util.ArsMoneyFormat;
import ar.edu.itba.paw.models.util.WallDateTimeDisplayFormat;
import ar.edu.itba.paw.services.policy.ListingAvailabilityPolicy;
import ar.edu.itba.paw.services.util.ListingAddressFormatter;

/** Owner listing UI; address lines via {@link ListingAddressFormatter}; reservation analytics via {@link ReservationService}. */
@Service
public final class ListingViewServiceImpl implements ListingViewService {

    private final ReservationService reservationService;
    private final LocationService locationService;
    private final ListingAvailabilityPolicy listingAvailabilityPolicy;
    private final ListingAddressFormatter listingAddressFormatter;
    private final ListingService listingService;
    private final CarService carService;
    private final CarPictureService carPictureService;
    private final ListingAvailabilityService listingAvailabilityService;

    @Autowired
    public ListingViewServiceImpl(
            final ReservationService reservationService,
            final LocationService locationService,
            final ListingAvailabilityPolicy listingAvailabilityPolicy,
            final ListingAddressFormatter listingAddressFormatter,
            @Lazy final ListingService listingService,
            final CarService carService,
            final CarPictureService carPictureService,
            @Lazy final ListingAvailabilityService listingAvailabilityService) {
        this.reservationService = reservationService;
        this.locationService = locationService;
        this.listingAvailabilityPolicy = listingAvailabilityPolicy;
        this.listingAddressFormatter = listingAddressFormatter;
        this.listingService = listingService;
        this.carService = carService;
        this.carPictureService = carPictureService;
        this.listingAvailabilityService = listingAvailabilityService;
    }

    @Override
    @Transactional(readOnly = true)
    public OwnerListingDetailPageModel buildOwnerListingDetailPageModel(
            final ListingDetail detail,
            final Locale locale) {
        return buildOwnerCarDetailPageModelInternal(
                detail.getCar(),
                detail.getOwner(),
                detail.getListingAvailabilities(),
                detail.getPictures().isEmpty() ? 0L : detail.getPictures().get(0).getImageId(),
                detail.getListing().getId(),
                locale);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<OwnerListingDetailPageModel> buildOwnerCarDetailPageModel(final long carId, final Locale locale) {
        return listingService.findMostRecentListingByCar(carId)
                .flatMap(l -> listingService.getListingDetailById(l.getId()))
                .map(detail -> buildOwnerListingDetailPageModel(detail, locale));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<OwnerListingDetailPageModel> buildOwnerCarDetailPageModelFromCar(
            final long carId, final Locale locale) {
        return carService.getCarById(carId).map(car -> {
            final List<ListingAvailability> availabilities = listingAvailabilityService.findByCarId(carId);
            final long carImageId = carPictureService.getCarPicturesByCarId(carId).stream()
                    .findFirst().map(p -> p.getImageId()).orElse(0L);
            final Long excludeListingId = listingService.findMostRecentListingByCar(carId)
                    .map(Listing::getId)
                    .orElse(null);
            return buildOwnerCarDetailPageModelInternal(
                    car, car.getOwner(), availabilities, carImageId, excludeListingId, locale);
        });
    }

    private OwnerListingDetailPageModel buildOwnerCarDetailPageModelInternal(
            final ar.edu.itba.paw.models.domain.Car car,
            final ar.edu.itba.paw.models.domain.User owner,
            final List<ListingAvailability> availabilities,
            final long carImageId,
            final Long excludeListingId,
            final Locale locale) {
        final long ownerId = owner.getId();
        final long carId = car.getId();
        final List<Neighborhood> allNeighborhoods = locationService.findAllNeighborhoods();
        final ListingAvailability mostRecent = availabilities.stream()
                .max(java.util.Comparator.comparing(ListingAvailability::getCreatedAt))
                .orElse(null);
        final Long listingNbId = mostRecent != null ? mostRecent.getNeighborhoodId().orElse(null) : null;
        final String listingNeighborhoodName = listingNbId == null
                ? null
                : allNeighborhoods.stream()
                        .filter(nb -> nb.getId() == listingNbId)
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
        final int forwardDays = listingAvailabilityPolicy.getMaxAvailabilityForwardWallDays();
        final LocalDate wallToday = LocalDate.now(AvailabilityPeriod.WALL_ZONE);
        final List<ListingAvailability> editPastAvailabilities = availabilities.stream()
                .filter(la -> la.getEndInclusive().isBefore(wallToday))
                .collect(Collectors.toList());
        final String listingCreatedAtDisplay = mostRecent != null
                ? WallDateTimeDisplayFormat.formatUtcAsWallLocalNoSeconds(mostRecent.getCreatedAt(), locale)
                : null;
        final String streetName = mostRecent != null ? mostRecent.getStartPointStreet() : null;
        final String streetNumber = mostRecent != null ? mostRecent.getStartPointNumber().orElse(null) : null;
        final BigDecimal dayPrice = mostRecent != null ? mostRecent.getDayPriceValue() : null;
        final java.time.LocalTime checkInTime = mostRecent != null ? mostRecent.getCheckInTime() : null;
        final java.time.LocalTime checkOutTime = mostRecent != null ? mostRecent.getCheckOutTime() : null;
        final String brand = car.getBrand() != null ? car.getBrand() : "";
        final String model = car.getModel() != null ? car.getModel() : "";
        final String title = brand + (!brand.isEmpty() && !model.isEmpty() ? " " : "") + model;
        final ListingPriceMarketInsight priceMarketInsight = listingService
                .getPriceMarketInsightForCar(car, excludeListingId)
                .orElse(null);
        return new OwnerListingDetailPageModel(
                allNeighborhoods,
                listingNeighborhoodName,
                streetNumber,
                streetName,
                dayPrice,
                checkInTime,
                checkOutTime,
                title,
                mostRecent != null,
                listingCreatedAtDisplay,
                car,
                owner,
                availabilities,
                carImageId,
                car.getStatus().name(),
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
                priceMarketInsight);
    }


    @Override
    @Transactional(readOnly = true)
    public String formatPublicDeliveryLocation(final Listing listing) {
        return listingAddressFormatter.formatPublicDeliveryLocation(listing);
    }

    @Override
    @Transactional(readOnly = true)
    public String formatFullDeliveryLocation(final Listing listing) {
        return listingAddressFormatter.formatFullDeliveryLocation(listing);
    }

    @Override
    @Transactional(readOnly = true)
    public String formatPublicPickupLocation(final Listing listing) {
        return listingAddressFormatter.formatPublicPickupLocation(listing);
    }

    @Override
    @Transactional(readOnly = true)
    public String formatFullPickupLocation(final Listing listing) {
        return listingAddressFormatter.formatFullPickupLocation(listing);
    }

    @Override
    @Transactional(readOnly = true)
    public String formatDeliveryForReservationView(
            final Listing listing,
            final Reservation reservation,
            final boolean viewerIsOwner) {
        return listingAddressFormatter.formatDeliveryForReservationView(listing, reservation, viewerIsOwner);
    }

    @Override
    @Transactional(readOnly = true)
    public String formatPickupForReservationView(
            final Listing listing,
            final Reservation reservation,
            final boolean viewerIsOwner) {
        return listingAddressFormatter.formatPickupForReservationView(listing, reservation, viewerIsOwner);
    }

    @Override
    @Transactional(readOnly = true)
    public String formatRiderReservationHandoverSummary(final Listing listing, final Reservation reservation) {
        return listingAddressFormatter.formatRiderReservationHandoverSummary(listing, reservation);
    }

    @Override
    @Transactional(readOnly = true)
    public String formatOwnerReservationHandoverSummary(final Listing listing) {
        return listingAddressFormatter.formatOwnerReservationHandoverSummary(listing);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal resolveMinEffectiveDayPrice(final ListingDetail detail) {
        final BigDecimal listingPrice = detail.getListing().getDayPrice();
        final LocalDate today = LocalDate.now(AvailabilityPeriod.WALL_ZONE);
        BigDecimal min = listingPrice;
        for (final ListingAvailability la : detail.getListingAvailabilities()) {
            if (la.getEndInclusive().isBefore(today)) {
                continue;
            }
            final BigDecimal periodPrice = la.getDayPriceValue();
            if (periodPrice != null && periodPrice.compareTo(min) < 0) {
                min = periodPrice;
            }
        }
        return min;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isListingPriceVariable(final ListingDetail detail) {
        final BigDecimal listingPrice = detail.getListing().getDayPrice();
        final LocalDate today = LocalDate.now(AvailabilityPeriod.WALL_ZONE);
        for (final ListingAvailability la : detail.getListingAvailabilities()) {
            if (la.getEndInclusive().isBefore(today)) {
                continue;
            }
            final BigDecimal periodPrice = la.getDayPriceValue();
            if (periodPrice != null && periodPrice.compareTo(listingPrice) != 0) {
                return true;
            }
        }
        return false;
    }
}
