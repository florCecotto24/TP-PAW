package ar.edu.itba.paw.services.location;

import ar.edu.itba.paw.models.domain.location.Neighborhood;

import java.util.List;
import java.util.Optional;

/**
 * Neighborhood catalog and parsing of search/filter parameters for listing browse.
 * Implementations use {@code LocationDao} for catalog reads; ID parsing and de-duplication live in the service layer.
 */
public interface LocationService {

    /** All neighborhoods for pickers and filters. */
    List<Neighborhood> findAllNeighborhoods();

    /** Single catalog row by id when present. */
    Optional<Neighborhood> findNeighborhoodById(long neighborhoodId);

    /**
     * Resolves a single {@code neighborhoodId} query fragment: positive id present in the catalog.
     */
    Optional<Long> resolveSearchNeighborhoodId(String raw);

    /**
     * Parses repeated values, keeps first-seen order, drops unknown or invalid ids.
     */
    List<Long> resolveSearchNeighborhoodIds(List<String> rawValues);

    /**
     * Same as {@link #resolveSearchNeighborhoodIds(List)} for servlet parameter arrays.
     */
    List<Long> resolveSearchNeighborhoodIds(String[] rawValues);
}
