package ar.edu.itba.paw.persistence.location;

import java.util.List;
import java.util.Optional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.itba.paw.models.domain.location.Neighborhood;
import ar.edu.itba.paw.persistence.location.LocationDao;

@Transactional(readOnly = true)
@Repository
public class LocationJpaDao implements LocationDao {

    @PersistenceContext
    private EntityManager em;

    @Override
    public List<Neighborhood> findAllNeighborhoods() {
        return em.createQuery("FROM Neighborhood n ORDER BY n.name ASC", Neighborhood.class)
                .getResultList();
    }

    @Override
    public Optional<Neighborhood> findNeighborhoodById(final long neighborhoodId) {
        return Optional.ofNullable(em.find(Neighborhood.class, neighborhoodId));
    }
}
