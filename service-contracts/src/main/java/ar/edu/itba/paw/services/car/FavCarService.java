package ar.edu.itba.paw.services.car;

import java.util.Collection;
import java.util.Set;

import ar.edu.itba.paw.exception.car.FavoriteValidationException;
import ar.edu.itba.paw.models.dto.car.CarCard;
import ar.edu.itba.paw.models.dto.Page;

/**
 * Read/write operations behind the "Favorite cars" feature. All business rules (e.g. a user
 * cannot favorite their own car, and only ACTIVE / PAUSED cars surface in "Mis favoritos")
 * are enforced here so controllers stay declarative.
 */
public interface FavCarService {

    /** Returns {@code true} when the given car is currently favorited by {@code userId}. */
    boolean isFavorited(long carId, long userId);

    /**
     * Toggles the favorite state for the (car, user) pair: if currently favorited the mark is
     * removed, otherwise a new one is persisted with the current timestamp.
     *
     * @return the new favorited state ({@code true} when the car is favorited after the call).
     * @throws FavoriteValidationException when the car does not exist or {@code userId} owns it.
     */
    boolean toggleFavorite(long carId, long userId);

    /**
     * Idempotently marks {@code carId} as favorited by {@code userId}.
     *
     * @throws FavoriteValidationException when the car does not exist or {@code userId} owns it.
     */
    void addFavorite(long carId, long userId);

    /**
     * Idempotently removes the favorite mark for the (car, user) pair.
     *
     * @throws FavoriteValidationException when the car does not exist or {@code userId} owns it.
     */
    void removeFavorite(long carId, long userId);

    /**
     * Paginated "Mis favoritos" listing for {@code userId}, ordered by favorited-at timestamp
     * (most recent first). Only cars in {@code ACTIVE} or {@code PAUSED} status are returned.
     */
    Page<CarCard> findMyFavorites(long userId, int page, int pageSize);

    /**
     * Returns the subset of {@code carIds} already favorited by {@code userId}. Designed to be
     * called once per page to annotate a card grid with the current heart state.
     */
    Set<Long> filterFavoritedCarIds(long userId, Collection<Long> carIds);
}
