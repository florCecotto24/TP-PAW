package ar.edu.itba.paw.persistence.car;

import java.util.List;
import java.util.Optional;

import ar.edu.itba.paw.models.domain.car.CarBrand;
import ar.edu.itba.paw.models.dto.Page;

/**
 * Read/write catalog of vehicle brands. Validated brands surface first in pickers; unvalidated brands are
 * those created by the publish-car "Other" flow and are kept until reviewed by an admin.
 */
public interface CarBrandDao {

    /** All brands ordered with validated first, then alphabetical name. */
    List<CarBrand> findAllOrdered();

    /**
     * SQL-paginated brand listing for {@code GET /brands}: {@code validated=null} keeps the
     * validated-first/alphabetical ordering of {@link #findAllOrdered()}, {@code true}/{@code false}
     * filter the same way as {@link #findValidatedOrdered()}. Counts
     * via {@code SELECT COUNT} rather than sizing an already-loaded list.
     */
    Page<CarBrand> findPage(Boolean validated, int page, int pageSize);

    /** Only validated brands, alphabetical. */
    List<CarBrand> findValidatedOrdered();

    /** Single brand by id when present. */
    Optional<CarBrand> findById(long brandId);

    /** Case-insensitive name lookup for de-duplication when creating "Other" brands. */
    Optional<CarBrand> findByNameIgnoreCase(String name);

    /** Inserts a new brand with the given validation flag. */
    CarBrand create(String name, boolean validated);

    /** Removes a brand row by id; cascades to car_models via FK. */
    void deleteById(long brandId);
}
