package ar.edu.itba.paw.services.reservation;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.itba.paw.models.domain.car.CarAvailability;
import ar.edu.itba.paw.persistence.reservation.ReservationAvailabilityDao;

/** Pass-through to {@link ReservationAvailabilityDao}; joins the caller's transaction when one is active. */
@Service
public final class ReservationAvailabilityServiceImpl implements ReservationAvailabilityService {

    private final ReservationAvailabilityDao reservationAvailabilityDao;

    @Autowired
    public ReservationAvailabilityServiceImpl(final ReservationAvailabilityDao reservationAvailabilityDao) {
        this.reservationAvailabilityDao = reservationAvailabilityDao;
    }

    @Override
    @Transactional
    public void insertCoveringAvailabilities(
            final long reservationId, final Collection<Long> availabilityIds) {
        reservationAvailabilityDao.insertCoveringAvailabilities(reservationId, availabilityIds);
    }

    @Override
    @Transactional
    public void deleteCoveringAvailabilities(final long reservationId) {
        reservationAvailabilityDao.deleteCoveringAvailabilities(reservationId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<BigDecimal> sumReservationTotal(final long reservationId) {
        return reservationAvailabilityDao.sumReservationTotal(reservationId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CarAvailability> findEffectivePickupAvailabilityForReservation(final long reservationId) {
        return reservationAvailabilityDao.findEffectivePickupAvailabilityForReservation(reservationId);
    }
}
