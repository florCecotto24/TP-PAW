package ar.edu.itba.paw.webapp.support;

import java.util.Optional;

import javax.ws.rs.core.UriInfo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ar.edu.itba.paw.models.domain.car.Car;
import ar.edu.itba.paw.models.domain.car.CarAvailability;
import ar.edu.itba.paw.models.domain.reservation.Reservation;
import ar.edu.itba.paw.models.domain.user.User;
import ar.edu.itba.paw.services.location.LocationService;
import ar.edu.itba.paw.services.reservation.ReservationAvailabilityService;
import ar.edu.itba.paw.services.user.UserService;
import ar.edu.itba.paw.webapp.dto.rest.ReservationDto;

@Component
public final class ReservationDtoEnricher {

    private final ReservationAvailabilityService reservationAvailabilityService;
    private final UserService userService;
    private final LocationService locationService;

    @Autowired
    public ReservationDtoEnricher(
            final ReservationAvailabilityService reservationAvailabilityService,
            final UserService userService,
            final LocationService locationService) {
        this.reservationAvailabilityService = reservationAvailabilityService;
        this.userService = userService;
        this.locationService = locationService;
    }

    public ReservationDto toDto(final Reservation reservation, final long ownerId, final UriInfo uriInfo) {
        final boolean hasPaymentReceipt = reservation.getPaymentReceiptFileId().isPresent();
        final boolean hasRefundReceipt = reservation.getPaymentRefundReceiptFileId().isPresent();

        String ownerCbu = null;
        final Optional<User> ownerOpt = userService.getUserById(ownerId);
        if (ownerOpt.isPresent()) {
            ownerCbu = ownerOpt.get().getCbu().orElse(null);
        }

        String pickupStreet = null;
        String pickupNumber = null;
        String checkInTime = null;
        String checkOutTime = null;

        final Optional<CarAvailability> pickupAvailability =
                reservationAvailabilityService.findEffectivePickupAvailabilityForReservation(reservation.getId());
        String pickupNeighborhood = null;
        if (pickupAvailability.isPresent()) {
            final CarAvailability av = pickupAvailability.get();
            pickupStreet = av.getStartPointStreet();
            pickupNumber = av.getStartPointNumber().orElse(null);
            checkInTime = av.getCheckInTime().toString();
            checkOutTime = av.getCheckOutTime().toString();
            pickupNeighborhood = av.getNeighborhoodId()
                    .flatMap(locationService::findNeighborhoodById)
                    .map(n -> n.getName())
                    .orElse(null);
        }

        return ReservationDto.builder(reservation, ownerId, uriInfo)
                .hasPaymentReceipt(hasPaymentReceipt)
                .hasRefundReceipt(hasRefundReceipt)
                .ownerCbu(ownerCbu)
                .pickupStreet(pickupStreet)
                .pickupNumber(pickupNumber)
                .pickupNeighborhood(pickupNeighborhood)
                .checkInTime(checkInTime)
                .checkOutTime(checkOutTime)
                .build();
    }

    public Optional<User> resolveCounterparty(final Reservation reservation, final long viewerUserId) {
        final Car car = reservation.getCar();
        if (car == null) {
            return Optional.empty();
        }
        final long ownerId = car.getOwnerId();
        final long riderId = reservation.getRiderId();
        if (viewerUserId == riderId) {
            return userService.getUserById(ownerId);
        }
        if (viewerUserId == ownerId) {
            return userService.getUserById(riderId);
        }
        return Optional.empty();
    }
}
