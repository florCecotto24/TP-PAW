package ar.edu.itba.paw.services;

import java.time.LocalDate;
import java.util.Locale;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.itba.paw.models.domain.Car;
import ar.edu.itba.paw.models.domain.CarAvailability;
import ar.edu.itba.paw.models.domain.Reservation;
import ar.edu.itba.paw.models.domain.User;
import ar.edu.itba.paw.models.dto.reservation.ReservationConfirmationPageModel;
import ar.edu.itba.paw.models.dto.reservation.ReservationFormPageModel;
import ar.edu.itba.paw.models.util.format.ArsMoneyFormat;
import ar.edu.itba.paw.models.util.time.WallDateTimeDisplayFormat;
import ar.edu.itba.paw.models.util.time.WallDateTimeParsing;
import ar.edu.itba.paw.services.policy.ProfileDocumentUploadPolicy;
import ar.edu.itba.paw.services.util.CarAvailabilityAddressFormatter;

/**
 * Builds the reservation-form and confirmation page models. Moves the orchestration that used
 * to live in {@code ReservationFormController} (CarService + CarAvailabilityService +
 * ReservationService + UserService + ImageService + WallDateTimeUiFormatter + the rider-docs
 * helper + the day-effective handover-location helper) into a single transactional facade.
 *
 * <p>Date display strings are formatted here using {@link WallDateTimeDisplayFormat} so the
 * controller no longer needs a {@code WallDateTimeUiFormatter} of its own.</p>
 */
@Service
public final class ReservationFormViewServiceImpl implements ReservationFormViewService {

    private final CarService carService;
    private final CarAvailabilityService carAvailabilityService;
    private final ReservationService reservationService;
    private final ImageService imageService;
    private final UserService userService;
    private final ProfileDocumentUploadPolicy profileDocumentUploadPolicy;
    private final CarAvailabilityAddressFormatter carAvailabilityAddressFormatter;

    @Autowired
    public ReservationFormViewServiceImpl(
            final CarService carService,
            final CarAvailabilityService carAvailabilityService,
            final ReservationService reservationService,
            final ImageService imageService,
            final UserService userService,
            final ProfileDocumentUploadPolicy profileDocumentUploadPolicy,
            final CarAvailabilityAddressFormatter carAvailabilityAddressFormatter) {
        this.carService = carService;
        this.carAvailabilityService = carAvailabilityService;
        this.reservationService = reservationService;
        this.imageService = imageService;
        this.userService = userService;
        this.profileDocumentUploadPolicy = profileDocumentUploadPolicy;
        this.carAvailabilityAddressFormatter = carAvailabilityAddressFormatter;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ReservationFormPageModel> loadReservationFormPage(
            final long carId,
            final User rider,
            final String fromDateTime,
            final String untilDateTime,
            final String reservationTotal,
            final String carNameOverride,
            final Locale locale) {
        final Optional<Car> carOpt = carService.getCarById(carId);
        if (carOpt.isEmpty()) {
            return Optional.empty();
        }
        final Car car = carOpt.get();
        final String resolvedCarName = carNameOverride != null && !carNameOverride.isBlank()
                ? carNameOverride
                : car.getBrand() + " " + car.getModel();
        final String deliveryLocation = resolveCarHandoverLocation(carId, fromDateTime);
        final User riderRow = userService.getUserById(rider.getId()).orElse(rider);

        return Optional.of(ReservationFormPageModel.builder()
                .car(car)
                .resolvedCarName(resolvedCarName)
                .deliveryLocation(deliveryLocation)
                .riderForename(rider.getForename())
                .riderSurname(rider.getSurname())
                .riderEmail(rider.getEmail())
                .clientReservationTotal(
                        reservationService.normalizeClientReservationTotal(reservationTotal).orElse(null))
                .reservationTotal(reservationService
                        .reservationTotalDisplayByCar(carId, fromDateTime, untilDateTime)
                        .orElse(null))
                .fromDateTimeDisplay(formatDateTimeInput(fromDateTime, locale))
                .untilDateTimeDisplay(formatDateTimeInput(untilDateTime, locale))
                .paymentProofUploadDeadlineHours(reservationService.getConfiguredPaymentProofDeadlineHours())
                .maxReservationBillableDays(reservationService.getConfiguredMaxReservationBillableDays())
                .riderHasBookingDocuments(userService.hasUploadedLicenseAndIdentity(riderRow))
                .riderMissingLicenseDocument(riderRow.getLicenseFileId().isEmpty())
                .riderMissingIdentityDocument(riderRow.getIdentityFileId().isEmpty())
                .uploadMaxProfileDocumentMegabytes(profileDocumentUploadPolicy.getMaxMegabytesRoundedUp())
                .build());
    }

    @Override
    @Transactional(readOnly = true)
    public ReservationConfirmationPageModel buildReservationConfirmation(
            final long carId,
            final User rider,
            final String carName,
            final String fromDateTime,
            final String untilDateTime,
            final Long availabilityId,
            final Reservation reservation,
            final Locale locale) {
        final String confirmLoc = resolveCarHandoverLocation(carId, fromDateTime);
        return ReservationConfirmationPageModel.builder()
                .carName(carName)
                .name(rider.getForename())
                .surname(rider.getSurname())
                .email(rider.getEmail())
                .fromDateTime(fromDateTime)
                .untilDateTime(untilDateTime)
                .deliveryLocation(confirmLoc == null || confirmLoc.isBlank() ? "" : confirmLoc)
                .reservationId(reservation.getId())
                .carId(carId)
                .availabilityId(availabilityId)
                .reservationTotal(ArsMoneyFormat.format(reservation.getTotalPrice()))
                .ownerCbu(userService.findOwnerCbuForCar(carId).orElse(""))
                .fromDateTimeDisplay(formatDateTimeInput(fromDateTime, locale))
                .untilDateTimeDisplay(formatDateTimeInput(untilDateTime, locale))
                .paymentProofUploadDeadlineHours(reservationService.getConfiguredPaymentProofDeadlineHours())
                .maxReservationBillableDays(reservationService.getConfiguredMaxReservationBillableDays())
                .uploadMaxImageBytes(imageService.getMaxImageBytes())
                .uploadMaxImageMegabytes(imageService.getMaxImageMegabytesRoundedUp())
                .build();
    }

    /**
     * Pickup/return address shown on the reservation summary before any payment is uploaded. Uses
     * the rider's chosen pickup date (not "today") to pick the effective availability row, and
     * falls back to the most recent row when the rider has not picked a valid date yet or the
     * chosen date is outside any published window. The address is rendered without the door
     * number — {@link CarAvailabilityAddressFormatter#formatPublicPickupLocation} enforces that policy.
     */
    private String resolveCarHandoverLocation(final long carId, final String fromDateTime) {
        final LocalDate pickupDay = WallDateTimeParsing.parseWallLocalDateTimeToWallDate(fromDateTime);
        Optional<CarAvailability> av = (pickupDay == null)
                ? Optional.empty()
                : carAvailabilityService.findEffectiveForDayByCar(carId, pickupDay);
        if (av.isEmpty()) {
            av = carAvailabilityService.findMostRecentByCarId(carId);
        }
        return av.map(carAvailabilityAddressFormatter::formatPublicPickupLocation).orElse("");
    }

    private static String formatDateTimeInput(final String raw, final Locale locale) {
        return WallDateTimeDisplayFormat.formatClientWallDateTimeInputOrRaw(raw, locale);
    }
}
