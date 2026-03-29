package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.Listing;
import ar.edu.itba.paw.models.Reservation;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.persistence.CarDao;
import ar.edu.itba.paw.persistence.ListingDao;
import ar.edu.itba.paw.persistence.ReservationDao;
import ar.edu.itba.paw.persistence.UserDao;
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
    private final ListingDao listingDao;
    private final CarDao carDao;
    private final UserDao userDao;
    private final EmailService emailService;

    @Autowired
    public ReservationServiceImpl(
            final ReservationDao reservationDao,
            final ListingDao listingDao,
            final CarDao carDao,
            final UserDao userDao,
            final EmailService emailService) {
        this.reservationDao = reservationDao;
        this.listingDao = listingDao;
        this.carDao = carDao;
        this.userDao = userDao;
        this.emailService = emailService;
    }

    @Override
    public Reservation createReservation(
            final long riderId,
            final long listingId,
            final OffsetDateTime startDate,
            final OffsetDateTime endDate,
            final String deliveryLocation) {
        final Reservation created = reservationDao.createReservation(riderId, listingId, startDate, endDate);
        notifyOwnerOfNewReservation(created, riderId);
        notifyRiderWithDetails(created, deliveryLocation);
        return created;
    }

    @Override
    public Optional<Reservation> getReservationById(final long id) {
        return reservationDao.getReservationById(id);
    }

    private void notifyOwnerOfNewReservation(final Reservation reservation, final long riderId) {
        final Optional<String> ownerEmail = resolveListingOwnerEmail(reservation.getListingId());
        final Optional<String> riderEmail = userDao.getUserById(riderId).map(User::getEmail);
        final Optional<String> listingTitle = listingDao.getListingById(reservation.getListingId()).map(Listing::getTitle);
        if (ownerEmail.isEmpty() || riderEmail.isEmpty() || listingTitle.isEmpty()) {
            LOG.warn("No se envía mail al dueño del listing: datos incompletos (owner=" + ownerEmail.isPresent()
                    + ", rider=" + riderEmail.isPresent() + ", título=" + listingTitle.isPresent() + ")");
            return;
        }
        emailService.notifyListingOwnerNewReservation(
                ownerEmail.get(),
                listingTitle.get(),
                reservation.getId(),
                riderEmail.get());
    }

    private void notifyRiderWithDetails(final Reservation reservation, final String deliveryLocation) {
        final Optional<User> rider = userDao.getUserById(reservation.getRiderId());
        final Optional<String> listingTitle = listingDao.getListingById(reservation.getListingId()).map(Listing::getTitle);
        final Optional<User> owner = resolveListingOwner(reservation.getListingId());
        if (rider.isEmpty() || listingTitle.isEmpty() || owner.isEmpty()) {
            LOG.warn("No se envía mail al rider: datos incompletos (rider=" + rider.isPresent()
                    + ", título=" + listingTitle.isPresent() + ", dueño=" + owner.isPresent() + ")");
            return;
        }
        final User o = owner.get();
        final User r = rider.get();
        emailService.notifyRiderReservationDetails(
                r.getEmail(),
                r.getName(),
                reservation.getId(),
                listingTitle.get(),
                reservation.getStartDate(),
                reservation.getEndDate(),
                deliveryLocation,
                o.getName(),
                o.getEmail());
    }

    private Optional<String> resolveListingOwnerEmail(final long listingId) {
        return resolveListingOwner(listingId).map(User::getEmail);
    }

    private Optional<User> resolveListingOwner(final long listingId) {
        return listingDao.getListingById(listingId)
                .flatMap(l -> carDao.getCarById(l.getCarId()))
                .flatMap(c -> userDao.getUserById(c.getOwnerId()));
    }
}
