package ar.edu.itba.paw.services;

import java.util.List;
import java.util.Optional;

import ar.edu.itba.paw.models.domain.Car;
import ar.edu.itba.paw.models.domain.CarModel;

/**
 * Catalog operations for vehicle models. A model belongs to a {@link ar.edu.itba.paw.models.domain.CarBrand}
 * and is locked to a body {@link Car.Type}. Lookup methods are normalized (trim, case-insensitive) here so
 * DAOs only execute parametrized queries.
 */
public interface CarModelService {

    /** All models for a brand, validated-first then alphabetical. */
    List<CarModel> findByBrandIdOrdered(long brandId);

    /** Only validated models for a brand, alphabetical. */
    List<CarModel> findValidatedByBrandIdOrdered(long brandId);

    /** All models in catalog ordered by brand id, validated-first then name. Used by render-once pickers. */
    List<CarModel> findAllOrderedGroupedByBrand();

    /** Catalog row by id when present. */
    Optional<CarModel> findById(long modelId);

    /**
     * Returns the existing model with {@code name} (case-insensitive) for the brand, or creates a new one
     * with {@code validated = false} and the given body {@code type} if no match exists. Empty/blank name
     * returns {@link Optional#empty()}.
     */
    Optional<CarModel> findOrCreateUnvalidated(long brandId, String rawName, Car.Type type);
}
