package ar.edu.itba.paw.persistence;

import java.util.List;
import java.util.Optional;

import ar.edu.itba.paw.models.domain.Neighborhood;

public interface LocationDao {

    List<Neighborhood> findAllNeighborhoods();

    Optional<Neighborhood> findNeighborhoodById(long neighborhoodId);
}
