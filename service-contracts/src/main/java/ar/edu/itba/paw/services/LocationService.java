package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.Neighborhood;

import java.util.List;
import java.util.Optional;

public interface LocationService {

    List<Neighborhood> findAllNeighborhoods();

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
