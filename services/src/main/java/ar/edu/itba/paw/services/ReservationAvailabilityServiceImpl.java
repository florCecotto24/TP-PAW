package ar.edu.itba.paw.services;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.itba.paw.models.domain.ReservationAvailabilityLink;
import ar.edu.itba.paw.persistence.ReservationAvailabilityDao;

/** Pass-through to {@link ReservationAvailabilityDao}; joins the caller’s transaction when one is active. */
@Service
public final class ReservationAvailabilityServiceImpl implements ReservationAvailabilityService {

    private final ReservationAvailabilityDao reservationAvailabilityDao;

    @Autowired
    public ReservationAvailabilityServiceImpl(final ReservationAvailabilityDao reservationAvailabilityDao) {
        this.reservationAvailabilityDao = reservationAvailabilityDao;
    }

    @Override
    @Transactional
    public void insertLinks(final long reservationId, final List<ReservationAvailabilityLink> links) {
        reservationAvailabilityDao.insertLinks(reservationId, links);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<BigDecimal> sumReservationTotal(final long reservationId) {
        return reservationAvailabilityDao.sumReservationTotal(reservationId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<BigDecimal> quoteTotalFromLinks(final List<ReservationAvailabilityLink> links) {
        return reservationAvailabilityDao.quoteTotalFromLinks(links);
    }
}
