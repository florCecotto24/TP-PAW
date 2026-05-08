package ar.edu.itba.paw.persistence.hibernate;

import java.util.List;
import java.util.Optional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.itba.paw.models.domain.Car;
import ar.edu.itba.paw.models.domain.Listing;
import ar.edu.itba.paw.persistence.CarDao;

@Transactional
@Repository
public class CarHibernateDao implements CarDao {

    @PersistenceContext
    private EntityManager em;

    private final int carCatalogLimit;

    public CarHibernateDao(@Value("${app.listing.car-catalog-limit:8}") final int carCatalogLimit) {
        this.carCatalogLimit = Math.max(1, carCatalogLimit);
    }

    @Override
    public Car createCar(final long ownerId, final String plate, final String brand, final String model,
                         final Car.Type type, final Car.Powertrain powertrain, final Car.Transmission transmission) {
        final Car car = Car.builder()
                .ownerId(ownerId)
                .plate(plate)
                .brand(brand)
                .model(model)
                .type(type)
                .powertrain(powertrain)
                .transmission(transmission)
                .build();
        em.persist(car);
        return car;
    }

    @Override
    public Optional<Car> getCarById(final long id) {
        return Optional.ofNullable(em.find(Car.class, id));
    }

    @Override
    public List<Car> getCheapestCars() {
        return em.createQuery(
                        "SELECT c FROM Car c, Listing l WHERE l.carId = c.id AND l.status = :status ORDER BY l.dayPrice ASC",
                        Car.class)
                .setParameter("status", Listing.Status.ACTIVE)
                .setMaxResults(carCatalogLimit)
                .getResultList();
    }

    @Override
    public List<Car> getMostRecentCars() {
        return em.createQuery(
                        "SELECT c FROM Car c, Listing l WHERE l.carId = c.id AND l.status = :status ORDER BY l.createdAt DESC",
                        Car.class)
                .setParameter("status", Listing.Status.ACTIVE)
                .setMaxResults(carCatalogLimit)
                .getResultList();
    }
}
