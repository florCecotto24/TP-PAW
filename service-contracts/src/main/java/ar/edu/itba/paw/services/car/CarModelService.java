package ar.edu.itba.paw.services.car;

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

    // -----------------------------------------------------------------------------------------------------------
    // Admin-orchestrated operations on model rows.
    //
    // These methods exist so that {@link AdminService} can mutate model state without bypassing the layering rule
    // "each service may only call its own DAO". The calling {@link AdminService} owns the surrounding flow
    // (cascading car-row cleanup, notification emails, etc.).
    // -----------------------------------------------------------------------------------------------------------

    /** Models awaiting admin validation. */
    List<CarModel> findPendingOrdered();

    /** Number of models attached to a brand. Used by admin reject-catalog flow to decide whether the brand
     *  should also be removed. */
    int countByBrandId(long brandId);

    /** Admin-only: sets {@code validated = true} on the model row. No-op when model is missing. */
    void markAsValidated(long modelId);

    /** Admin-only: removes the model. The caller is responsible for breaking dangling references on car rows. */
    void deleteById(long modelId);
}
