package ar.edu.itba.paw.services.scheduling;

import ar.edu.itba.paw.services.CarService;
import ar.edu.itba.paw.services.EmailService;
import ar.edu.itba.paw.services.ListingAvailabilityService;
import ar.edu.itba.paw.services.ReservationService;
import ar.edu.itba.paw.services.UserService;
import ar.edu.itba.paw.services.util.ListingAddressFormatter;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import ar.edu.itba.paw.models.util.ArsMoneyFormat;
import ar.edu.itba.paw.models.domain.Car;
import ar.edu.itba.paw.models.domain.ListingAvailability;
import ar.edu.itba.paw.models.domain.AvailabilityPeriod;
import ar.edu.itba.paw.models.domain.Reservation;
import ar.edu.itba.paw.models.email.ReservationMailPayload;
import ar.edu.itba.paw.models.domain.User;

/**
 * Daily job: emails riders a reminder the day before pickup for reservations starting tomorrow (wall zone window
 * mapped to UTC for the query). Cron and zone: {@code app.scheduler.reservation-reminder.*}.
 */
@Component
public final class ReservationReminderScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReservationReminderScheduler.class);

    private final ReservationService reservationService;
    private final ListingAvailabilityService listingAvailabilityService;
    private final ListingAddressFormatter listingAddressFormatter;
    private final CarService carService;
    private final UserService userService;
    private final EmailService emailService;

    @Autowired
    public ReservationReminderScheduler(
            final ReservationService reservationService,
            final ListingAvailabilityService listingAvailabilityService,
            final ListingAddressFormatter listingAddressFormatter,
            final CarService carService,
            final UserService userService,
            final EmailService emailService) {
        this.reservationService = reservationService;
        this.listingAvailabilityService = listingAvailabilityService;
        this.listingAddressFormatter = listingAddressFormatter;
        this.carService = carService;
        this.userService = userService;
        this.emailService = emailService;
    }

    @Scheduled(
            cron = "${app.scheduler.reservation-reminder.cron:0 0 9 * * ?}",
            zone = "${app.scheduler.reservation-reminder.zone:America/Argentina/Buenos_Aires}")
    public void sendReservationReminders() {
        final LocalDate tomorrow = LocalDate.now(AvailabilityPeriod.WALL_ZONE).plusDays(1);
        final OffsetDateTime from = tomorrow.atStartOfDay(AvailabilityPeriod.WALL_ZONE).toInstant().atOffset(ZoneOffset.UTC);
        final OffsetDateTime to = tomorrow.plusDays(1).atStartOfDay(AvailabilityPeriod.WALL_ZONE).toInstant().atOffset(ZoneOffset.UTC);
        LOGGER.atInfo()
                .addArgument(tomorrow)
                .addArgument(from)
                .addArgument(to)
                .log("Reservation reminder job: pickup wall date {} (UTC window {} .. {})");
        final var reservations = reservationService.findReminderReservations(from, to);
        LOGGER.atInfo().addArgument(reservations.size()).log("Found {} reservations to send reminders for");

        for (final Reservation reservation : reservations) {
            try {
                final Optional<User> riderOpt = userService.getUserById(reservation.getRiderId());
                if (riderOpt.isEmpty()) {
                    LOGGER.atWarn().addArgument(reservation.getRiderId()).addArgument(reservation.getId()).log("Skipping reservation reminder email: user not found for riderId={} reservationId={}");
                    continue;
                }

                final Car car = reservation.getCar();
                final Optional<User> ownerOpt = Optional.ofNullable(car).map(Car::getOwner);

                if (ownerOpt.isEmpty()) {
                    LOGGER.atWarn().addArgument(reservation.getId()).log("Skipping reservation reminder email: owner not found for reservationId={}");
                    continue;
                }

                final User rider = riderOpt.get();
                final User listingOwner = ownerOpt.get();
                final String vehicleLabel = (car.getBrand() != null ? car.getBrand() : "")
                        + " "
                        + (car.getModel() != null ? car.getModel() : "");
                final Optional<ListingAvailability> mostRecentAv =
                        listingAvailabilityService.findMostRecentByCarId(car.getId());
                final String riderLoc = mostRecentAv
                        .map(a -> listingAddressFormatter.formatRiderReservationHandoverSummary(a, reservation))
                        .orElse(null);
                final String ownerLoc = mostRecentAv
                        .map(listingAddressFormatter::formatOwnerReservationHandoverSummary)
                        .orElse(null);
                final ReservationMailPayload payload = ReservationMailPayload.builder()
                        .recipientEmail(rider.getEmail())
                        .riderFullName(rider.getForename() + " " + rider.getSurname())
                        .reservationId(reservation.getId())
                        .listingId(car.getId())
                        .vehicleLabel(vehicleLabel.trim())
                        .startDate(reservation.getStartDate())
                        .endDate(reservation.getEndDate())
                        .riderHandoverLocation(riderLoc)
                        .ownerHandoverLocation(ownerLoc)
                        .ownerFullName(listingOwner.getForename() + " " + listingOwner.getSurname())
                        .ownerEmail(listingOwner.getEmail())
                        .reservationTotal(ArsMoneyFormat.format(reservation.getTotalPrice()))
                        .riderMailLocale(userService.resolveMailLocale(rider.getId()))
                        .ownerMailLocale(userService.resolveMailLocale(listingOwner.getId()))
                        .build();
                LOGGER.atInfo().addArgument(rider.getEmail()).addArgument(reservation.getId())
                        .log("Queueing reservation reminder email to {} for reservation id={}");
                emailService.sendReservationReminderEmail(payload);
            } catch (final RuntimeException e) {
                LOGGER.atError().setCause(e).addArgument(reservation.getId()).log("Could not send reservation reminder email for reservation id={}");
            }
        }
    }
}
