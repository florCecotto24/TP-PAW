package ar.edu.itba.paw.services.car;

import java.util.List;
import java.util.Optional;

import ar.edu.itba.paw.models.domain.CarBrand;

import ar.edu.itba.paw.services.user.AdminService;
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

    // -----------------------------------------------------------------------------------------------------------
    // Admin-orchestrated operations on brand rows.
    //
    // These methods exist so that {@link AdminService} can mutate brand state without bypassing the layering rule
    // "each service may only call its own DAO". The calling {@link AdminService} owns the surrounding flow
    // (model dependency checks, notification emails, etc.).
    // -----------------------------------------------------------------------------------------------------------

    /** Brands awaiting admin validation. */
    List<CarBrand> findPendingOrdered();

    /** Admin-only: sets {@code validated = true} on the brand row. No-op when brand is missing. */
    void markAsValidated(long brandId);

    /** Admin-only: removes a (typically pending) brand. Cascades follow database FK rules. */
    void deleteById(long brandId);
}
