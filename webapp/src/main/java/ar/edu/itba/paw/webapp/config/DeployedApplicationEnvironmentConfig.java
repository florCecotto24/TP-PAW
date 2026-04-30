package ar.edu.itba.paw.webapp.config;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.PropertySource;

/**
 * Default when the {@code local} profile is not active (e.g. Tomcat on Pampero, no -D; or any
 * build where you do not pass {@code -Dspring.profiles.active=local}). Merged app + production
 * credentials in {@code application/application-deployed.properties} (not committed; see
 * application-deployed.properties.example).
 */
@Configuration
@Profile("!local")
@PropertySource("classpath:application/application-deployed.properties")
public class DeployedApplicationEnvironmentConfig {

    @Bean
    public DataSource dataSource(
            @Value("${spring.datasource.url}") final String url,
            @Value("${spring.datasource.username}") final String username,
            @Value("${spring.datasource.password}") final String password) {
        return RydenDataSourceFactory.createWithSchemaAndMigrations(url, username, password);
    }
}
