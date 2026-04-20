package ar.edu.itba.paw.services;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import ar.edu.itba.paw.models.Listing;
import ar.edu.itba.paw.models.AvailabilityPeriod;
import ar.edu.itba.paw.models.Reservation;
import ar.edu.itba.paw.models.ReservationConfirmationPayload;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.persistence.ReservationDao;

@Component
public class ReservationReminderScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReservationReminderScheduler.class);

    private final ReservationDao reservationDao;
    private final ListingService listingService;
    private final UserService userService;
    private final EmailService emailService;

    @Autowired
    public ReservationReminderScheduler(
            final ReservationDao reservationDao,
            final ListingService listingService,
            final UserService userService,
            final EmailService emailService) {
        this.reservationDao = reservationDao;
        this.listingService = listingService;
        this.userService = userService;
        this.emailService = emailService;
    }

    @Scheduled(cron = "0 0 9 * * ?")
    public void sendReservationReminders() {
        final LocalDate tomorrow = LocalDate.now(AvailabilityPeriod.WALL_ZONE).plusDays(1);
        final OffsetDateTime from = tomorrow.atStartOfDay(AvailabilityPeriod.WALL_ZONE).toInstant().atOffset(ZoneOffset.UTC);
        final OffsetDateTime to = tomorrow.plusDays(1).atStartOfDay(AvailabilityPeriod.WALL_ZONE).toInstant().atOffset(ZoneOffset.UTC);
        final var reservations = reservationDao.getReminderReservations(from, to);
        LOGGER.atInfo().log("Found " + reservations.size() + " reservations to send reminders for");

        for (final Reservation reservation : reservations) {
            try {
                final Optional<User> riderOpt = userService.getUserById(reservation.getRiderId());
                final Optional<Listing> listingOpt = listingService.getListingById(reservation.getListingId());
                if (riderOpt.isEmpty()) {
                    LOGGER.atWarn().log("Skipping reservation reminder email: user not found for riderId=" + reservation.getRiderId()
                            + " reservationId=" + reservation.getId());
                    continue;
                }
                if (listingOpt.isEmpty()) {
                    LOGGER.atWarn().log("Skipping reservation reminder email: listing not found for listingId=" + reservation.getListingId()
                            + " reservationId=" + reservation.getId());
                    continue;
                }

                final User rider = riderOpt.get();
                final Listing listing = listingOpt.get();
                final ReservationConfirmationPayload payload = new ReservationConfirmationPayload(
                        rider.getEmail(),
                        rider.getForename() + " " + rider.getSurname(),
                        reservation.getId(),
                        reservation.getListingId(),
                        listing.getTitle(),
                        reservation.getStartDate(),
                        reservation.getEndDate(),
                        listing.getStartPoint(),
                        null,
                        null,
                        reservation.getTotalPrice().toString(),
                        resolveMailMessageLocale());
                LOGGER.atInfo().log("Queueing reservation reminder email to " + rider.getEmail()
                        + " for reservation id=" + reservation.getId());
                emailService.sendReservationReminderEmail(payload);
            } catch (final Exception e) {
                LOGGER.atError().log("Could not send reservation reminder email for reservation id=" + reservation.getId(), e);
            }
        }
    }

    private static Locale resolveMailMessageLocale() {
        final Locale locale = LocaleContextHolder.getLocale();
        if (locale != null && "es".equalsIgnoreCase(locale.getLanguage())) {
            return Locale.forLanguageTag("es");
        }
        return Locale.ENGLISH;
    }
}

