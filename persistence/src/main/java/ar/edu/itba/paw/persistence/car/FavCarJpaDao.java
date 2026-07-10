package ar.edu.itba.paw.persistence.car;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.itba.paw.models.domain.car.Car;
import ar.edu.itba.paw.models.domain.car.CarModel;
import ar.edu.itba.paw.models.domain.car.FavCar;
import ar.edu.itba.paw.models.domain.car.FavCarId;
import ar.edu.itba.paw.models.domain.user.User;
import ar.edu.itba.paw.models.dto.Page;
import ar.edu.itba.paw.models.dto.car.CarCard;
import ar.edu.itba.paw.persistence.car.FavCarDao;

@Transactional(readOnly = true)
@Repository
public class FavCarJpaDao implements FavCarDao {

    @PersistenceContext
    private EntityManager em;

    /**
     * Cross-aggregate DAOs intentionally injected to assemble the favourites {@code CarCard}
     * page in three batched queries (favourite id-pagination, JPQL {@code JOIN FETCH} for catalog
     * data, plus the cover-image and min-price batch lookups). The N+1 rule in {@code AGENTS.md}
     * (with {@code loadReservationCardsByIdNativeQuery} as the reference example) takes priority
     * over the service-layer aggregation rule for these card composers; the equivalent injections
     * exist in {@link CarJpaDao} and
     * {@link ar.edu.itba.paw.persistence.reservation.ReservationJpaDao}.
     */
    private final CarPictureDao carPictureDao;
    private final CarAvailabilityDao carAvailabilityDao;

    @Autowired
    public FavCarJpaDao(final CarPictureDao carPictureDao,
                        final CarAvailabilityDao carAvailabilityDao) {
        this.carPictureDao = carPictureDao;
        this.carAvailabilityDao = carAvailabilityDao;
    }

    @Override
    public boolean isFavorited(final long carId, final long userId) {
        return em.find(FavCar.class, new FavCarId(carId, userId)) != null;
    }

    @Override
    @Transactional
    public void addFavorite(final long carId, final long userId, final OffsetDateTime favoritedAt) {
        final Car carRef = em.getReference(Car.class, carId);
        final User userRef = em.getReference(User.class, userId);
        em.persist(new FavCar(carRef, userRef, favoritedAt));
    }

    @Override
    @Transactional
    public void removeFavorite(final long carId, final long userId) {
        final FavCar existing = em.find(FavCar.class, new FavCarId(carId, userId));
        if (existing != null) {
            em.remove(existing);
        }
    }

    @Override
    public Page<CarCard> findFavoriteCarCards(
            final long userId,
            final Collection<Car.Status> allowedStatuses,
            final int page,
            final int pageSize) {
        final int safePage = Math.max(0, page);
        final int safePageSize = Math.max(1, pageSize);
        if (allowedStatuses == null || allowedStatuses.isEmpty()) {
            return new Page<>(List.of(), safePage, safePageSize, 0L);
        }
        final long total = countFavoriteCars(userId, allowedStatuses);
        if (total == 0L) {
            return new Page<>(List.of(), safePage, safePageSize, 0L);
        }
        final List<CarCard> content = loadFavoriteCarCardsWindow(
                userId, allowedStatuses, safePage * safePageSize, safePageSize);
        return new Page<>(content, safePage, safePageSize, total);
    }

    /**
     * COUNT in JPQL navigating the {@link FavCar} entity graph (not the raw table). Used as the
     * "+1" of the 1+1 pagination pattern that {@link #loadFavoriteCarCardsWindow} closes.
     */
    private long countFavoriteCars(final long userId, final Collection<Car.Status> allowedStatuses) {
        final Number count = (Number) em.createQuery(
                "SELECT COUNT(fc) FROM FavCar fc "
                + "WHERE fc.id.userId = :userId "
                + "AND fc.car.status IN :allowedStatuses "
                + "AND fc.car.owner.blocked = FALSE")
                .setParameter("userId", userId)
                .setParameter("allowedStatuses", allowedStatuses)
                .getSingleResult();
        return count == null ? 0L : count.longValue();
    }

