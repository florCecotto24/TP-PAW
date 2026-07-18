package ar.edu.itba.paw.mail;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

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

    /**
     * SPA deep link with {@code ?self=} carrying the relative API item path
     * ({@code /cars/{id}}, {@code /reservations/{id}}, …) so F5/bookmark can follow
     * hypermedia without inventing the URN in the client.
     */
    public String absolutePathWithSelf(final String spaPath, final String apiCollection, final long resourceId) {
        final String self = "/" + apiCollection + "/" + resourceId;
        final String encoded = URLEncoder.encode(self, StandardCharsets.UTF_8);
        if (spaPath == null || spaPath.isBlank()) {
            return absolutePath("/?self=" + encoded);
        }
        final String path = spaPath.startsWith("/") ? spaPath : "/" + spaPath;
        final String sep = path.contains("?") ? "&" : "?";
        return absolutePath(path + sep + "self=" + encoded);
    }
}
