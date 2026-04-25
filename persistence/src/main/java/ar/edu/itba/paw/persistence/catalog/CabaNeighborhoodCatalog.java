package ar.edu.itba.paw.persistence.catalog;

import ar.edu.itba.paw.persistence.LocationDao;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.sql.DataSource;

import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import ar.edu.itba.paw.models.Neighborhood;

/**
 * Neighborhood catalog in memory: a single {@code SELECT} when creating the bean (Spring context startup).
 * The data comes from the {@code neighborhoods} table.
 */
@Component
@Primary
public class CabaNeighborhoodCatalog implements LocationDao {

    private static final String LOAD_SQL = "SELECT id, name FROM neighborhoods ORDER BY id";

    private final List<Neighborhood> allSortedByName;
    private final Map<Long, Neighborhood> byId;

    public CabaNeighborhoodCatalog(final DataSource dataSource) {
        final JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        final List<Neighborhood> rows = jdbcTemplate.query(
                LOAD_SQL,
                (rs, rowNum) -> new Neighborhood(rs.getLong("id"), rs.getString("name")));
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
