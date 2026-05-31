package ar.edu.itba.paw.persistence;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Set;

import ar.edu.itba.paw.models.domain.Car;
import ar.edu.itba.paw.models.dto.car.CarCard;
import ar.edu.itba.paw.models.dto.Page;

/**
 * Persistence operations backing the "Favorite cars" feature. Every method is read- or write-only:
 * higher-level rules (e.g. "a user cannot favorite their own car") live in the service layer.
 */
public interface FavCarDao {

    /** Returns {@code true} when the (car, user) pair is present in {@code fav_cars}. */
    boolean isFavorited(long carId, long userId);

    /**
     * Persists a new favorite mark. Pre-condition: the pair does not exist yet (the service
     * checks via {@link #isFavorited(long, long)} as part of its toggle flow).
     */
    void addFavorite(long carId, long userId, OffsetDateTime favoritedAt);

    /** Removes the (car, user) favorite mark, if it exists. No-op when the pair is absent. */
    void removeFavorite(long carId, long userId);

    /**
     * Returns the {@link Page} of car cards favorited by {@code userId}, ordered by
     * {@code favorited_at DESC}. Only cars whose {@link Car.Status} belongs to
     * {@code allowedStatuses} are returned. Pagination (offset/limit) and total count are
     * computed inside the DAO so callers do not have to compose them.
     */
    Page<CarCard> findFavoriteCarCards(
            long userId, Collection<Car.Status> allowedStatuses, int page, int pageSize);

    /**
     * Returns the subset of {@code carIds} already favorited by {@code userId}. Used to annotate
     * card grids (search, home, etc.) with the current heart state in a single query.
     */
    Set<Long> filterFavoritedCarIds(long userId, Collection<Long> carIds);
}
