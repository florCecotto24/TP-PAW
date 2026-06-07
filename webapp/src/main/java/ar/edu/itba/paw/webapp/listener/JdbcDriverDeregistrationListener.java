package ar.edu.itba.paw.webapp.listener;

import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Enumeration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 * Avoids Tomcat 8+ warnings about JDBC drivers left registered when the webapp stops. PostgreSQL
 * (and other drivers) loaded from the webapp classloader should be deregistered in {@code contextDestroyed}
 * so {@link org.apache.catalina.loader.WebappClassLoaderBase} does not deregister "by force".
 */
public final class JdbcDriverDeregistrationListener implements ServletContextListener {

    private static final Logger LOG = LoggerFactory.getLogger(JdbcDriverDeregistrationListener.class);

    @Override
    public void contextInitialized(final ServletContextEvent sce) {
    }

    @Override
    public void contextDestroyed(final ServletContextEvent sce) {
        final ClassLoader thisCl = JdbcDriverDeregistrationListener.class.getClassLoader();
        final Enumeration<Driver> drivers = DriverManager.getDrivers();
        while (drivers.hasMoreElements()) {
            final Driver driver = drivers.nextElement();
            if (driver.getClass().getClassLoader() == thisCl) {
                try {
                    DriverManager.deregisterDriver(driver);
                } catch (final SQLException e) {
                    LOG.atDebug()
                            .setMessage("Failed to deregister JDBC driver {}: {}")
                            .addArgument(driver.getClass().getName())
                            .addArgument(e.toString())
                            .setCause(e)
                            .log();
                }
            }
        }
    }
}
