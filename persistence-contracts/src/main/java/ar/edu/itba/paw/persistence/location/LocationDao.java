package ar.edu.itba.paw.persistence.location;

import java.util.List;
import java.util.Optional;

import ar.edu.itba.paw.models.domain.location.Neighborhood;

/** Read-only neighborhood catalog. */
public interface LocationDao {

    List<Neighborhood> findAllNeighborhoods();

    Optional<Neighborhood> findNeighborhoodById(long neighborhoodId);
}
