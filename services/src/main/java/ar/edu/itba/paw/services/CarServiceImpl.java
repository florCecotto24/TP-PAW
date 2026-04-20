package ar.edu.itba.paw.services;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.itba.paw.models.Car;
import ar.edu.itba.paw.persistence.CarDao;

@Service
public class CarServiceImpl implements CarService {

    private final CarDao carDao;

    @Autowired
    public CarServiceImpl(final CarDao carDao) {
        this.carDao = carDao;
    }

    @Override
    @Transactional
    public Car createCar(
            final long ownerId,
            final String plate,
            final String brand,
            final String model,
            final Car.Type type,
            final Car.Powertrain powertrain,
            final Car.Transmission transmission) {
        return carDao.createCar(ownerId, plate, brand, model, type, powertrain, transmission);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Car> getCarById(final long id) {
        return carDao.getCarById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Car> getCheapestCars() {
        return carDao.getCheapestCars();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Car> getMostRecentCars() {
        return carDao.getMostRecentCars();
    }
}
