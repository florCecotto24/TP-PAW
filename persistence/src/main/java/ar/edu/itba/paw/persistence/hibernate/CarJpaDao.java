package ar.edu.itba.paw.persistence.hibernate;

import static ar.edu.itba.paw.persistence.util.JpaQueryUtils.bindParams;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.itba.paw.models.domain.Car;
import ar.edu.itba.paw.models.domain.Listing;
import ar.edu.itba.paw.models.domain.User;
import ar.edu.itba.paw.persistence.CarDao;

@Transactional
@Repository
public class CarJpaDao implements CarDao {

    @PersistenceContext
    private EntityManager em;

    private final int carCatalogLimit;

    public CarJpaDao(@Value("${app.listing.car-catalog-limit:8}") final int carCatalogLimit) {
        this.carCatalogLimit = Math.max(1, carCatalogLimit);
    }

    @Override
    public Car createCar(final long ownerId, final String plate, final String brand, final String model,
                         final Car.Type type, final Car.Powertrain powertrain, final Car.Transmission transmission) {
        final User ownerRef = em.getReference(User.class, ownerId);
        final Car car = Car.builder()
                .owner(ownerRef)
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
    public boolean existsByOwnerAndPlate(final long ownerId, final String plate) {
        final Long count = (Long) em.createQuery(
                        "SELECT COUNT(c) FROM Car c WHERE c.owner.id = :ownerId AND c.plate = :plate")
                .setParameter("ownerId", ownerId)
                .setParameter("plate", plate)
                .getSingleResult();
        return count > 0;
    }

    @Override
    public Optional<Car> getCarById(final long id) {
        return Optional.ofNullable(em.find(Car.class, id));
    }

    @Override
    public List<Car> getCheapestCars() {
        return loadCarsByOrderedIds(loadActiveCarCatalogOrderedNativeIds("l.day_price ASC"));
    }

    @Override
    public List<Car> getMostRecentCars() {
        return loadCarsByOrderedIds(loadActiveCarCatalogOrderedNativeIds("l.created_at DESC"));
    }

    private List<Long> loadActiveCarCatalogOrderedNativeIds(final String orderBySql) {
        final String sql = "SELECT c.id FROM cars c INNER JOIN listings l ON l.car_id = c.id WHERE l.status = '"
                + Listing.Status.ACTIVE.name().toLowerCase() + "' "
                + "ORDER BY " + orderBySql;
        @SuppressWarnings("unchecked")
        final List<Number> raw =
                em.createNativeQuery(sql).setMaxResults(carCatalogLimit).getResultList();
        return new ArrayList<>(raw.stream().map(Number::longValue).collect(Collectors.toCollection(LinkedHashSet::new)));
    }

    private List<Car> loadCarsByOrderedIds(final List<Long> orderedCarIds) {
        if (orderedCarIds.isEmpty()) {
            return List.of();
        }
        final Map<String, Object> qParams = new HashMap<>();
        qParams.put("ids", orderedCarIds);
        final List<Car> cars =
                bindParams(em.createQuery("FROM Car c WHERE c.id IN :ids", Car.class), qParams).getResultList();
        final Map<Long, Car> byId =
                cars.stream().collect(Collectors.toMap(Car::getId, Function.identity(), (a, b) -> a));
        return orderedCarIds.stream().map(byId::get).filter(Objects::nonNull).collect(Collectors.toList());
    }
}
