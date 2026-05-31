package ar.edu.itba.paw.persistence;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.itba.paw.models.domain.Car;
import ar.edu.itba.paw.models.domain.FavCar;
import ar.edu.itba.paw.models.domain.FavCarId;
import ar.edu.itba.paw.models.domain.User;
import ar.edu.itba.paw.models.dto.CarCard;
import ar.edu.itba.paw.persistence.FavCarDao;

@Transactional(readOnly = true)
@Repository
public class FavCarJpaDao implements FavCarDao {

    @PersistenceContext
    private EntityManager em;

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
    public List<CarCard> findFavoriteCarCardsWindow(
            final long userId,
            final Collection<Car.Status> allowedStatuses,
            final int offset,
            final int limit) {
        if (allowedStatuses == null || allowedStatuses.isEmpty() || limit <= 0) {
            return List.of();
        }
        // Single native query that joins fav_cars with the catalog tables and computes the cover
        // image and minimum day price via correlated subqueries. Pagination is enforced at SQL
        // level via LIMIT/OFFSET and ordering matches the feature spec (most recent first).
        final String sql =
                "SELECT c.id, cb.name AS brand, cm.name AS model, "
                + "(SELECT cp.image_id FROM car_pictures cp "
                + "  WHERE cp.car_id = c.id AND cp.image_id IS NOT NULL "
                + "  ORDER BY cp.display_order ASC, cp.id ASC LIMIT 1) AS image_id, "
                + "(SELECT MIN(la.day_price) FROM listing_availability la "
                + "  WHERE la.car_id = c.id AND la.kind = 'offered') AS min_price, "
                + "c.status, c.rating_avg, cm.validated AS model_validated, "
                + "c.minimum_rental_days, c.owner_id "
                + "FROM fav_cars fc "
                + "INNER JOIN cars c ON c.id = fc.car_id "
                + "INNER JOIN users u ON u.id = c.owner_id AND u.blocked = FALSE "
                + "INNER JOIN car_models cm ON cm.id = c.model_id "
                + "INNER JOIN car_brands cb ON cb.id = cm.brand_id "
                + "WHERE fc.user_id = :userId "
                + "AND c.status IN (:allowedStatuses) "
                + "ORDER BY fc.favorited_at DESC, c.id DESC "
                + "LIMIT :limit OFFSET :offset";
        final Query q = em.createNativeQuery(sql)
                .setParameter("userId", userId)
                .setParameter("allowedStatuses", toStatusDbValues(allowedStatuses))
                .setParameter("limit", limit)
                .setParameter("offset", offset);
        @SuppressWarnings("unchecked")
        final List<Object[]> rows = q.getResultList();
        final List<CarCard> cards = new ArrayList<>(rows.size());
        for (final Object[] row : rows) {
            final boolean modelValidated = row[7] != null && (Boolean) row[7];
            cards.add(CarCard.builder()
                    .carId(((Number) row[0]).longValue())
                    .brand((String) row[1])
                    .model((String) row[2])
                    .imageId(row[3] == null ? 0L : ((Number) row[3]).longValue())
                    .dayPrice(row[4] == null ? null : toBigDecimal(row[4]))
                    .status(Car.Status.valueOf(((String) row[5]).toUpperCase()))
                    .ratingAvg(row[6] == null ? null : toBigDecimal(row[6]))
                    .modelPendingValidation(!modelValidated)
                    .minimumRentalDays(row[8] == null ? 1 : ((Number) row[8]).intValue())
                    .ownerId(row[9] == null ? null : ((Number) row[9]).longValue())
                    .build());
        }
        return cards;
    }

    @Override
    public long countFavoriteCars(final long userId, final Collection<Car.Status> allowedStatuses) {
        if (allowedStatuses == null || allowedStatuses.isEmpty()) {
            return 0L;
        }
        final String sql =
                "SELECT COUNT(*) FROM fav_cars fc "
                + "INNER JOIN cars c ON c.id = fc.car_id "
                + "INNER JOIN users u ON u.id = c.owner_id AND u.blocked = FALSE "
                + "WHERE fc.user_id = :userId AND c.status IN (:allowedStatuses)";
        final Object result = em.createNativeQuery(sql)
                .setParameter("userId", userId)
                .setParameter("allowedStatuses", toStatusDbValues(allowedStatuses))
                .getSingleResult();
        return result == null ? 0L : ((Number) result).longValue();
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

    private static BigDecimal toBigDecimal(final Object value) {
        if (value instanceof BigDecimal bigDecimalValue) {
            return bigDecimalValue;
        }
        if (value instanceof Number numberValue) {
            return BigDecimal.valueOf(numberValue.doubleValue());
        }
        if (value == null) {
            return null;
        }
        return new BigDecimal(value.toString());
    }
}
