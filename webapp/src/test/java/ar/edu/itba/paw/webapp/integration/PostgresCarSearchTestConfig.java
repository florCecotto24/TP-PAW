package ar.edu.itba.paw.webapp.integration;

import java.util.Properties;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import ar.edu.itba.paw.webapp.config.RydenDataSourceFactory;

/**
 * Persistence config for {@link CarFlexibleSearchPostgresIT}. Schema from the same baseline + Flyway
 * path production uses ({@link RydenDataSourceFactory}). Container starts lazily only when Docker is
 * available so the explicit IT run can {@code Assumptions.assumeTrue} instead of failing class init.
 */
@Configuration
@ComponentScan(basePackages = "ar.edu.itba.paw.persistence")
@EnableTransactionManagement
public class PostgresCarSearchTestConfig {

    private static PostgreSQLContainer<?> postgres;

    static synchronized PostgreSQLContainer<?> requirePostgres() {
        if (!DockerClientFactory.instance().isDockerAvailable()) {
            throw new IllegalStateException("Docker is not available");
        }
        if (postgres == null) {
            postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"));
            postgres.start();
        }
        return postgres;
    }

    @Bean
    public DataSource dataSource() {
        final PostgreSQLContainer<?> container = requirePostgres();
        return RydenDataSourceFactory.createWithSchemaAndMigrations(
                container.getJdbcUrl(), container.getUsername(), container.getPassword());
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(final DataSource dataSource) {
        final LocalContainerEntityManagerFactoryBean factoryBean = new LocalContainerEntityManagerFactoryBean();
        factoryBean.setPackagesToScan("ar.edu.itba.paw.models");
        factoryBean.setDataSource(dataSource);
        factoryBean.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        final Properties properties = new Properties();
        properties.setProperty("hibernate.hbm2ddl.auto", "none");
        properties.setProperty("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        factoryBean.setJpaProperties(properties);
        return factoryBean;
    }

    @Bean
    public PlatformTransactionManager transactionManager(final EntityManagerFactory emf) {
        return new JpaTransactionManager(emf);
    }

    @Bean
    public JdbcTemplate jdbcTemplate(final DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
}
