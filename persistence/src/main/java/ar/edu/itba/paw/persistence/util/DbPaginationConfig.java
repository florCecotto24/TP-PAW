package ar.edu.itba.paw.persistence.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Persistence-layer DB fetch window for dual-layer paginated queries (e.g. home grid + /search):
 * the DAO loads {@link #getDbFetchSize()} rows per SQL window and the UI slices the page out of it.
 *
 * Lives in the persistence module because the DB fetch size is an optimisation detail of the
 * SQL access layer (it does not affect what the UI shows per page; that is owned by
 * {@code AppPaginationProperties} in the webapp module).
 */
@Component
public final class DbPaginationConfig {

    private static final String DB_FETCH_SIZE = "app.pagination.db-fetch-size";

    private static final int FALLBACK_DB_FETCH_SIZE = 24;

    private final int dbFetchSize;

    @Autowired
    public DbPaginationConfig(final Environment environment) {
        final Integer v = environment.getProperty(DB_FETCH_SIZE, Integer.class);
        this.dbFetchSize = v != null && v > 0 ? v : FALLBACK_DB_FETCH_SIZE;
    }

    /** Rows per SQL window for dual-layer browse queries. Always {@code >= 1}. */
    public int getDbFetchSize() {
        return dbFetchSize;
    }
}
