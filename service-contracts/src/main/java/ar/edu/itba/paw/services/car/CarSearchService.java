package ar.edu.itba.paw.services.car;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import ar.edu.itba.paw.models.domain.Car;
import ar.edu.itba.paw.models.util.search.CarSearchCriteria;
import ar.edu.itba.paw.models.util.search.CarSearchRequest;
import ar.edu.itba.paw.models.util.search.OwnerCarSearchCriteria;

/**
 * Search-criteria construction extracted from the {@link CarService} monolith so that the
 * lifecycle/mutations surface stays focused.
 *
 * The query/browse methods themselves (cheapest, most-recent, search, owner cards, similar)
 * live on {@link CarService} because they read the {@code car} table directly through {@code CarDao};
 * the architectural rule "each ServiceImpl can only call its own DAO" forbids this service from
 * touching {@code CarDao}. This contract therefore only builds criteria and exposes shared
 * timing helpers.
 */
public interface CarSearchService {

    /** See {@link CarService#buildSearchCriteria}. */
    CarSearchCriteria buildSearchCriteria(CarSearchRequest request);

    /** See {@link CarService#buildOwnerCarSearchCriteria(long, List, List, List, BigDecimal, BigDecimal, List, List, String, int, int, String)}. */
    OwnerCarSearchCriteria buildOwnerCarSearchCriteria(
            long ownerId,
            List<Car.Type> category,
            List<Car.Transmission> transmission,
            List<Car.Powertrain> powertrain,
            BigDecimal priceMin,
            BigDecimal priceMax,
            List<Car.Status> carStatus,
            List<String> rating,
            String textQuery,
            int page,
            int pageSize,
            String sort);

    /** See {@link CarService#buildOwnerCarSearchCriteria(long, List, List, List, BigDecimal, BigDecimal, List, List, String, int, int, String, Long)}. */
    OwnerCarSearchCriteria buildOwnerCarSearchCriteria(
            long ownerId,
            List<Car.Type> category,
            List<Car.Transmission> transmission,
            List<Car.Powertrain> powertrain,
            BigDecimal priceMin,
            BigDecimal priceMax,
            List<Car.Status> carStatus,
            List<String> rating,
            String textQuery,
            int page,
            int pageSize,
            String sort,
            Long excludeCarId);

    /**
     * Wall-clock floor below which a freshly bookable car cannot appear in public catalog browses
     * (cheapest, most-recent, or filtered search). Exposed so callers that build their own
     * {@link CarSearchCriteria.Builder} can stay consistent with the public browse contract.
     */
    LocalDate publicBrowseMinBookableWallDate();
}
