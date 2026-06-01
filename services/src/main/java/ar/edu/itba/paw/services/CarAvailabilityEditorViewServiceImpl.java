package ar.edu.itba.paw.services;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.itba.paw.models.domain.Car;
import ar.edu.itba.paw.models.domain.User;
import ar.edu.itba.paw.models.dto.car.CarPriceMarketInsight;
import ar.edu.itba.paw.models.dto.car.CarAvailabilityEditorPageModel;
import ar.edu.itba.paw.models.util.time.AppTimezone;
import ar.edu.itba.paw.services.policy.ReservationTimingPolicy;

/**
 * Implementation of {@link CarAvailabilityEditorViewService}. Centralises the orchestration of
 * {@link UserService} (publisher CBU + email), {@link LocationService} (neighborhoods),
 * {@link CarAvailabilityService} (min / max wall-day window), {@link ReservationTimingPolicy}
 * (pickup lead hours) and {@link CarService} (market-price insight) that both
 * {@code buildCreateListingView} and {@code buildEditAvailabilityView} used to perform inline.
 */
@Service
public final class CarAvailabilityEditorViewServiceImpl implements CarAvailabilityEditorViewService {

    private final UserService userService;
    private final LocationService locationService;
    private final CarAvailabilityService carAvailabilityService;
    private final ReservationTimingPolicy reservationTimingPolicy;
    private final CarService carService;

    @Autowired
    public CarAvailabilityEditorViewServiceImpl(
            final UserService userService,
            final LocationService locationService,
            final CarAvailabilityService carAvailabilityService,
            final ReservationTimingPolicy reservationTimingPolicy,
            final CarService carService) {
        this.userService = userService;
        this.locationService = locationService;
        this.carAvailabilityService = carAvailabilityService;
        this.reservationTimingPolicy = reservationTimingPolicy;
        this.carService = carService;
    }

    @Override
    @Transactional(readOnly = true)
    public CarAvailabilityEditorPageModel loadEditorContext(
            final Car car, final long userId, final LocalTime checkInTime) {
        final User freshUser = userService.getUserById(userId).orElse(null);
        final boolean userHasCbu = freshUser != null && userService.hasValidCbu(freshUser);
        final String publisherEmail = freshUser != null ? freshUser.getEmail() : "";

        final LocalDate minAvail = carAvailabilityService.getPublicationMinAvailabilityFirstWallDay(
                checkInTime, Instant.now());
        final int forwardDays = carAvailabilityService.getConfiguredMaxAvailabilityForwardWallDays();
        final LocalDate wallToday = LocalDate.now(AppTimezone.WALL_ZONE);

        final CarPriceMarketInsight insightOrNull = carService.getPriceMarketInsightForCar(car, null).orElse(null);

        return new CarAvailabilityEditorPageModel(
                car,
                userHasCbu,
                locationService.findAllNeighborhoods(),
                minAvail.toString(),
                reservationTimingPolicy.getPickupLeadHours(),
                forwardDays,
                wallToday.plusDays(forwardDays).toString(),
                publisherEmail,
                insightOrNull);
    }
}
