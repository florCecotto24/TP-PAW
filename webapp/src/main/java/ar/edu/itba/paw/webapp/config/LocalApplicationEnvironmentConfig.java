package ar.edu.itba.paw.webapp.config;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.PropertySource;

/**
 * {@code -Dspring.profiles.active=local} (IDE, Jetty). Merged app + credentials in
 * {@code application/application-local.properties} (not committed; see
 * application-local.properties.example).
 */
@Configuration
@Profile("local")
@PropertySource(
        value = "classpath:application/application-local.properties",
        ignoreResourceNotFound = true)
public class LocalApplicationEnvironmentConfig {

    @Bean
    public DataSource dataSource(
            @Value("${spring.datasource.url}") final String url,
            @Value("${spring.datasource.username}") final String username,
            @Value("${spring.datasource.password}") final String password) {
        return RydenDataSourceFactory.createWithSchemaAndMigrations(url, username, password, true);
    }
}
