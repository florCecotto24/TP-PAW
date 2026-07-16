package ar.edu.itba.paw.webapp.listener;

import java.util.Collections;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Disables servlet session tracking so the container does not emit {@code JSESSIONID}
 * cookies or rewrite URLs with {@code ;jsessionid=}. Complements Spring Security
 * {@code SessionCreationPolicy.STATELESS} for the JWT API.
 */
public final class SessionTrackingDisablingListener implements ServletContextListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(SessionTrackingDisablingListener.class);

    @Override
    public void contextInitialized(final ServletContextEvent sce) {
        final ServletContext servletContext = sce.getServletContext();
        try {
            servletContext.setSessionTrackingModes(Collections.emptySet());
            LOGGER.atInfo().log("Disabled servlet session tracking (no JSESSIONID cookie/URL)");
        } catch (final IllegalStateException | UnsupportedOperationException e) {
            LOGGER.atWarn().setCause(e).log("Could not disable servlet session tracking");
        }
    }

    @Override
    public void contextDestroyed(final ServletContextEvent sce) {
        // no-op
    }
}
