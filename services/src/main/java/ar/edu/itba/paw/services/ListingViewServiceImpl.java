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

    @Autowired
    public ListingViewServiceImpl(
            final ReservationService reservationService,
            final LocationService locationService,
            final ListingAvailabilityPolicy listingAvailabilityPolicy,
            final ListingAddressFormatter listingAddressFormatter,
            @Lazy final ListingService listingService) {
        this.reservationService = reservationService;
        this.locationService = locationService;
        this.listingAvailabilityPolicy = listingAvailabilityPolicy;
        this.listingAddressFormatter = listingAddressFormatter;
        this.listingService = listingService;
    }

    @Override
    @Transactional(readOnly = true)
    public OwnerListingDetailPageModel buildOwnerListingDetailPageModel(
            final ListingDetail detail,
            final Locale locale) {
        final Listing listing = detail.getListing();
        final long ownerId = detail.getOwner().getId();
        final long listingId = listing.getId();
        final long carImageId = detail.getPictures().isEmpty() ? 0L : detail.getPictures().get(0).getImageId();
        final List<Neighborhood> allNeighborhoods = locationService.findAllNeighborhoods();
        final Long listingNbId = listing.getNeighborhoodId().orElse(null);
        final String listingNeighborhoodName = listingNbId == null
                ? null
                : allNeighborhoods.stream()
                        .filter(nb -> nb.getId() == listingNbId)
                        .map(Neighborhood::getName)
                        .findFirst()
                        .orElse(null);
        final Map<String, Long> reservationStatusCounts =
                reservationService.countListingReservationsByStatus(ownerId, listingId);
        final long reservationTotal = reservationStatusCounts.values().stream().mapToLong(Long::longValue).sum();
        final String totalEarnings = ArsMoneyFormat.format(reservationService.getListingTotalEarnings(ownerId, listingId));
        final String pendingEarnings = ArsMoneyFormat.format(reservationService.getListingPendingEarnings(ownerId, listingId));
        final long totalDaysRented = reservationService.getListingTotalDaysRented(ownerId, listingId);
        final long reservationsThisMonth = reservationService.getListingReservationsThisMonth(ownerId, listingId);
        final long cancelled = reservationStatusCounts.getOrDefault("cancelled", 0L);
        final String cancellationRateDisplay = reservationTotal > 0
                ? String.format("%.1f%%", (double) cancelled / reservationTotal * 100.0)
                : "0.0%";
        final String nextReservationDisplay = reservationService.getListingNextReservationDate(ownerId, listingId)
                .map(dt -> WallDateTimeDisplayFormat.formatUtcAsWallLocalNoSeconds(dt, locale))
                .orElse(null);
        final int forwardDays = listingAvailabilityPolicy.getMaxAvailabilityForwardWallDays();
        final LocalDate wallToday = LocalDate.now(AvailabilityPeriod.WALL_ZONE);
        final List<ListingAvailability> editPastAvailabilities = detail.getListingAvailabilities().stream()
                .filter(la -> la.getEndInclusive().isBefore(wallToday))
                .collect(Collectors.toList());
        final String listingCreatedAtDisplay =
                WallDateTimeDisplayFormat.formatUtcAsWallLocalNoSeconds(listing.getCreatedAt(), locale);
        return new OwnerListingDetailPageModel(
                allNeighborhoods,
                listingNeighborhoodName,
                listing.getStartPointNumber().orElse(null),
                listing,
                listingCreatedAtDisplay,
                detail.getCar(),
                detail.getOwner(),
                detail.getListingAvailabilities(),
                carImageId,
                listing.getStatus().name(),
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
                wallToday);
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
