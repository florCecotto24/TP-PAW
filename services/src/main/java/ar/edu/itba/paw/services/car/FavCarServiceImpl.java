package ar.edu.itba.paw.services.car;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.itba.paw.exception.MessageKeys;
import ar.edu.itba.paw.exception.car.FavoriteValidationException;
import ar.edu.itba.paw.models.domain.Car;
import ar.edu.itba.paw.models.dto.car.CarCard;
import ar.edu.itba.paw.models.dto.Page;
import ar.edu.itba.paw.persistence.FavCarDao;

/**
 * Business rules of the favorite-cars feature: ownership checks, allowed status filter, and
 * the toggle semantics consumed by controllers. Reads/writes go through {@link FavCarDao}; the
 * owner check delegates to {@link CarService} so we don't bypass the car aggregate.
 */
@Service
public final class FavCarServiceImpl implements FavCarService {

    /** Cars in any other status are hidden from "Mis favoritos" (e.g. deactivated, lacking docs). */
    private static final Set<Car.Status> FAVORITES_VISIBLE_STATUSES =
            EnumSet.of(Car.Status.ACTIVE, Car.Status.PAUSED);

    private final FavCarDao favCarDao;
    private final CarService carService;

    @Autowired
    public FavCarServiceImpl(final FavCarDao favCarDao, final CarService carService) {
        this.favCarDao = favCarDao;
        this.carService = carService;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isFavorited(final long carId, final long userId) {
        return favCarDao.isFavorited(carId, userId);
    }

    @Override
    @Transactional
    public boolean toggleFavorite(final long carId, final long userId) {
        final Car car = carService.getCarById(carId)
                .orElseThrow(() -> new FavoriteValidationException(MessageKeys.FAV_CAR_NOT_FOUND));
        if (car.getOwner().getId() == userId) {
            throw new FavoriteValidationException(MessageKeys.FAV_CAR_CANNOT_FAV_OWN);
        }
        if (favCarDao.isFavorited(carId, userId)) {
            favCarDao.removeFavorite(carId, userId);
            return false;
        }
        favCarDao.addFavorite(carId, userId, OffsetDateTime.now());
        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CarCard> findMyFavorites(final long userId, final int page, final int pageSize) {
        return favCarDao.findFavoriteCarCards(userId, FAVORITES_VISIBLE_STATUSES, page, pageSize);
    }

    @Override
    @Transactional(readOnly = true)
    public Set<Long> filterFavoritedCarIds(final long userId, final Collection<Long> carIds) {
        return favCarDao.filterFavoritedCarIds(userId, carIds);
    }
}
