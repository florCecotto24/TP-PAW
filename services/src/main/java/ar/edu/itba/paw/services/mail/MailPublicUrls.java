package ar.edu.itba.paw.services.mail;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Builds absolute URLs for mail CTAs. Configure {@code mail.app.base.url} per environment
 * (e.g. {@code application/application-local.properties} for local, server URL when deployed); must include context path if any.
 */
@Component
public final class MailPublicUrls {

    private final Environment environment;

    @Autowired
    public MailPublicUrls(final Environment environment) {
        this.environment = environment;
    }

    public String absolutePath(final String pathFromContextRoot) {
        final String base = environment.getProperty("mail.app.base.url", "http://localhost:8080").replaceAll("/+$", "");
        if (pathFromContextRoot == null || pathFromContextRoot.isBlank()) {
            return base;
        }
        final String p = pathFromContextRoot.startsWith("/") ? pathFromContextRoot : "/" + pathFromContextRoot;
        return base + p;
    }
}
