package ar.edu.itba.paw.persistence;

import java.util.List;
import java.util.Optional;

import ar.edu.itba.paw.models.domain.CarBrand;

/**
 * Read/write catalog of vehicle brands. Validated brands surface first in pickers; unvalidated brands are
 * those created by the publish-car "Other" flow and are kept until reviewed by an admin.
 */
public interface CarBrandDao {

    /** All brands ordered with validated first, then alphabetical name. */
    List<CarBrand> findAllOrdered();

    /** Only validated brands, alphabetical. */
    List<CarBrand> findValidatedOrdered();

    /** Single brand by id when present. */
    Optional<CarBrand> findById(long brandId);

    /** Case-insensitive name lookup for de-duplication when creating "Other" brands. */
    Optional<CarBrand> findByNameIgnoreCase(String name);

    /** Inserts a new brand with the given validation flag. */
    CarBrand create(String name, boolean validated);
}
