package ar.edu.itba.paw.persistence.car;

import java.util.List;
import java.util.Optional;

import ar.edu.itba.paw.models.domain.car.Car;
import ar.edu.itba.paw.models.domain.car.CarModel;
import ar.edu.itba.paw.models.dto.Page;

/**
 * Read/write catalog of vehicle models. Models are scoped to a brand and locked to a body type, mirroring
 * {@link ar.edu.itba.paw.models.domain.car.Car.Type}. Models created by the publish-car "Other" flow are kept
 * with {@code validated = false}.
 */
public interface CarModelDao {

    /** All models for a brand, ordered with validated first then alphabetical name. */
    List<CarModel> findByBrandIdOrdered(long brandId);

    /** Only validated models for a brand, alphabetical. */
    List<CarModel> findValidatedByBrandIdOrdered(long brandId);

    /** All models in the catalog ordered by brand id, then validated-first, then name. */
    List<CarModel> findAllOrderedGroupedByBrand();

    /** Single model by id when present. */
    Optional<CarModel> findById(long modelId);

    /** Case-insensitive lookup scoped to a brand for de-duplication when creating "Other" models. */
    Optional<CarModel> findByBrandIdAndNameIgnoreCase(long brandId, String name);

    /** Inserts a new model under the given brand with the given validation flag and body type. */
    CarModel create(long brandId, String name, boolean validated, Car.Type type);

    /** SQL-paginated unvalidated models ordered alphabetically by brand then name. */
    Page<CarModel> findPendingPage(int page, int pageSize);

    /** Removes a model row by id. */
    void deleteById(long modelId);

    /** Returns the number of models (validated or not) belonging to the given brand. */
    int countByBrandId(long brandId);
}
