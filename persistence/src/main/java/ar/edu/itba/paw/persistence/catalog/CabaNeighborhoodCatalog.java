package ar.edu.itba.paw.persistence.catalog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import ar.edu.itba.paw.models.domain.Neighborhood;
import ar.edu.itba.paw.persistence.LocationDao;

/**
 * Neighborhood catalog in memory: a single query on bean init (Spring context startup).
 * The data comes from the {@code neighborhoods} table.
 */
@Component
@Primary
public final class CabaNeighborhoodCatalog implements LocationDao {

    @PersistenceContext
    private EntityManager em;

    private List<Neighborhood> allSortedByName;
    private Map<Long, Neighborhood> byId;

    @PostConstruct
    private void load() {
        final List<Neighborhood> rows = em.createQuery(
                "SELECT n FROM Neighborhood n ORDER BY n.id", Neighborhood.class)
                .getResultList();
        if (rows.isEmpty()) {
            throw new IllegalStateException(
                    "Neighborhoods table is empty; verify Flyway migrations and the seed of the database.");
        }
        final Map<Long, Neighborhood> map = new HashMap<>();
        for (final Neighborhood n : rows) {
            map.put(n.getId(), n);
        }
        this.byId = Collections.unmodifiableMap(map);
        final List<Neighborhood> sorted = new ArrayList<>(map.values());
        sorted.sort(Comparator.comparing(Neighborhood::getName, String.CASE_INSENSITIVE_ORDER));
        this.allSortedByName = Collections.unmodifiableList(sorted);
    }

    @Override
    public List<Neighborhood> findAllNeighborhoods() {
        return allSortedByName;
    }

    @Override
    public Optional<Neighborhood> findNeighborhoodById(final long neighborhoodId) {
        return Optional.ofNullable(byId.get(neighborhoodId));
    }
}
