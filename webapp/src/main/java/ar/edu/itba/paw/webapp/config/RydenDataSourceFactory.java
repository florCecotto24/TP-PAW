package ar.edu.itba.paw.webapp.config;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

/**
 * PostgreSQL: applies {@code classpath:db/ryden_baseline.sql} (idempotent baseline, not Flyway),
 * then Flyway on {@code classpath:db/migration} (V2 onward). {@code baselineOnMigrate} at version 1
 * tags schemas that already have baseline tables so only incremental migrations run.
 */
public final class RydenDataSourceFactory {

    private RydenDataSourceFactory() {}

    public static DataSource createWithSchemaAndMigrations(
            final String url, final String username, final String password) {
        return createWithSchemaAndMigrations(url, username, password, false);
    }

    /**
     * @param repairFlywayOnStartup when {@code true}, runs {@link Flyway#repair()} before {@link Flyway#migrate()}
     *                              (local dev only: realigns checksums after a migration file was edited post-apply).
     */
    public static DataSource createWithSchemaAndMigrations(
            final String url,
            final String username,
            final String password,
            final boolean repairFlywayOnStartup) {
        final SimpleDriverDataSource dataSource = new SimpleDriverDataSource();
        dataSource.setDriverClass(org.postgresql.Driver.class);
        dataSource.setUrl(url);
        dataSource.setUsername(username);
        dataSource.setPassword(password);

        applyBaselineScript(dataSource);

        final Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .baselineVersion("1")
                .failOnMissingLocations(true)
                .load();
        if (repairFlywayOnStartup) {
            flyway.repair();
        }
        flyway.migrate();
        return dataSource;
    }

    private static void applyBaselineScript(final DataSource dataSource) {
        final ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.addScript(new ClassPathResource("db/ryden_baseline.sql"));
        populator.setSqlScriptEncoding("UTF-8");
        populator.execute(dataSource);
    }
}
