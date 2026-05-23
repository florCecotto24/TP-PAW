package ar.edu.itba.paw.services;

import java.util.List;
import java.util.Optional;

import ar.edu.itba.paw.models.domain.CarBrand;

/**
 * Catalog operations for vehicle brands. Lookup methods are normalized at this layer (trim, case-insensitive)
 * so DAOs only execute parametrized queries.
 */
public interface CarBrandService {

    /** All brands ordered validated-first, then alphabetical. Used by publish-car pickers. */
    List<CarBrand> findAllOrdered();

    /** Only validated brands, alphabetical. */
    List<CarBrand> findValidatedOrdered();

    /** Catalog row by id when present. */
    Optional<CarBrand> findById(long brandId);

    /**
     * Returns the existing brand whose name matches {@code rawName} case-insensitively, or creates a new
     * one with {@code validated = false} if no match exists. Empty/blank input returns {@link Optional#empty()}.
     */
    Optional<CarBrand> findOrCreateUnvalidated(String rawName);
}
