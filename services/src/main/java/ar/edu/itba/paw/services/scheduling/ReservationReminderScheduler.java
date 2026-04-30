package ar.edu.itba.paw.services.scheduling;

import ar.edu.itba.paw.services.EmailService;
import ar.edu.itba.paw.services.ListingService;
import ar.edu.itba.paw.services.UserService;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import ar.edu.itba.paw.models.domain.Listing;
import ar.edu.itba.paw.models.domain.AvailabilityPeriod;
import ar.edu.itba.paw.models.domain.Reservation;
import ar.edu.itba.paw.models.email.ReservationConfirmationEmailPayload;
import ar.edu.itba.paw.models.domain.User;
import ar.edu.itba.paw.persistence.ReservationDao;

@Component
public final class ReservationReminderScheduler {

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
        LOGGER.atInfo().addArgument(reservations.size()).log("Found {} reservations to send reminders for");

        for (final Reservation reservation : reservations) {
            try {
                final Optional<User> riderOpt = userService.getUserById(reservation.getRiderId());
                final Optional<Listing> listingOpt = listingService.getListingById(reservation.getListingId());
                if (riderOpt.isEmpty()) {
                    LOGGER.atWarn().addArgument(reservation.getRiderId()).addArgument(reservation.getId()).log("Skipping reservation reminder email: user not found for riderId={} reservationId={}");
                    continue;
                }
                if (listingOpt.isEmpty()) {
                    LOGGER.atWarn().addArgument(reservation.getListingId()).addArgument(reservation.getId()).log("Skipping reservation reminder email: listing not found for listingId={} reservationId={}");
                    continue;
                }

                final User rider = riderOpt.get();
                final Listing listing = listingOpt.get();
                final Optional<User> ownerOpt = userService.getListingOwner(reservation.getListingId());
                if (ownerOpt.isEmpty()) {
                    LOGGER.atWarn().addArgument(reservation.getListingId()).addArgument(reservation.getId()).log("Skipping reservation reminder email: listing owner not found for listingId={} reservationId={}");
                    continue;
                }
                final User listingOwner = ownerOpt.get();
                final String riderLoc = listingService.formatRiderReservationHandoverSummary(listing, reservation);
                final String ownerLoc = listingService.formatOwnerReservationHandoverSummary(listing);
                final ReservationConfirmationEmailPayload payload = ReservationConfirmationEmailPayload.builder()
                        .recipientEmail(rider.getEmail())
                        .riderFullName(rider.getForename() + " " + rider.getSurname())
                        .reservationId(reservation.getId())
                        .listingId(reservation.getListingId())
                        .vehicleLabel(listing.getTitle())
                        .startDate(reservation.getStartDate())
                        .endDate(reservation.getEndDate())
                        .riderHandoverLocation(riderLoc)
                        .ownerHandoverLocation(ownerLoc)
                        .ownerFullName(listingOwner.getForename() + " " + listingOwner.getSurname())
                        .ownerEmail(listingOwner.getEmail())
                        .reservationTotal(reservation.getTotalPrice().toString())
                        .riderMailLocale(userService.resolveMailLocale(rider.getId()))
                        .ownerMailLocale(userService.resolveMailLocale(listingOwner.getId()))
                        .build();
                LOGGER.atInfo().addArgument(rider.getEmail()).addArgument(reservation.getId())
                        .log("Queueing reservation reminder email to {} for reservation id={}");
                emailService.sendReservationReminderEmail(payload);
            } catch (final Exception e) {
                LOGGER.atError().setCause(e).addArgument(reservation.getId()).log("Could not send reservation reminder email for reservation id={}");
            }
        }
    }
}

