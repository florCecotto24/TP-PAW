package ar.edu.itba.paw.scheduling;

import ar.edu.itba.paw.services.email.EmailService;
import ar.edu.itba.paw.services.reservation.ReservationAvailabilityService;
import ar.edu.itba.paw.services.reservation.ReservationLifecycleRowProcessor;
import ar.edu.itba.paw.services.reservation.ReservationService;
import ar.edu.itba.paw.services.user.UserLocaleService;
import ar.edu.itba.paw.services.user.UserService;
import ar.edu.itba.paw.util.CarAvailabilityAddressFormatter;
import ar.edu.itba.paw.util.format.MoneyFormat;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import ar.edu.itba.paw.models.util.time.AppTimezone;
import ar.edu.itba.paw.models.domain.car.Car;
import ar.edu.itba.paw.models.domain.car.CarAvailability;
import ar.edu.itba.paw.models.domain.reservation.Reservation;
import ar.edu.itba.paw.models.email.reservation.ReservationMailPayload;
import ar.edu.itba.paw.models.domain.user.User;

/**
 * Daily job: emails riders a reminder the day before pickup for reservations starting tomorrow (wall zone window
 * mapped to UTC for the query). Cron and zone: {@code app.scheduler.reservation-reminder.*}.
 * Each row is claimed ({@code pickup_reminder_email_sent}) before {@code @Async} mail is queued.
 */
@Component
public final class ReservationReminderScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReservationReminderScheduler.class);

    private final ReservationService reservationService;
    private final ReservationAvailabilityService reservationAvailabilityService;
    private final ReservationLifecycleRowProcessor lifecycleRowProcessor;
    private final CarAvailabilityAddressFormatter carAvailabilityAddressFormatter;
    private final UserLocaleService userLocaleService;
    private final EmailService emailService;
    private final MoneyFormat moneyFormat;

    @Autowired
    public ReservationReminderScheduler(
            final ReservationService reservationService,
            final ReservationAvailabilityService reservationAvailabilityService,
            final ReservationLifecycleRowProcessor lifecycleRowProcessor,
            final CarAvailabilityAddressFormatter carAvailabilityAddressFormatter,
            final UserLocaleService userLocaleService,
            final EmailService emailService,
            final MoneyFormat moneyFormat) {
        this.reservationService = reservationService;
        this.reservationAvailabilityService = reservationAvailabilityService;
        this.lifecycleRowProcessor = lifecycleRowProcessor;
        this.carAvailabilityAddressFormatter = carAvailabilityAddressFormatter;
        this.userLocaleService = userLocaleService;
        this.emailService = emailService;
        this.moneyFormat = moneyFormat;
    }

    @Scheduled(
            cron = "${app.scheduler.reservation-reminder.cron:0 0 9 * * ?}",
            zone = "${app.scheduler.reservation-reminder.zone:${app.scheduler.default-zone}}")
    public void sendReservationReminders() {
        final LocalDate tomorrow = LocalDate.now(AppTimezone.WALL_ZONE).plusDays(1);
        final OffsetDateTime from = tomorrow.atStartOfDay(AppTimezone.WALL_ZONE).toInstant().atOffset(ZoneOffset.UTC);
        final OffsetDateTime to = tomorrow.plusDays(1).atStartOfDay(AppTimezone.WALL_ZONE).toInstant().atOffset(ZoneOffset.UTC);
        LOGGER.atInfo()
                .addArgument(tomorrow)
                .addArgument(from)
                .addArgument(to)
                .log("Reservation reminder job: pickup wall date {} (UTC window {} .. {})");
        final var reservations = reservationService.findReminderReservations(from, to);
        LOGGER.atInfo().addArgument(reservations.size()).log("Found {} reservations to send reminders for");

        int queued = 0;
        for (final Reservation reservation : reservations) {
            try {
                // Rider, car and owner come pre-fetched from findReminderReservations (JOIN FETCH
                // r.rider / r.car / c.owner / cm / cm.brand). Don't issue a SELECT users / SELECT
                // cars per row — the previous version triggered three extra queries per reminder.
                final User rider = reservation.getRider();
                final Car car = reservation.getCar();
                if (rider == null || car == null || car.getOwner() == null) {
                    LOGGER.atWarn().addArgument(reservation.getId())
                            .log("Skipping reservation reminder email: rider/car/owner missing for reservationId={}");
                    continue;
                }
                if (!lifecycleRowProcessor.claimPickupReminder(reservation.getId())) {
                    continue;
                }

                final User listingOwner = car.getOwner();
                final String vehicleLabel = (car.getBrand() != null ? car.getBrand() : "")
                        + " "
                        + (car.getModel() != null ? car.getModel() : "");
                final Optional<CarAvailability> snapshotAv =
                        reservationAvailabilityService.findEffectivePickupAvailabilityForReservation(reservation.getId());
                final String riderLoc = snapshotAv
                        .map(a -> carAvailabilityAddressFormatter.formatRiderReservationHandoverSummary(a, reservation))
                        .orElse(null);
                final String ownerLoc = snapshotAv
                        .map(carAvailabilityAddressFormatter::formatOwnerReservationHandoverSummary)
                        .orElse(null);
                final ReservationMailPayload payload = ReservationMailPayload.builder()
                        .recipientEmail(rider.getEmail())
                        .riderFullName(rider.getForename() + " " + rider.getSurname())
                        .reservationId(reservation.getId())
                        .carId(car.getId())
                        .vehicleLabel(vehicleLabel.trim())
                        .startDate(reservation.getStartDate())
                        .endDate(reservation.getEndDate())
                        .riderHandoverLocation(riderLoc)
                        .ownerHandoverLocation(ownerLoc)
                        .ownerFullName(listingOwner.getForename() + " " + listingOwner.getSurname())
                        .ownerEmail(listingOwner.getEmail())
                        .reservationTotal(moneyFormat.format(reservation.getTotalPrice()))
                        .riderMailLocale(userLocaleService.resolveMailLocaleFor(rider))
                        .ownerMailLocale(userLocaleService.resolveMailLocaleFor(listingOwner))
                        .build();
                LOGGER.atInfo().addArgument(rider.getEmail()).addArgument(reservation.getId())
                        .log("Queueing reservation reminder email to {} for reservation id={}");
                emailService.sendReservationReminderEmail(payload);
                queued++;
            } catch (final RuntimeException e) {
                LOGGER.atError().setCause(e).addArgument(reservation.getId()).log("Could not send reservation reminder email for reservation id={}");
            }
        }
        LOGGER.atInfo().addArgument(queued).log("Reservation reminder job: queued {} email(s)");
    }
}
