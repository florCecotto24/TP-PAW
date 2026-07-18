package ar.edu.itba.paw.services.car;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.itba.paw.models.domain.car.Car;
import ar.edu.itba.paw.persistence.car.CarDao;

/**
 * Per-row {@code REQUIRES_NEW} helper for the exhaustion sweep. Extracted so the Spring proxy
 * honours a new transaction per car (self-invocation on {@link CarServiceImpl} would not).
 */
@Component
public class CarExhaustionRowProcessor {

    private final CarDao carDao;

    @Autowired
    public CarExhaustionRowProcessor(final CarDao carDao) {
        this.carDao = carDao;
    }

    /**
     * Pauses the car iff it is still {@link Car.Status#ACTIVE} under a write lock.
     *
     * @return {@code true} when the status flipped to {@link Car.Status#PAUSED}
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean pauseIfStillActive(final long carId) {
        carDao.lockForReservationWrite(carId);
        return carDao.updateCarStatusIfCurrent(carId, Car.Status.PAUSED, Car.Status.ACTIVE);
    }
}
