package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.Reservation;
import ar.edu.itba.paw.persistence.ReservationDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Optional;

@Service
public class ReservationServiceImpl implements ReservationService {

    private final ReservationDao reservationDao;

    @Autowired
    public ReservationServiceImpl(final ReservationDao reservationDao) {
        this.reservationDao = reservationDao;
    }

    @Override
    public Reservation createReservation( final long riderId, final long listingId,  final OffsetDateTime startDate,  final OffsetDateTime endDate,  final Reservation.Status status) {
        if (reservationDao.hasActiveOverlap(listingId, startDate, endDate)) {
            throw new ReservationConflictException("The selected dates are no longer available for this listing.");
        }
        return reservationDao.createReservation(riderId, listingId, startDate, endDate, status);
    }

    @Override
    public Optional<Reservation> getReservationById(final long id) {
        return reservationDao.getReservationById(id);
    }
}