    /**
     * Real "1+1" pattern:
     *   1. NATIVE query to fetch only the favourited car IDs ordered by favorited_at (the only
     *      reason native SQL is needed here is the dialect-specific LIMIT/OFFSET pagination).
     *   2. JPQL with JOIN FETCH hydrates the Car entities (and their CarModel + CarBrand).
     *   3. Cover image and "from" day price are asked to the DAOs that own those entities,
     *      so this DAO never touches car_pictures or car_availability directly.
     */
    private List<CarCard> loadFavoriteCarCardsWindow(
            final long userId,
            final Collection<Car.Status> allowedStatuses,
            final int offset,
            final int limit) {
        final List<String> statusDbValues = toStatusDbValues(allowedStatuses);
        final String idSql =
                "SELECT c.id FROM fav_cars fc "
                + "INNER JOIN cars c ON c.id = fc.car_id "
                + "INNER JOIN users u ON u.id = c.owner_id AND u.blocked = FALSE "
                + "WHERE fc.user_id = :userId "
                + "AND c.status IN (:allowedStatuses) "
                + "ORDER BY fc.favorited_at DESC, c.id DESC "
                + "LIMIT :limit OFFSET :offset";
        final Query q = em.createNativeQuery(idSql)
                .setParameter("userId", userId)
                .setParameter("allowedStatuses", statusDbValues)
                .setParameter("limit", limit)
                .setParameter("offset", offset);
        @SuppressWarnings("unchecked")
        final List<Number> rawIds = q.getResultList();
        if (rawIds.isEmpty()) {
            return List.of();
        }
        final List<Long> orderedIds = new ArrayList<>(rawIds.size());
        for (final Number n : rawIds) {
            orderedIds.add(n.longValue());
        }

        final List<Car> cars = em.createQuery(
                        "FROM Car c "
                                + "JOIN FETCH c.owner "
                                + "JOIN FETCH c.carModel cm "
                                + "JOIN FETCH cm.brand "
                                + "WHERE c.id IN :ids",
                        Car.class)
                .setParameter("ids", orderedIds)
                .getResultList();
        final Map<Long, Car> byId = new HashMap<>(cars.size());
        for (final Car c : cars) {
            byId.put(c.getId(), c);
        }
        final Set<Long> carIdSet = new LinkedHashSet<>(orderedIds);
        final Map<Long, Long> coverByCar = carPictureDao.findCoverImageIdsByCarIds(carIdSet);
        final Map<Long, BigDecimal> priceByCar = carAvailabilityDao.findMinOfferedDayPriceByCarIds(carIdSet);

        final List<CarCard> result = new ArrayList<>(orderedIds.size());
        for (final Long id : orderedIds) {
            final Car c = byId.get(id);
            if (c == null) {
                continue;
            }
            final CarModel model = c.getCarModel().orElse(null);
            final boolean modelValidated = model != null && model.isValidated();
            final String brand = model == null || model.getBrand() == null ? null : model.getBrand().getName();
            final String modelName = model == null ? null : model.getName();
            result.add(CarCard.builder()
                    .carId(c.getId())
                    .brand(brand)
                    .model(modelName)
                    .imageId(coverByCar.getOrDefault(c.getId(), 0L))
                    .dayPrice(priceByCar.get(c.getId()))
                    .status(c.getStatus())
                    .ratingAvg(c.getRatingAvg().orElse(null))
                    .modelPendingValidation(!modelValidated)
                    .minimumRentalDays(c.getMinimumRentalDays())
                    .ownerId(c.getOwnerId())
                    .build());
        }
        return result;
    }

    @Override
    public Set<Long> filterFavoritedCarIds(final long userId, final Collection<Long> carIds) {
        if (carIds == null || carIds.isEmpty()) {
            return Set.of();
        }
        @SuppressWarnings("unchecked")
        final List<Number> rows = em.createQuery(
                "SELECT fc.id.carId FROM FavCar fc "
                + "WHERE fc.id.userId = :userId AND fc.id.carId IN :carIds")
                .setParameter("userId", userId)
                .setParameter("carIds", carIds)
                .getResultList();
        final Set<Long> result = new HashSet<>(rows.size());
        for (final Number n : rows) {
            result.add(n.longValue());
        }
        return result;
    }

    private static List<String> toStatusDbValues(final Collection<Car.Status> statuses) {
        // Car.StatusConverter stores enum names as lowercase; the IN clause must match that format.
        return statuses.stream().map(s -> s.name().toLowerCase()).collect(Collectors.toList());
    }
}
