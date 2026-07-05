package ar.edu.itba.paw.services.location;

import ar.edu.itba.paw.models.domain.location.Neighborhood;
import ar.edu.itba.paw.persistence.location.LocationDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Catalog reads via {@link LocationDao}; search query fragments are normalized here (ids, de-duplication, order). */
@Service
public class LocationServiceImpl implements LocationService {

    private static final Logger LOG = LoggerFactory.getLogger(LocationServiceImpl.class);

    private final LocationDao locationDao;

    @Autowired
    public LocationServiceImpl(final LocationDao locationDao) {
        this.locationDao = locationDao;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Neighborhood> findAllNeighborhoods() {
        return locationDao.findAllNeighborhoods();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Neighborhood> findNeighborhoodById(final long neighborhoodId) {
        return locationDao.findNeighborhoodById(neighborhoodId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Long> resolveSearchNeighborhoodId(final String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        try {
            final long id = Long.parseLong(raw.trim());
            if (id <= 0L) {
                return Optional.empty();
            }
            return locationDao.findNeighborhoodById(id).map(Neighborhood::getId);
        } catch (final NumberFormatException e) {
            LOG.atDebug()
                    .setMessage("Search neighborhood id not a number [{}]")
                    .addArgument(raw)
                    .setCause(e)
                    .log();
            return Optional.empty();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> resolveSearchNeighborhoodIds(final String[] rawValues) {
        if (rawValues == null || rawValues.length == 0) {
            return List.of();
        }
        return resolveSearchNeighborhoodIds(Arrays.asList(rawValues));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> resolveSearchNeighborhoodIds(final List<String> rawValues) {
        if (rawValues == null || rawValues.isEmpty()) {
            return List.of();
        }
        final Set<Long> seen = new LinkedHashSet<>();
        for (final String raw : rawValues) {
            resolveSearchNeighborhoodId(raw).ifPresent(seen::add);
        }
        return List.copyOf(seen);
    }
}
