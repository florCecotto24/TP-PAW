package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.Listing;
import ar.edu.itba.paw.models.Reservation;
import ar.edu.itba.paw.models.ReservationConfirmationPayload;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.persistence.ReservationDao;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Optional;

@Service
public class ReservationServiceImpl implements ReservationService {

    private static final Log LOG = LogFactory.getLog(ReservationServiceImpl.class);

    private final ReservationDao reservationDao;
    private final UserService userService;
    private final ListingService listingService;
    private final EmailService emailService;

    @Autowired
    public ReservationServiceImpl(
            final ReservationDao reservationDao,
            final UserService userService,
            final ListingService listingService,
            final EmailService emailService) {
        this.reservationDao = reservationDao;
        this.userService = userService;
        this.listingService = listingService;
        this.emailService = emailService;
    }

    @Override
    public Reservation createReservation(
            final long riderId,
            final long listingId,
            final OffsetDateTime startDate,
            final OffsetDateTime endDate,
            final Reservation.Status status) {
        if (reservationDao.hasActiveOverlap(listingId, startDate, endDate)) {
            throw new ReservationConflictException("The selected dates are no longer available for this listing.");
        }
        final String deliveryLocation = listingService.getListingById(listingId)
                .map(Listing::getStartPoint)
                .orElse(null);
        final Reservation reservation =
                reservationDao.createReservation(riderId, listingId, startDate, endDate, status);
        enqueueReservationConfirmationEmail(riderId, listingId, reservation, deliveryLocation);
        return reservation;
    }

    private void enqueueReservationConfirmationEmail(
            final long riderId,
            final long listingId,
            final Reservation reservation,
            final String deliveryLocation) {
        try {
            final Optional<User> riderOpt = userService.getUserById(riderId);
            final Optional<User> listingOwnerOpt = userService.getListingOwner(listingId);
            final Optional<Listing> listingOpt = listingService.getListingById(listingId);
            if (!riderOpt.isPresent()) {
                LOG.warn("Skipping reservation confirmation email: user not found for riderId=" + riderId
                        + " reservationId=" + reservation.getId());
                return;
            }
            if (!listingOpt.isPresent()) {
                LOG.warn("Skipping reservation confirmation email: listing not found for listingId=" + listingId
                        + " reservationId=" + reservation.getId());
                return;
            }
            final User rider = riderOpt.get();
            final User listingOwner = listingOwnerOpt.orElseThrow( () -> new ReservationConflictException("Listing owner not found for listingId=" + listingId));
            final Listing listing = listingOpt.get();
            final String vehicleLabel = listing.getTitle();
            final String riderFullName = rider.getForename() + " " + rider.getSurname();
            final String trimmedDelivery =
                    deliveryLocation == null || deliveryLocation.isBlank() ? null : deliveryLocation.trim();
            final ReservationConfirmationPayload payload = new ReservationConfirmationPayload(
                    rider.getEmail(),
                    riderFullName,
                    reservation.getId(),
                    listingId,
                    vehicleLabel,
                    reservation.getStartDate(),
                    reservation.getEndDate(),
                    trimmedDelivery,
                    listingOwner.getForename() + " " + listingOwner.getSurname(),
                    listingOwner.getEmail());
            LOG.info("Queueing reservation confirmation email to " + rider.getEmail()
                    + " for reservation id=" + reservation.getId());
            emailService.sendReservationConfirmationEmail(payload);
        } catch (final Exception e) {
            LOG.error("Could not enqueue reservation confirmation email for reservation id=" + reservation.getId(), e);
        }
    }

    @Override
    public Optional<Reservation> getReservationById(final long id) {
        return reservationDao.getReservationById(id);
    }
}
